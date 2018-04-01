#ifndef ARRO_NODE_DB_H
#define ARRO_NODE_DB_H


#include <stdexcept>
#include <thread>

#include <google/protobuf/message.h>
#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <unordered_map>
#include <map>
#include <queue>
#include <mutex>
#include <condition_variable>
#include <memory>
#include <functional>

#include "RealNode.h"
#include "INodeDefinition.h"
#include "Trace.h"


namespace Arro
{
class NodeDb;

class InputPad {
public:
    /**
     * Constructor for InputPad. Can set only one listener to node input.
     *
     * \param l Listener
     * \param n Node to which this input is attached.
     */
    InputPad(const std::string& interfaceName, std::function<void (const MessageBuf& m_msg, const std::string& interfaceName)> l, RealNode* n):
        m_callback(l), m_node(n), m_msg(new std::string()), m_interfaceName(interfaceName) { };
    virtual ~InputPad() {};

    // Copy and assignment is not supported.
    InputPad(const InputPad&) = delete;
    InputPad& operator=(const InputPad& other) = delete;

    /**
     * Passes the message on to the listener.
     *
     * \param msg Message to pass on.
     */
    void handleMessage(const MessageBuf& msg);

    const MessageBuf& getData() const { return m_msg; };

private:
    std::function<void (const MessageBuf& m_msg, const std::string& interfaceName)> m_callback;
    RealNode* m_node;
    MessageBuf m_msg;
public:
    std::string m_interfaceName;

};

/**
 * \brief Class instance represents one output for a node.
 *
 * NodeSingleOutputRef is created when calling registerNodeOutput for a node.
 * It keeps a list 'inputs' that contains all connected inputs.
 * Connect multiple InputPad objects to one OutputPad.
 */
class OutputPad {
public:
    /**
     * Constructor for OutputPad.
     *
     * \param db Node database.
     */
    OutputPad(NodeDb* db);
    virtual ~OutputPad() {};

    // Copy and assignment is not supported.
    OutputPad(const OutputPad&) = delete;
    OutputPad& operator=(const OutputPad& other) = delete;

    /**
     * Connect this output to an input.
     *
     * \param i Input node to connect to.
     */
    void connectInput(InputPad *i);

    /**
     * Forward a message.
     *
     * \param msg Message to forward.
     */
    void forwardMessage(const MessageBuf& msg);

    /**
     * Submit a Protobuf buffer into msg queue.
     *
     * \param msg Buffer to submit.
     */
    void submitMessage(google::protobuf::MessageLite* msg);

    /**
     * Fill a MessageBuf from string and submit Protobuf buffer into queue.
     *
     * \param msg String to submit.
     */
    void submitMessageBuffer(const char* msg);

private:
    NodeDb* m_nm;
    std::vector<InputPad*> m_inputs;
};


    /**
     * \brief Node database.
     *
     * Node database into which to store all nodes.
     */
    class NodeDb {
    friend class OutputPad;
    friend class InputPad;

    public:

        /**
         * \brief Class instance represents one input for a node.
         *
         * InputPad is created when calling registerNodeInput for a node.
         * Connect multiple InputPad objects to one OutputPad.
         */

        class FullMsg {
        public:
            /**
             * Constructor for (addressable) message container.
             *
             * \param o OutputPad instance where to send this message to.
             * \param s Message buffer to send.
             */
            FullMsg(OutputPad* o, MessageBuf& s);
            virtual ~FullMsg() {};

            // Copy and assignment is not supported.
            FullMsg(const FullMsg&) = delete;
            FullMsg& operator=(const FullMsg& other) = delete;

        // FIXME Should be private
            OutputPad* m_output;
            MessageBuf m_msg;
        };

    public:
        /**
         * NodeDb is basically the runtime process. This is the constructor.
         * Instead of just registering a list of nodes where each node maintains its
         * own inputs and outputs, here we have:
         * - a dictionary of nodes, key e.g. node.subnode.subsubnode
         * - a dictionary of inputs, key e.g. node.subnode.subsubnode.inputpad
         * - a dictionary of outputs, key e.g. node.subnode.subsubnode.outputpad
         * Also the message queues are created here.
         */
        NodeDb();
        ~NodeDb();

        // Copy and assignment is not supported.
        NodeDb(const NodeDb&) = delete;
        NodeDb& operator=(const NodeDb& other) = delete;

        /**
         * Swap (full) input queue and (empty) output queue.
         */
        void toggleQueue();

        /**
         * Register an RealNode by name.
         *
         * \param node Node to register.
         * \param name Name to use for registration.
         */
        RealNode* registerNode(RealNode* node, const std::string& name);

        /**
         * Register an input with the node.
         * Note: a pad registers as node with one input and one output.
         * \param name Name of the interface as "node.node.interface".
         * \param n The instance of the node.
         */
        InputPad* registerNodeInput(RealNode* node, const std::string& interfaceName, std::function<void (const MessageBuf& m_msg, const std::string& interfaceName)> listen);

        /**
         * Register an output with the node.
         * Note: a pad registers as node with one input and one output.
         * \param name Name of the interface as "node.node.interface".
         * \param n The instance of the node.
         */
        OutputPad* registerNodeOutput(RealNode* node, const std::string& interfaceName);

        /**
         * Start the runtime process by creating and starting thread for it.
         */
        void start();

        /**
         * Stop the runtime process by stopping the thread.
         */
        void stop();

        /**
         * Connect node output to node input.
         * example: connect("main.pid1.output", "main.pid1.input")
         *
         * \param output Name of node output.
         * \param input Name of node input.
         */
        void connect(std::string& output, std::string& input);

        /**
         * Send all messages in queue to nodes, trigger runCycle methods on nodes.
         * Then swap queues.
         *
         * \param nm Node to run.
         */
        static void runCycle(NodeDb* nm);

        /**
         * Get a node from its name.
         * Note: only 'leaf' nodes are returned; others are not registered in allNodes.
         *
         * \param name Name of the node to lookup.
         *
         * \return Found node reference (makes this method non-const).
         */
        RealNode* getNode(const std::string& name);

        /**
         * Find an input from a name "node.subnode.subsub.input".
         *
         * \param name Name of input to lookup.
         *
         * \return Found input reference (makes this method non-const).
         */
        InputPad* getInputPad(const std::string& name);

        /**
         * Find an output from a name "node.subnode.subsub.output".
         *
         * \param name Name of output to lookup.
         *
         * \return Found output reference (makes this method non-const).
         */
        OutputPad* getOutputPad(const std::string& name);

        void visitNodes(std::function<void(RealNode&)> f) {
            for(auto const& it : m_allNodes) {
                f(*(it.second));
            }

        }

    private:
        Trace m_trace;
        std::map<std::string, std::unique_ptr<InputPad> > m_allInputs;
        std::map<std::string, std::unique_ptr<OutputPad> > m_allOutputs;
        std::map<std::string, std::unique_ptr<RealNode> > m_allNodes;
        std::queue<FullMsg*> m_inQueue, *m_pInQueue;
        std::queue<FullMsg*> m_outQueue, *m_pOutQueue;
        bool m_running;
        std::thread* m_thrd;
        std::mutex m_mutex;
        std::condition_variable m_condition;

    };
}


#endif
