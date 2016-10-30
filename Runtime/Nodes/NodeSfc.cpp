#include <iostream>
#include <vector>
#include <exception>

#include "Trace.h"
#include "ConfigReader.h"
#include "ServerEngine.h"

#include "arro.pb.h"
#include "NodeDb.h"
#include "NodeSfc.h"


using namespace std;
using namespace Arro;
using namespace arro;



NodeSfc::NodeSfc(Process* d, TiXmlElement* elt):
    m_trace("NodeSfc", true),
    m_process(d) {

    // Define "start" as initial step
    m_activeSteps.insert("start");

    // Read XML blocks and register steps
    TiXmlElement* eltStep = elt->FirstChildElement("step");
    while(eltStep) {
        const string* nameAttr = eltStep->Attribute(string("name"));
        m_trace.println(std::string("New SfcStep ") + *nameAttr);
        m_steps.push_front(unique_ptr<SfcStep>(new SfcStep(*nameAttr, *this, false)));

        eltStep = eltStep->NextSiblingElement("step");
    }


    // Read XML blocks and register transitions
    TiXmlElement* eltTransition = elt->FirstChildElement("transition");
    while(eltTransition) {
        const string* conditionAttr = eltTransition->Attribute(string("condition"));
        m_trace.println(std::string("New SfcTransition with condition ") + *conditionAttr);
        auto transition = new SfcTransition(*conditionAttr, *this);

        // Read XML and register actions
        TiXmlElement* eltAction = eltTransition->FirstChildElement("action");
        while(eltAction) {
            const string* nodeNameAttr = eltAction->Attribute(string("node"));
            const string* actionAttr = eltAction->Attribute(string("action"));

            if(nodeNameAttr && actionAttr) {
                m_trace.println("New action " + *actionAttr + " for node " + *nodeNameAttr);
                transition->AddAction(*nodeNameAttr, *actionAttr);
            }
            eltAction = eltAction->NextSiblingElement("action");
        }
        m_transitions.push_front(std::unique_ptr<SfcTransition>(transition));
        eltTransition = eltTransition->NextSiblingElement("condition");
    }

}

void
NodeSfc::test() {
    m_trace.println("Testing expressions");
    for(auto it = m_transitions.begin(); it != m_transitions.end(); ++it) {
        (*it)->testRule_START();
    }
}


void
NodeSfc::handleMessage(MessageBuf* m, const std::string& padName) {
    string action_input = this->m_process->getName() + ARRO_NAME_SEPARATOR + "_action";

    if(m_currentInputs.find(padName) == m_currentInputs.end()) {
        m_trace.println("Adding input " + padName);
    }
    m_currentInputs[padName] = *m;
}

void
NodeSfc::runCycle() {

    //trace.println(string("NodeSfc input = ") + to_string((long double)actual_position));

    if(true /*actual_mode == "Active"*/) {
        //trace.println(string("NodeSfc output = ") + to_string((long double)output));
        for(auto it = m_transitions.begin(); it != m_transitions.end(); ++it) {
            (*it)->runTransitions(m_activeSteps);
        }
    }
}


//////////////////////////////////////////////////////////////////////////////////////////////////

#define START        1
#define IN           2
#define LIST_BEGIN   3
#define SINGLE_STATE 5
#define LIST_END     6
#define AND          7
#define OR           8
#define DONE         9
#define END          10


