#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <unordered_map>
#include <map>
#include <queue>
#include <algorithm>

#include "NodeDb.h"
#include "NodeTimer.h"


using namespace Arro;
using namespace std;

NodeDb::NodeDb():
    trace("NodeDb", true),
    allInputs(),
    allOutputs(),
    allNodes(),
    inQueue(),
    pInQueue(&inQueue),
    running(false),
	thrd(nullptr) {
}

NodeDb::~NodeDb() {
    while(inQueue.empty() != true) {
        auto fm = inQueue.front();
        inQueue.pop();
        delete fm;
    }


    for(auto it = allOutputs.begin(); it != allOutputs.end() ; ++it) {
        delete it->second;
    }
    for(auto it = allInputs.begin(); it != allInputs.end() ; ++it) {
        delete it->second;
    }

    for(auto it = allNodes.begin(); it != allNodes.end() ; ++it) {
        delete it->second;
    }
}

void
NodeDb::NodeSingleInput::handleMessage(MessageBuf* msg) {
    listen->handleMessage(msg);
}

NodeDb::NodeMultiOutput::NodeMultiOutput(NodeDb* n):
    nm(n),
    inputs() {
}

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
NodeDb::NodeMultiOutput::forwardMessage(MessageBuf* msg) {
    for_each(inputs.begin(), inputs.end(), [msg](NodeSingleInput* i) { i->handleMessage(msg); });
}
void
NodeDb::NodeMultiOutput::submitMessage(google::protobuf::MessageLite* msg) {
    string s = msg->SerializeAsString();
    submitMessageBuffer(s.c_str());
}

void
NodeDb::NodeMultiOutput::submitMessageBuffer(const char* msg) {
    auto s = new MessageBuf(msg);
    auto fm = new FullMsg(this, s);

    std::lock_guard<std::mutex> lock(nm->mutex);
    nm->pInQueue->push(fm);

    nm->condition.notify_one();

}



AbstractNode*
NodeDb::getNode(const string& name) {
    try {
        auto n = allNodes[name];
        return n;
    }
    catch (const std::out_of_range& oor) {
        trace.fatal("### non-registered name " + name);
        return nullptr;
    }
}


AbstractNode*
NodeDb::registerNode(AbstractNode* node, const string& name) {
     allNodes[name] = node;
     return node;
}

NodeDb::NodeSingleInput*
NodeDb::registerNodeInput(AbstractNode* node, const string& interfaceName, NodeDb::NodeSingleInput::IListener* listen) {
    auto n = new NodeDb::NodeSingleInput(/*interfaceName, */listen, node);
    // If NodePass don't use interfaceName
    if(interfaceName == "") {
        allInputs[node->getName()] = n;
        trace.println(string("registering input ") + node->getName());
        return (NodeDb::NodeSingleInput*)n;
    } else {
        allInputs[node->getName() + ARRO_NAME_SEPARATOR + interfaceName] = n;
        trace.println(("registering input ") + node->getName() + ARRO_NAME_SEPARATOR + interfaceName);
        return (NodeDb::NodeSingleInput*)n;
    }
}

NodeDb::NodeMultiOutput*
NodeDb::registerNodeOutput(AbstractNode* node, const string& interfaceName) {
    auto n = new NodeDb::NodeMultiOutput(this);

    // If NodePass don't use interfaceName
    if(interfaceName == "") {
        allOutputs[node->getName()] = n;
        trace.println("registering output " + node->getName());
        return (NodeMultiOutput*)n;
    } else {
        allOutputs[node->getName() + ARRO_NAME_SEPARATOR + interfaceName] = n;
        trace.println("registering output " + node->getName() + ARRO_NAME_SEPARATOR + interfaceName);
        return (NodeMultiOutput*)n;
    }
}

NodeDb::NodeMultiOutput*
NodeDb::getOutput(const string& name) {
    return (NodeMultiOutput*)(allOutputs[name]);
}

NodeDb::FullMsg::FullMsg(NodeMultiOutput* o /*string s*/, MessageBuf* m) {
    //target = s;
    output = o;
    msg = m;
}



void
NodeDb::runCycle(NodeDb* nm) {

    try {
        while(nm->running)
        {
            // Deliver all messages to the right nodes until empty
        	{
                std::unique_lock<std::mutex> lock(nm->mutex);

                while(!(nm->pInQueue->empty())) {
                    FullMsg* fm = nm->pInQueue->front();
                    nm->trace.println("new msg");
                    nm->pInQueue->pop();
                    if(fm != nullptr) {
                        MessageBuf* msg = fm->msg;
                        fm->output->forwardMessage(msg);
                        delete msg;
                        delete fm;
                    }
                }
        	} // make sure mutex is unlocked here

            /* Then trigger all runCycle methods on nodes */
			try {
				for_each(nm->allNodes.begin(), nm->allNodes.end(), [&](pair<string, AbstractNode*> n) { n.second->runCycle(); });
			} catch (std::runtime_error& e) {
		        nm->trace.println("Error "+ string(e.what()));
			}

            {
                std::unique_lock<std::mutex> lock(nm->mutex);

                // And wait until new message arrive in queue
                nm->condition.wait(lock);  // keep waiting if queue empty
            } // make sure mutex is unlocked here
        }
    } catch (std::runtime_error& e) {
    	// If exception, for instance Python (syntax) error, then thread exits here.
    	// User can stop NodeDb after that.
        nm->trace.println("Execution stopped, error " + string(e.what()));
    }
}

void
NodeDb::start() {
    running = true;

    trace.println("Starting ...");

    thrd = new thread(&NodeDb::runCycle, this);

    NodeTimer::start();
}

void
NodeDb::stop() {
    if(running) {
        trace.println("Stopping ...");

        running = false;

        condition.notify_one();

        NodeTimer::stop();

        thrd->join();
        delete thrd;
        thrd = nullptr;
    }
}


void
NodeDb::connect(string& output, string& input) {
    NodeMultiOutput* out = nullptr;
    NodeSingleInput* in = nullptr;

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


