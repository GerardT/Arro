#ifndef ARRO_NODE_SFC_H
#define ARRO_NODE_SFC_H

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include <list>
#include <string>
#include <iostream>

#include "../lemon/CodeGenInterface.h"


namespace Arro {

    class NodeSfc;

    class SfcStep {
    public:
        SfcStep(const std::string& name, const NodeSfc& parent, bool initStep):
            m_trace("SfcStep", true),
            m_parent(parent),
            m_name(name),
            m_init(initStep)
        {}

        const std::string& getName() const {
            return m_name;
        }


    private:
        Trace m_trace;
        const NodeSfc& m_parent;
        std::string m_name;
        bool m_init;
    };

    // syntax
    //
    // SINGLE_COND  :== SINGLE_COND + { ['AND' | 'OR'] SINGLE_COND }
    // SINGLE_COND  :== NODE 'IN' '(' { SINGLE_STATE } ')'
    // CONDITION :== SINGLE_COND


    class SfcTransition: CodeGenInterface {
    public:
        SfcTransition(const std::string& condition, const std::string& from, const std::string& to, NodeSfc& parent);
        virtual ~SfcTransition();

        /**
         * Check if any of the transitions in this SFC can fire.
         * If so, run the transition.
         *
         * @param m_currentSteps
         */
        void runTransition(std::set<std::string>& m_currentSteps, std::set<std::string>& m_newSteps);

        void sendActions();

        void AddAction(const std::string& nodeName, const std::string& actionString) {
            m_trace.println("Adding action " + actionString);

            m_actions[nodeName] = actionString;
        }

        void parseExpression();

        virtual bool hasNode(const std::string& node);
        virtual bool hasStates(const std::string& node, const std::string& states);
        virtual bool nodeInState(const std::string& node, const std::string& state);


    private:
        Trace m_trace;
        NodeSfc& m_parent;
        std::string m_expression;
        std::map<std::string, std::string> m_actions;
        const std::string m_from;
        const std::string m_to;
        CodeGenerator* m_cg;
    };




    class NodeSfc: public INodeDefinition {
    public:
        /**
         * Constructor
         *
         * \param elemBlock The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeSfc(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement* elt);
        virtual ~NodeSfc() {};

        virtual void finishConstruction();

        // Copy and assignment is not supported.
        NodeSfc(const NodeSfc&) = delete;
        NodeSfc& operator=(const NodeSfc& other) = delete;



        /**
         * Setup steps and conditions
         */
        virtual void test();

        /**
         * Make the node execute a processing cycle.
         */
        virtual void runCycle();

        /**
         * TODO this is not the happiest function, it is for SFC only. Should be something more elegant.
         * @param sfc
         */
        virtual void registerChildSfc(const std::string& name, INodeDefinition* sfc) {

            m_trace.println(std::string("Registering node: ") + name);
            m_childSfc[name] = (NodeSfc*)sfc;
        }
        virtual void sendTerminate();

        /**
         * Check if there is a node with specified nodeName, which has
         * step with specified stepName.
         *
         * @param nodeName
         * @param step
         * @return
         */
        bool sfcHasStates(const std::string& nodeName, const std::string& states) const {
            // std::istringstream by design reads to next space in inputstream
            std::istringstream iis(states);
            std::vector<std::string> stateList((std::istream_iterator<std::string>(iis)),
                                                std::istream_iterator<std::string>());

            m_trace.println("sfcHasStates - checking node " + nodeName + " for states " + states);
            for(std::string state: stateList) {
                bool found = false;
                try {
                    if(nodeName == "request") {
                        for(auto it = m_steps.begin(); it != m_steps.end(); ++it) {
                            if((*it)->getName() == state) {
                                m_trace.println("good " + nodeName);
                                found = true;
                            }
                        }
                    } else {
                        NodeSfc* sfc = m_childSfc.at(nodeName);
                        for(auto it = sfc->m_steps.begin(); it != sfc->m_steps.end(); ++it) {
                            m_trace.println("Check state " + (*it)->getName());
                            if((*it)->getName() == state) {
                                m_trace.println("good " + nodeName);
                                found = true;
                            }
                        }
                    }
                }
                catch (std::out_of_range&) {
                    m_trace.println("Node in expression not found: " + nodeName);
                    throw std::runtime_error("Node in expression not found: " + nodeName);
                }
                if(!found) {
                    m_trace.println(std::string("state not found: ") + state + " for node: " + nodeName);
                    throw std::runtime_error(std::string("state not found: ") + state + " for node: " + nodeName);
                    return false;
                }
            }
            return true;
        }

        bool sfcHasNode(const std::string& nodeName) const {
            m_trace.println("sfcHasNode - checking node " + nodeName);
            try {
                if(nodeName == "request") {
                    return true;
                } else {
                    for(auto it = m_childSfc.begin(); it != m_childSfc.end(); ++it) {
                        m_trace.println("Check state " + it->first);
                        if(nodeName == it->first) {
                            return true;
                        }
                    }
                }
            }
            catch (std::out_of_range&) {
                m_trace.println("Node in expression not found: " + nodeName);
                throw std::runtime_error("Node in expression not found: " + nodeName);
            }

            return false;
        }

        bool sfcNodeAtStep(const std::string& node, const std::string& step) const {
            m_trace.println("sfcNodeAtStep - checking step " + step);
            if(node == "request") {
                return (m_activeSteps.find(step) != m_activeSteps.end());
            } else {
                std::string nodeName = /*this->m_elemBlock->getName() + ARRO_PAD_SEPARATOR +*/ "_step_" + node;

                if(m_currentInputs.find(nodeName) != m_currentInputs.end()) {
                    const std::string& value = m_currentInputs.at(nodeName);

                    return value == step;
                }
                // dump currentInputs
                for(auto it = m_currentInputs.begin(); it != m_currentInputs.end(); ++it) {
                    m_trace.println("input: " + it->first + " value: " + it->second);
                }
            }
            return false;
        }

        void updateActiveStep(const std::string from, const std::string to) {
            m_activeSteps.erase(from);
            m_activeSteps.insert(to);

        }

        const INodeContext* getProcess() const {
            return m_elemBlock;
        }

//        void inputHasValue(const std::string& input, const std::string& value) {
//            std::string action_input = this->m_elemBlock->getName() + ARRO_PAD_SEPARATOR + "_action";
//
//        }

    private:
        Trace m_trace;
        INodeContext* m_elemBlock;
        // List of steps for this SFC
        std::list<std::unique_ptr<SfcStep> > m_steps;
        // List of transitions for this SFC
        std::list<std::unique_ptr<SfcTransition> > m_transitions;
        // Library of child SFCs
        std::map<std::string, NodeSfc*> m_childSfc;
        // Steps currently active
        std::set<std::string> m_activeSteps;

        // last inputs: action and steps
        std::map<std::string, std::string> m_currentInputs;

        INodeContext::ItRef m_stepsPad;

        INodeContext::ItRef m_specialActionPad;
    };
}

#endif

