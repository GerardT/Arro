#include <NodeDb.h>
#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <unordered_map>
#include <map>
#include <queue>
#include <NodeTimer.h>


#define TIMEOUT 100

/**
 * NodeDb is basically the runtime process. This is the constructor.
 * Instead of just registering a list of nodes where each node maintains its
 * own inputs and outputs, here we have:
 * - a dictionary of nodes, key e.g. node.subnode.subsubnode
 * - a dictionary of inputs, key e.g. node.subnode.subsubnode.inputpad
 * - a dictionary of outputs, key e.g. node.subnode.subsubnode.outputpad
 * Also the message queues are created here.
 */
NodeDb::NodeDb():
    trace(string("NodeDb"), true),
    allInputs(),
    allOutputs(),
	allNodes(),
	inQueue(),
	pInQueue(&inQueue),
	outQueue(),
	pOutQueue(&outQueue),
	running(false) {
}

/**
 * Destructor. Cleanup message queues and static structures.
 */
NodeDb::~NodeDb() {
    while(outQueue.empty() != true) {
        FullMsg* fm = outQueue.front();
        outQueue.pop();
        delete fm;
    }
    while(inQueue.empty() != true) {
        FullMsg* fm = inQueue.front();
        inQueue.pop();
        delete fm;
    }

	for(std::map<string, NodeMultiOutput*>::iterator it = allOutputs.begin(); it != allOutputs.end() ; ++it) {
		std::pair<string, NodeMultiOutput*> elt = *it;
		delete elt.second;
	}
	for(std::map<string, NodeSingleInput*>::iterator it = allInputs.begin(); it != allInputs.end() ; ++it) {
		std::pair<string, NodeSingleInput*> elt = *it;
		delete elt.second;
	}

	for(std::map<string, INode*>::iterator it = allNodes.begin(); it != allNodes.end() ; ++it) {
		std::pair<string, INode*> elt = *it;
		delete elt.second;
	}
}

void
NodeDb::NodeSingleInput::handleMessage(MessageBuf* msg) {
	listen->handleMessage(msg);
}

/**
 * Constructor for output.
 */
NodeDb::NodeMultiOutput::NodeMultiOutput(NodeDb* n):
		nm(n),
		inputs() {
}

/**
 * Connect one input to the node output.
 */
void
NodeDb::NodeMultiOutput::connectInput(NodeSingleInput* i) {
	if(i) {
        inputs.push_back((NodeSingleInput*)i);
	}
	else {
        nm->trace.println("### cannot connect ");
	}
}
void
NodeDb::NodeMultiOutput::forwardMessage(string* msg) {
    for(std::vector<NodeSingleInput*>::iterator it = inputs.begin(); it != inputs.end() ; ++it) {
        NodeSingleInput* i = *it;
        i->handleMessage(msg);
    }
}
void
NodeDb::NodeMultiOutput::submitMessage(MessageLite* msg) {
	string s = msg->SerializeAsString();
	submitMessageBuffer(s.c_str());
}

void
NodeDb::NodeMultiOutput::submitMessageBuffer(const char* msg) {
	MessageBuf* s = new MessageBuf(msg);
    FullMsg* fm = new FullMsg(this, s);
    nm->pInQueue->push(fm);
}



/**
 * Get a node from its name.
 */
INode*
NodeDb::getNode(string& name) {
    try {
        INode* n = allNodes[name];
        return (INode*)n;
    }
    catch (const std::out_of_range& oor) {
        trace.fatal("### non-registered name " + name);
        return NULL;
    }
}


/**
 * INode is the NodeDb-internal representation of a Node.
 * A Node first registers itself, then registers inputs and outputs.
 */
INode::INode(const string& n) {
    name = n;
}

INode*
NodeDb::registerNode(INode* node, const string& name) {
     allNodes[name] = node;
     return node;
}

/**
 * Register an input with the node.
 * Note: a pad registers as node with one input and one output.
 * @param name Name of the interface as "node.node.interface".
 * @param n The instance of the node.
 */
