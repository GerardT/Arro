#ifndef NODE_MANAGER_H
#define NODE_MANAGER_H

#include <Trace.h>
#include <INode.h>

#include <google/protobuf/message.h>
#include <stdexcept>
#include <thread>

#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <unordered_map>
#include <map>
#include <queue>
#include <IDevice.h>


using namespace std;
using namespace google;
using namespace protobuf;


/**
 * Node database.
 * Parses XML file and creates Pad or Process objects.
 */
class NodeDb {


public:
	/**
	 * NodeSingleInput is created when calling registerNodeInput for a node.
	 * Connect multiple NodeSingleInput objects to one NodeMultiOutput.
	 */
	class NodeSingleInput {
	public:
		/**
		 * Listener for updates to NodeSingleInput objects, invoked by
		 * NodeSingleInput::handleMessage().
		 */
		class IListener {
		public:
			virtual ~IListener() {};
			virtual void handleMessage(MessageBuf* msg) = 0;
		};

	private:
		NodeDb::NodeSingleInput::IListener* listen;
		INode* node;

	public:
		NodeSingleInput(NodeDb::NodeSingleInput::IListener* l, INode* n) { listen= l,	node = n; };
		void handleMessage(MessageBuf* msg);
	};

	/**
	 * NodeSingleOutputRef is created when calling registerNodeOutput for a node.
     * It contains a list 'inputs' that contains all connected inputs.
     * Connect multiple NodeSingleInput objects to one NodeMultiOutput.
     */
    class NodeMultiOutput {
    private:
        NodeDb* nm;
        vector<NodeSingleInput*> inputs;
    public:
        NodeMultiOutput(NodeDb* nm);
        void connectInput(NodeSingleInput *i);
        void forwardMessage(MessageBuf* msg);
        void submitMessage(MessageLite* msg);
        void submitMessageBuffer(const char* msg);
    };

    class FullMsg {
    public:
        NodeMultiOutput* output;
        string* msg;
        FullMsg(NodeMultiOutput* o, MessageBuf* s);
    };

private:
    Trace trace;
    map<string, NodeSingleInput*> allInputs;
    map<string, NodeMultiOutput*> allOutputs;
	map<string, INode*> allNodes;
    queue<FullMsg*> inQueue, *pInQueue;
    queue<FullMsg*> outQueue, *pOutQueue;
    bool running;
    std::thread* thread;

public:
    NodeDb();
    ~NodeDb();

    void toggleQueue();
    INode* registerNode(INode* node, string name);
    NodeSingleInput* registerNodeInput(INode* node, string interfaceName, NodeDb::NodeSingleInput::IListener* listen);
    NodeMultiOutput* registerNodeOutput(INode* node, string interfaceName);
    void start();
    void stop();
    void connect(string output, string input);
    static void runCycle(NodeDb* nm);
    INode* getNode(string name);
    NodeMultiOutput* getOutput(string name);
};


#endif
