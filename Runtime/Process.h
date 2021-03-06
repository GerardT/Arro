#ifndef ARRO_PROCESS_H
#define ARRO_PROCESS_H

#include "Trace.h"
#include "ConfigReader.h"
#include "NodeDb.h"
#include "RealNode.h"

namespace Arro
{
    class NodeSfc;

    /**
     * \brief Process, like Pad, implements INodeContext.
     *
     * Process is for functional process nodes, Pad is for non-functional connection pads.
     * This constructor creates (depending on url) one associated INodeDefinition derived instance.
     */
    class Process: public RealNode {

    public:
        /**
         * Constructor.
         *
         * \param nodeDb Database that contains all nodes.
         * \param url Url to refers the node code.
         * \param instance Name of this instance.
         * \param params List of parameters to pass to node.
         */
        Process(NodeDb& nodeDb, const std::string& url, const std::string& instance, StringMap params, TiXmlElement* elt = nullptr);
        //Process(NodeDb& nodeDb, const std::string& instance);
        ~Process();

        // Copy and assignment is not supported.
        Process(const Process&) = delete;
        Process& operator=(const Process& other) = delete;
        virtual void finishConstruction() {
            m_elemBlock->finishConstruction();
        };


        void test() {
            m_elemBlock->test();
        }

        /**
         * Called from ConfigReader in order to register an input Pad as input. Basically
         * it installs a listener for this Pad that handles incoming messages.
         *
         * \param interfaceName Name of the interface to register.
         * \param enableRunCycle Set runCycle to true every time a message is received on this interface.
         */
        virtual void registerInput(const std::string& interfaceName, bool enableRunCycle);

        /**
         * Called from ConfigReader in order to register an output Pad as output.
         *
         * \param interfaceName Name of the interface to register.
         */
        virtual void registerOutput(unsigned int padId, const std::string& interfaceName);

        /**
         * Lookup an input by its name, which is internally concatenated: "procesname.name".
         *
         * \param name Name of input.
         */
        InputPad* getInputPad(const std::string& name) const;

        const std::list<unsigned int> getConnections(InputPad* input);

        ItRef begin(InputPad* input, unsigned int connection, Mode mode);
        ItRef end(OutputPad* input);

        /**
         * Lookup an output by its name, which is concatenated: "procesname.name".
         *
         * \param name Name of output.
         */
        OutputPad* getOutputPad(const std::string& name) const;

        virtual void setOutputData(OutputPad* output, google::protobuf::MessageLite* msg) const;

        /**
         * Let implementation of Process run one execution cycle. Only run a
         * cycle if at least one input configured to trigger a cycle received a
         * message.
         */
        virtual void runCycle();

        /**
         * Instantiate a node of the given primitive type.
         *
         * \param url Url that identifies actual code implementation of this node.
         * \param instance Node instance.
         * \param params Parameter list to pass to the node.
         */
        void getPrimitive(const std::string& url, const std::string& instance, StringMap& params, TiXmlElement* elt = nullptr);

        /**
         * TODO this is not the happiest function, it is for SFC only. Should be something more elegant.
         * @param sfc
         */
        void registerChildSfc(const std::string& name, Process* sfc);
        void sendTerminate() {
            m_elemBlock->sendTerminate();
        }

        virtual const std::string& getName() const { return m_name; };

        virtual std::string getParameter(const std::string& parm);

    private:
        Trace m_trace;
        NodeDb& m_nodeDb;
        INodeDefinition* m_elemBlock;
        bool m_doRunCycle;
        std::string m_name;
        std::map<std::string, std::string> m_params;
    };
}

#endif