SfcTransition::SfcTransition(const std::string& condition, const NodeSfc& parent):
    m_trace("SfcTransition", true),
    m_parent(parent),
    m_expression(condition),
    m_parser(START, DONE)
{
    m_parser.addRule(START,         'n', IN,           [this](const std::string& token){ this->context.node = token; return true; });
    m_parser.addRule(IN,            'i', LIST_BEGIN,   [this](const std::string&      ){ return true; });
    m_parser.addRule(LIST_BEGIN,    '(', SINGLE_STATE, [this](const std::string&      ){ return true; });
    m_parser.addRule(SINGLE_STATE,  'n', LIST_END,     [this](const std::string& token){
                                                                                         bool ret = m_parent.hasStep(this->context.node, token);
                                                                                         if(ret) {
                                                                                             ret = m_parent.nodeAtStep(this->context.node, token);
                                                                                             this->m_trace.println("Node at step ", ret);
                                                                                         }
                                                                                         return ret; });

    m_parser.addRule(SINGLE_STATE,  'n', SINGLE_STATE, [this](const std::string& token){
                                                                                         bool ret = m_parent.hasStep(this->context.node, token);
                                                                                         if(ret) {
                                                                                             ret = m_parent.nodeAtStep(this->context.node, token);
                                                                                             this->m_trace.println("Node at step ", ret);
                                                                                         }
                                                                                         return ret; });

    m_parser.addRule(LIST_END,      ')', DONE,         [this](const std::string&      ){ return true; });

    m_parser.addRule(DONE,          'a', START,        [this](const std::string&      ){ return true; });  // AND
    m_parser.addRule(DONE,          'o', START,        [this](const std::string&      ){ return true; });  // OR
    m_parser.addRule(DONE,          '$', END,          [this](const std::string&      ){ return true; });

    //Tokenizer tokens("node IN(step step)AND node IN(step)");
    Tokenizer tokens(m_expression);
    if(m_parser.parse(tokens, m_instrList)) {
        m_trace.println("Parsing condition succeeded");
    }
    else {
        throw std::runtime_error("Parsing condition failed " + m_expression);
    }
}

void
SfcTransition::runTransitions(std::set<std::string>& m_currentSteps) {
    if(m_currentSteps.find(m_from) != m_currentSteps.end()) {
        Tokenizer tokens(m_expression);
        m_parser.runCode(tokens);
//        // check the condition
//        if(testRule_START()) {
//            m_trace.println(string("Transition fires from step ") + m_from);
//            // send all actions.
//
//            //Value* value = new Value();
//
//           // value->set_value(output);
//
//            //m_process->getOutput("output")->submitMessage(value);
//        }
    }
}

bool
SfcTransition::testRule_START() {
    std::list<Instruction>::iterator it = m_instrList.begin();
    bool ret = false;

    if(it!= m_instrList.end()) {
        // we now know that 'n' should be a node name
        if(it->match(START, IN)) {
            testRule_IN(it, it->getArgument());
        } else {
            m_trace.println("Error in condition.");
        }
    }
    return ret;
}

bool
SfcTransition::testRule_IN(std::list<Instruction>::iterator& it, const std::string& nodeName) {
    ++it;
    bool ret = false;

    if(it!= m_instrList.end()) {
        if(it->match(IN, LIST_BEGIN)) {
            testRule_LIST_BEGIN(it, nodeName);
        } else {
            m_trace.println("Error in condition.");
        }
    }
    return ret;
}

bool
SfcTransition::testRule_LIST_BEGIN(std::list<Instruction>::iterator& it, const std::string& nodeName) {
    ++it;
    bool ret = false;

    if(it!= m_instrList.end()) {
        if(it->match(LIST_BEGIN, SINGLE_STATE)) {
            testRule_SINGLE_STATE(it, nodeName);
        } else {
            m_trace.println("Error in condition.");
        }
    }
    return ret;
}

bool
SfcTransition::testRule_SINGLE_STATE(std::list<Instruction>::iterator& it, const std::string& nodeName) {
    ++it;
    bool ret = false;

    if(it!= m_instrList.end()) {
        // we now know that 'n' should be a step name
        ret = m_parent.hasStep(nodeName, it->getArgument());

        if(ret) {
            ret = m_parent.nodeAtStep(nodeName, it->getArgument());
            if(ret && it->match(SINGLE_STATE, LIST_END)) {
                ret = testRule_LIST_END(it);
            } else if(ret && it->match(SINGLE_STATE, SINGLE_STATE)) {
                ret = testRule_SINGLE_STATE(it, nodeName);
            } else {
                m_trace.println("Error in condition.");
            }
        }
    }
    return ret;
}

bool
SfcTransition::testRule_LIST_END(std::list<Instruction>::iterator& it) {
    ++it;
    bool ret = false;

    if(it!= m_instrList.end()) {
        if(it->match(LIST_END, DONE)) {
            //testRule1(it);
        }
        // And / Or for later..
    }
    return ret;
}

