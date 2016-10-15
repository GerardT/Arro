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


    #define CONDITION    1
    #define IN           2
    #define STATE_LIST_B 3
    #define SINGLE_STATE 5
    #define STATE_LIST_E 6
    #define AND          7
    #define OR           8
    #define DONE         9


    class SfcTransition {
    public:
        SfcTransition(const std::string& condition, const NodeSfc& parent):
            m_trace("SfcTransition", true),
            m_parent(parent),
            m_expression(condition),
            m_parser(CONDITION, DONE)
        {
            m_parser.addRule(CONDITION,     'n', IN);
            m_parser.addRule(IN,            'i', STATE_LIST_B);
            m_parser.addRule(STATE_LIST_B,  '(', SINGLE_STATE);
            m_parser.addRule(SINGLE_STATE,  'n', STATE_LIST_E);

            m_parser.addRule(SINGLE_STATE,  'n', SINGLE_STATE);  // another state

            m_parser.addRule(STATE_LIST_E,  ')', DONE);

            m_parser.addRule(DONE,          'a', CONDITION);
            m_parser.addRule(DONE,          'o', CONDITION);

            //Tokenizer tokens("node IN(step step)AND node IN(step)");
            Tokenizer tokens(m_expression);
            if(m_parser.parse(tokens, m_instrList)) {
                m_trace.println("Parsing condition succeeded");
            }
            else {
                throw std::runtime_error("Parsing condition failed " + m_expression);
            }
        }

        void runTransitions(std::set<std::string>& m_currentSteps) {
            if(m_currentSteps.find(m_from) != m_currentSteps.end()) {
                // check the condition
                if(testRule0()) {
                    // send all actions.

                    //Value* value = new Value();

                   // value->set_value(output);

                    //m_process->getOutput("output")->submitMessage(value);
                }
            }
        }

        bool testRule0() {
            std::list<Instruction>::iterator it = m_instrList.begin();

            if(it!= m_instrList.end()) {
                // we now know that 'n' should be a node name
                if(it->match(CONDITION, IN)) {
                    testRule1(it, it->getArgument());
                } else {
                    m_trace.println("Error in condition.");
                }
            }
        }

        bool testRule1(std::list<Instruction>::iterator& it, const std::string& argument) {
            ++it;

            if(it!= m_instrList.end()) {
                if(it->match(IN, STATE_LIST_B)) {
                    testRule2(it, argument);
                } else {
                    m_trace.println("Error in condition.");
                }
            }
        }

        bool testRule2(std::list<Instruction>::iterator& it, const std::string& argument) {
            ++it;

            if(it!= m_instrList.end()) {
                if(it->match(STATE_LIST_B, SINGLE_STATE)) {
                    testRule3(it, argument);
                } else {
                    m_trace.println("Error in condition.");
                }
            }
        }

        bool testRule3(std::list<Instruction>::iterator& it, const std::string& argument);

        bool testRule4(std::list<Instruction>::iterator& it) {
            ++it;

            if(it!= m_instrList.end()) {
                if(it->match(IN, STATE_LIST_B)) {
                    //testRule1(it);
                }
            }
        }

        void AddAction(const std::string& actionName, const std::string& actionString) {
            m_actions.at(actionName) = actionString;
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

        bool hasStep(const std::string& name, const std::string& step) const {
            std::string tmp = name;
            m_trace.println("checking step " + tmp);
            if(childSfc.find(tmp) == childSfc.end()) {
                m_trace.println("Node in expression not found " + tmp);
                throw std::runtime_error("Node in expression not found " + tmp);
                return false;
            } else {
                NodeSfc* sfc = childSfc.at(name);
                for(auto it = sfc->m_steps.begin(); it != sfc->m_steps.end(); ++it) {
                    if((*it)->getName() == step) {
                        return true;
                    }
                }
            }
            m_trace.println("step not found " + step);
            throw std::runtime_error("step not found " + step);
            return false;
        }

    private:
        Trace m_trace;
        Process* m_process;
        std::list<std::unique_ptr<SfcStep> > m_steps;
        std::list<std::unique_ptr<SfcTransition> > m_transitions;
        std::map<std::string, NodeSfc*> childSfc;
        std::set<std::string> m_currentSteps;
    };
}

#endif

