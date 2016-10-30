#ifndef ARRO_NODE_SFC_H
#define ARRO_NODE_SFC_H

#include "arro.pb.h"
#include "ConfigReader.h"
#include "NodeDb.h"
#include "Process.h"

#include "Parser.hpp"


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


    class SfcTransition {
    public:
        SfcTransition(const std::string& condition, const NodeSfc& parent);

        /**
         * Check if any of the transitions in this SFC can fire.
         * If so, run the transition.
         *
         * @param m_currentSteps
         */
        void runTransitions(std::set<std::string>& m_currentSteps);

        /**
         * Check if condition for this transition is true.
         * Function testRule0 will call testRule1, which will call testRule2, etc.
         *
         * @return true if condition true
         */
        bool testRule_START();
        bool testRule_IN(std::list<Instruction>::iterator& it, const std::string& argument);
        bool testRule_LIST_BEGIN(std::list<Instruction>::iterator& it, const std::string& argument);
        bool testRule_SINGLE_STATE(std::list<Instruction>::iterator& it, const std::string& argument);
        bool testRule_LIST_END(std::list<Instruction>::iterator& it);

        void AddAction(const std::string& nodeName, const std::string& actionString) {
            m_actions[nodeName] = actionString;
        }


    private:
        Trace m_trace;
        const NodeSfc& m_parent;
        std::string m_expression;
        std::map<std::string, std::string> m_actions;
        Parser m_parser;
        std::list<Instruction> m_instrList;
        std::string m_from;
        std::string m_to;

        struct Context {
            std::string node;
            bool expValue;
        } context;
    };




    class NodeSfc: public IDevice {
    public:
        /**
         * Constructor
         *
         * \param device The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeSfc(Process* device, TiXmlElement* elt);
        virtual ~NodeSfc() {};

        // Copy and assignment is not supported.
        NodeSfc(const NodeSfc&) = delete;
        NodeSfc& operator=(const NodeSfc& other) = delete;

        /**
         * Setup steps and conditions
         */
        virtual void test();

        /**
         * Handle a message that is sent to this node.
         *
         * \param msg Message sent to this node.
         * \param padName name of pad that message was sent to.
         */
        void handleMessage(MessageBuf* msg, const std::string& padName);

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

        /**
         * TODO this is not the happiest function, it is for SFC only. Should be something more elegant.
         * @param sfc
         */
        void registerSfc(const std::string& name, NodeSfc* sfc) {
            m_trace.println(std::string("Registering node ") + name);
            childSfc[name] = sfc;
        }

        /**
         * Check if there is a node with specified nodeName, which has
         * step with specified stepName.
         *
         * @param nodeName
         * @param step
         * @return
         */
        bool hasStep(const std::string& nodeName, const std::string& stepName) const {
            //std::string tmp = name;
            m_trace.println("checking step " + nodeName);
            try {
                NodeSfc* sfc = childSfc.at(nodeName);
                for(auto it = sfc->m_steps.begin(); it != sfc->m_steps.end(); ++it) {
                    if((*it)->getName() == stepName) {
                        m_trace.println("good " + nodeName);
                        return true;
                    }
                }
            }
            catch (std::out_of_range) {
                m_trace.println("Node in expression not found " + nodeName);
                throw std::runtime_error("Node in expression not found " + nodeName);
            }

            m_trace.println("step not found " + stepName);
            throw std::runtime_error("step not found " + stepName);
            return false;
        }

        bool nodeAtStep(const std::string& node, const std::string& step) const {
            std::string nodeName = this->m_process->getName() + ARRO_NAME_SEPARATOR + "step_" + node;

            if(m_currentInputs.find(node) != m_currentInputs.end()) {
                const std::string& value = m_currentInputs.at(node);

                return value == step;
            }
            return false;
        }

//        void inputHasValue(const std::string& input, const std::string& value) {
//            std::string action_input = this->m_process->getName() + ARRO_NAME_SEPARATOR + "_action";
//
//        }

    private:
        Trace m_trace;
        Process* m_process;
        // List of steps for this SFC
        std::list<std::unique_ptr<SfcStep> > m_steps;
        // List of transitions for this SFC
        std::list<std::unique_ptr<SfcTransition> > m_transitions;
        // Library of child SFCs
        std::map<std::string, NodeSfc*> childSfc;
        // Steps currently active
        std::set<std::string> m_activeSteps;

        // last inputs: action and steps
        std::map<std::string, std::string> m_currentInputs;
    };
}

#endif

