#include <lemon/CodeGenInterface.h>
#include <Nodes/NodeSfc.h>
#include <NodeDb.h>
#include <iostream>
#include <vector>
#include <exception>

#include "arro.pb.h"


using namespace std;
using namespace Arro;
using namespace arro;


static RegisterMe<NodeSfc> registerMe("_SFC");

NodeSfc::NodeSfc(INodeContext* elemBlock, const string& /*name*/, StringMap& /*params*/, TiXmlElement* elt):
    m_trace{"NodeSfc", true},
    m_elemBlock{elemBlock},
    m_stepsPad{nullptr} {

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
                const string* nodeNameAttr = eltAction->Attribute(string("block"));
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
NodeSfc::finishConstruction() {
    m_stepsPad = m_elemBlock->begin(m_elemBlock->getInputPad("_steps"), 0, INodeContext::DELTA);

//    auto t = m_stepsPad->getConnections();
//
//    for(auto conn = m_stepsPad->getConnections().begin(); conn != m_stepsPad->getConnections().end(); ++conn) {
//        OutputPad* op = m_stepsPad->getOutputPad(*conn);
//        if(op) {
//            m_trace.println("finishConstruction found node " + op->getNodeName());
//        }
//    }

    m_trace.println("finishConstruction");
    for(auto it = m_transitions.begin(); it != m_transitions.end(); ++it) {
        (*it)->parseExpression();
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
NodeSfc::runCycle() {

    //trace.println(string("NodeSfc input = ") + to_string((long double)actual_position));


    MessageBuf msgBuf;
    if(m_stepsPad->getNext(msgBuf)) {
        Step* step = new Step();

        step->ParseFromString((msgBuf)->c_str());

        std::string node = step->node();
        std::string padName = std::string("_step_") + node.substr(node.rfind(".") + 1);

        m_trace.println(string("SFC2: ") + m_elemBlock->getName() + " for node " + padName + " mode " + step->name());

        // m_currentInputs hold the values that have been read from input. If entry does
        // not exist yet, add it.
        if(m_currentInputs.find(padName) == m_currentInputs.end()) {
            m_trace.println("Adding input " + padName + " msg " + step->name());
        }
        m_currentInputs[padName] = step->name();

        if(true /*actual_mode == "Active"*/) {
            //trace.println(string("NodeSfc output = ") + to_string((long double)output));
            m_trace.println("One cycle ");

            std::set<std::string> newSteps{};
            for(auto it = m_transitions.begin(); it != m_transitions.end(); ++it) {
                (*it)->runTransition(m_activeSteps, newSteps);
            }
            m_activeSteps = newSteps;
        }
    }

}


//////////////////////////////////////////////////////////////////////////////////////////////////


SfcTransition::SfcTransition(const std::string& expression, const std::string& from, const std::string& to, NodeSfc& parent):
    m_trace{"SfcTransition", true},
    m_parent{parent},
    m_expression{expression},
    m_from{from},
    m_to{to},
    m_cg{nullptr}
{
}

SfcTransition::~SfcTransition() {
    CodeGenInterface::delInstance(m_cg);
    m_cg = nullptr;
}

/**
 * Done separately since cannot check if nodes and states are available
 * before they are all declared.
 */
void SfcTransition::parseExpression() {
    if(!m_cg) m_cg = CodeGenInterface::getInstance(m_expression, this);
}

void
SfcTransition::runTransition(std::set<std::string>& currentSteps, std::set<std::string>& newSteps) {
    m_trace.println("runTransition step-from= " + m_from);
    if(currentSteps.find(m_from) != currentSteps.end()) {
        if(CodeGenInterface::run(m_cg) == true /* expression returned true, change state */) {
            newSteps.insert(m_to);
        }
    }
}

void
SfcTransition::sendActions() {

    for(auto it = m_actions.begin(); it != m_actions.end(); ++it) {
        arro::Action* action = new arro::Action();
        m_trace.println("Send action " + it->second + " to " + it->first);

        action->set_action(it->second);
        auto p = m_parent.getProcess();

        p->setOutputData(p->getOutputPad(string("_action_") + it->first), action);
    }
}

bool
SfcTransition::hasNode(const std::string& node){
    return m_parent.sfcHasNode(node);
};
bool
SfcTransition::hasStates(const std::string& node, const std::string& states){
    return m_parent.sfcHasStates(node, states);
};

bool
SfcTransition::nodeInState(const std::string& node, const std::string& state){
    return m_parent.sfcNodeAtStep(node, state);
};


