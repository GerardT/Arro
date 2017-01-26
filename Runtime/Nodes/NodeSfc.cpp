#include <iostream>
#include <vector>
#include <exception>

#include "arro.pb.h"
#include "NodeSfc.h"


using namespace std;
using namespace Arro;
using namespace arro;


static RegisterMe<NodeSfc> registerMe("_SFC");

NodeSfc::NodeSfc(AbstractNode* device, const string& /*name*/, StringMap& /*params*/, TiXmlElement* elt):
    m_trace{"NodeSfc", true},
    m_process{device} {

    // Define "start" as initial step
    m_activeSteps.insert("_ready");

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
        const string* sourceAttr = eltTransition->Attribute(string("source"));
        const string* targetAttr = eltTransition->Attribute(string("target"));
        const string* conditionAttr = eltTransition->Attribute(string("condition"));


        if(sourceAttr && targetAttr && conditionAttr) {
            m_trace.println(std::string("New SfcTransition with condition ") + *conditionAttr);
            auto transition = new SfcTransition(*conditionAttr, *sourceAttr, *targetAttr, *this);

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
        }
        eltTransition = eltTransition->NextSiblingElement("transition");
    }

}

void
NodeSfc::test() {
    m_trace.println("Testing expressions");
    for(auto it = m_transitions.begin(); it != m_transitions.end(); ++it) {
        //(*it)->testRule_START();
    }
}


void
NodeSfc::handleMessage(const MessageBuf& m, const std::string& padName) {
    string action_input = this->m_process->getName() + ARRO_NAME_SEPARATOR + "_action";

    auto msg = new arro::Step();
    msg->ParseFromString((m)->c_str());

    m_trace.println(string("SFC: ") + m_process->getName() + " received " + msg->name());


    // m_currentInputs hold the values that have been read from input. If entry does
    // not exist yet, add it.
    if(m_currentInputs.find(padName) == m_currentInputs.end()) {
        m_trace.println("Adding input " + padName + " msg " + msg->name());
    }
    m_currentInputs[padName] = msg->name();
}

void
NodeSfc::runCycle() {

    //trace.println(string("NodeSfc input = ") + to_string((long double)actual_position));

    if(true /*actual_mode == "Active"*/) {
        //trace.println(string("NodeSfc output = ") + to_string((long double)output));
        m_trace.println("One cycle ");
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


SfcTransition::SfcTransition(const std::string& condition, const std::string& from, const std::string& to, NodeSfc& parent):
    m_trace{"SfcTransition", true},
    m_parent{parent},
    m_expression{condition},
    m_from{from},
    m_to{to},
    m_parser{START, END}
{
    m_parser.addRule(START,         'n', IN,           [this](const std::string& token){ m_context.node = token; return true; });
    m_parser.addRule(IN,            'i', LIST_BEGIN,   [this](const std::string&      ){ return true; });
    m_parser.addRule(LIST_BEGIN,    '(', SINGLE_STATE, [this](const std::string&      ){ return true; });
    m_parser.addRule(SINGLE_STATE,  'n', LIST_END,     [this](const std::string& token){
                                                                                         bool ret = m_parent.hasStep(m_context.node, token);
                                                                                         if(ret) {
                                                                                             //ret = m_parent.nodeAtStep(m_context.node, token);
                                                                                             m_trace.println("Node at step ", ret);
                                                                                             m_context.expValue = ret;
                                                                                         }
                                                                                         return ret; });

    m_parser.addRule(SINGLE_STATE,  'n', SINGLE_STATE, [this](const std::string& token){
                                                                                         bool ret = m_parent.hasStep(m_context.node, token);
                                                                                         if(ret) {
                                                                                             // ret = m_parent.nodeAtStep(m_context.node, token);
                                                                                             m_trace.println("Node at step ", ret);
                                                                                             m_context.expValue = ret;
                                                                                         }
                                                                                         return true; });

    m_parser.addRule(LIST_END,      ')', DONE,         [this](const std::string&      ){ return true; });

    m_parser.addRule(DONE,          'a', START,        [this](const std::string&      ){ return true; });  // AND
    m_parser.addRule(DONE,          'o', START,        [this](const std::string&      ){ return true; });  // OR
    m_parser.addRule(DONE,          '$', END,          [this](const std::string&      ){ if(m_context.expValue) {
                                                                                             sendActions();
                                                                                             m_parent.updateActiveStep(m_from, m_to);
                                                                                         }
                                                                                         return true; });

    //Tokenizer tokens("node IN(step step)AND node IN(step)");
    Tokenizer tokens(m_expression);
    if(m_parser.parse(tokens, m_instrList)) {
        m_trace.println("Parsing condition succeeded");
    }
    else {
        SendToConsole(string("Parsing condition failed for ") + this->m_parent.getProcess()->getName() + ": \'" + m_expression + "\'");
        throw std::runtime_error("Parsing condition failed: \'" + m_expression + "\'");
    }
}

void
SfcTransition::runTransitions(std::set<std::string>& m_currentSteps) {
    m_trace.println("runTransition step-from= " + m_from);
    if(m_currentSteps.find(m_from) != m_currentSteps.end()) {
        Tokenizer tokens(m_expression);
        m_parser.runCode(tokens);
    }
}

void
SfcTransition::sendActions() {

    for(auto it = m_actions.begin(); it != m_actions.end(); ++it) {
        arro::Action* action = new arro::Action();
        m_trace.println("Send action " + it->second + " to " + it->first);

        action->set_action(it->second);

        m_parent.getProcess()->submitMessage(string("_action_") + it->first, action);
    }
}

