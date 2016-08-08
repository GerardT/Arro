#ifndef ARRO_PROCESS_H
#define ARRO_PROCESS_H

#include "Trace.h"
#include "ConfigReader.h"
#include "NodeDb.h"

namespace Arro
{
    class NodeSfc;

    /**
     * \brief Process, like Pad, implements AbstractNode.
     *
     * Process is for functional process nodes, Pad is for non-functional connection pads.
     * This constructor creates (depending on url) one associated IDevice derived instance.
     */
    class Process: public AbstractNode {

    public:
        /**
         * \brief Interface that let implementer listen to updates on a Pad connected to Process.
         * Listener for updates to NodeSingleInput objects, invoked by
         * NodeSingleInput::handleMessage().
         */
        class IPadListener {
        public:
            virtual ~IPadListener() {};
            virtual void handleMessage(MessageBuf* msg, std::string padName) = 0;
        };

        /**
         * Constructor.
         *
         * \param nodeDb Database that contains all nodes.
         * \param url Url to refers the node code.
         * \param instance Name of this instance.
         * \param params List of parameters to pass to node.
         */
        Process(NodeDb& nodeDb, const std::string& url, const std::string& instance, ConfigReader::StringMap params, TiXmlElement* elt = nullptr);
        Process(NodeDb& nodeDb, const std::string& instance);
        ~Process();

        // Copy and assignment is not supported.
        Process(const Process&) = delete;
        Process& operator=(const Process& other) = delete;

        void test() {
            device->test();
        }

        bool hasStep(const std::string& name, const std::string& step);


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
        virtual void registerOutput(const std::string& interfaceName);

        /**
         * Lookup an input by its name, which is concatenated: "procesname.name".
         *
         * \param name Name of input.
         */
        NodeDb::NodeSingleInput*  getInput(const std::string& name);

        /**
         * Lookup an output by its name, which is concatenated: "procesname.name".
         *
         * \param name Name of output.
         */
        NodeDb::NodeMultiOutput* getOutput(const std::string& name);

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
        void getPrimitive(const std::string& url, const std::string& instance, ConfigReader::StringMap& params, TiXmlElement* elt = nullptr);

        /**
         * TODO this is not the happiest function, it is for SFC only. Should be something more elegant.
         * @param sfc
         */
        void registerSfc(const std::string& name, Process* sfc);

    private:
        Trace trace;
        NodeDb& nodeDb;
        IDevice* device;
        IPadListener* listener;
        bool doRunCycle;
    };
}

#endif