NodeDb::NodeSingleInput*
NodeDb::registerNodeInput(INode* node, const string& interfaceName, NodeDb::NodeSingleInput::IListener* listen) {
	NodeDb::NodeSingleInput* n = new NodeDb::NodeSingleInput(/*interfaceName, */listen, node);
    // If NodePass don't use interfaceName
    if(interfaceName == "") {
    	allInputs[node->getName()] = n;
    	trace.println(string("registering input ") + node->getName());
        return (NodeDb::NodeSingleInput*)n;
    } else {
    	allInputs[node->getName() + NAME_SEPARATOR + interfaceName] = n;
    	trace.println(("registering input ") + node->getName() + NAME_SEPARATOR + interfaceName);
        return (NodeDb::NodeSingleInput*)n;
    }
}

/**
 * Register an output with the node.
 * Note: a pad registers as node with one input and one output.
 * @param name Name of the interface as "node.node.interface".
 * @param n The instance of the node.
 */
NodeDb::NodeMultiOutput*
NodeDb::registerNodeOutput(INode* node, const string& interfaceName) {
	NodeDb::NodeMultiOutput* n = new NodeDb::NodeMultiOutput(/*interfaceName*/this);
    // If NodePass don't use interfaceName
    if(interfaceName == "") {
    	allOutputs[node->getName()] = n;
    	trace.println("registering output " + node->getName());
        return (NodeMultiOutput*)n;
    } else {
    	allOutputs[node->getName() + NAME_SEPARATOR + interfaceName] = n;
    	trace.println("registering output " + node->getName() + NAME_SEPARATOR + interfaceName);
        return (NodeMultiOutput*)n;
    }
}

/**
 * Find an output from a name "node.subnode.subsub.output".
 */
NodeDb::NodeMultiOutput*
NodeDb::getOutput(const string& name) {
	return (NodeMultiOutput*)(allOutputs[name]);
}

/**
 * Constructor for (addressable) message container.
 */
NodeDb::FullMsg::FullMsg(NodeMultiOutput* o /*string s*/, MessageBuf* m) {
    //target = s;
    output = o;
    msg = m;
}

/**
 * Swap (full) input queue and (empty) output queue.
 */
void
NodeDb::toggleQueue() {
	  queue<FullMsg*>* tmp = pOutQueue;
	  pOutQueue = pInQueue;
	  pInQueue = tmp;
}



/**
 * Send all messages in queue to nodes, trigger runCycle methods on nodes.
 * Then swap queues.
 */
void
NodeDb::runCycle(NodeDb* nm) {

	try {
		while(nm->running)
		{
			/* First deliver all message to right nodes */
		    while(nm->pOutQueue->empty() != true) {
		        FullMsg* fm = nm->pOutQueue->front();
		        nm->trace.println("====================> new msg");
		        nm->pOutQueue->pop();
		        if(fm != NULL) {
		        	MessageBuf* msg = fm->msg;
		            fm->output->forwardMessage(msg);
					delete msg;
					delete fm;

		        }
		    }

		    /* Then trigger all runCycle methods on nodes */
		    for(map<string, INode*>::iterator it = nm->allNodes.begin(); it != nm->allNodes.end() ; ++it) {
		    	std::pair<string, INode*> n = *it;
		    	n.second->runCycle();
		    }

		    /* And switch the queues */
		    nm->toggleQueue();
		}
	} catch (std::runtime_error& e) {
	}
}

/**
 * Start the runtime process by creating and starting thread for it.
 */
void
NodeDb::start() {
	running = true;

	thread = new std::thread(&NodeDb::runCycle, this);

	NodeTimer::start();
}

/**
 * Stop the runtime process by stopping the thread.
 */
void
NodeDb::stop() {
	if(running) {
		running = false;

		NodeTimer::stop();

		thread->join();
		delete thread;
	}
}


/**
 * Connect node output to node input.
 * example: connect("main.pid1.output", "main.pid1.input")
 */
void
NodeDb::connect(string& output, string& input) {
    NodeMultiOutput* out = NULL;
    NodeSingleInput* in = NULL;

    try {
        out = allOutputs[output];
    }
    catch (std::out_of_range) {
        trace.println("### non-registered output " + output);
    }
    try {
        in = allInputs[input];
    }
    catch (std::out_of_range) {
        trace.println("### non-registered input " + input);
    }
    if(in == 0)
    {
        trace.println("### non-registered input " + input);
    }
    else if(out == 0)
    {
        trace.println("### non-registered output " + output);
    }
    else
    {
        out->connectInput((NodeSingleInput*)in);
    }
}


