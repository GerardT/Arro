#include <Nodes/NodeTimer.h>
#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <unordered_map>
#include <map>
#include <queue>
#include <algorithm>

#include "ServerEngine.h"
#include "NodeDb.h"
#include "RealNode.h"


using namespace Arro;
using namespace std;

NodeDb::NodeDb():
    m_trace{"NodeDb", true},
    m_allInputs{},
    m_allOutputs{},
    m_allNodes{},
    m_inQueue{},
    m_pInQueue{&m_inQueue},
    m_outQueue{},
    m_pOutQueue{&m_outQueue},
    m_running{false},
    m_thrd{nullptr} {
}

NodeDb::~NodeDb() {
    while(m_inQueue.empty() != true) {
        auto fm = m_inQueue.front();
        m_inQueue.pop();
        delete fm;
    }
    while(m_outQueue.empty() != true) {
        auto fm = m_outQueue.front();
        m_outQueue.pop();
        delete fm;
    }
}

void
NodeSingleInput::handleMessage(const MessageBuf& msg) {
    m_msg = msg;
    // TODO remove sending message (handlemessage)
    m_callback(msg, m_interfaceName);
}

NodeMultiOutput::NodeMultiOutput(NodeDb* n):
    m_nm{n},
    m_inputs{} {
}

void
NodeMultiOutput::connectInput(NodeSingleInput* i) {
    if(i) {
        m_inputs.push_back((NodeSingleInput*)i);
    }
    else {
        m_nm->m_trace.println("### cannot connect ");
    }
}

void
NodeMultiOutput::forwardMessage(const MessageBuf& msg) {
    for_each(m_inputs.begin(), m_inputs.end(), [msg](NodeSingleInput* i) { i->handleMessage(msg); });
}
void
NodeMultiOutput::submitMessage(google::protobuf::MessageLite* msg) {
    string s = msg->SerializeAsString();
    submitMessageBuffer(s.c_str());
    free(msg);
}

void
NodeMultiOutput::submitMessageBuffer(const char* msg) {
    MessageBuf s(new string(msg));
    auto fm = new NodeDb::FullMsg(this, s);

    std::lock_guard<std::mutex> lock(m_nm->m_mutex);
    m_nm->m_pInQueue->push(fm);

    m_nm->m_condition.notify_one();

}



INodeContext*
NodeDb::getNode(const string& name) {
    try {
        auto n = &(*(m_allNodes[name]));
        return n;
    }
    catch (const std::out_of_range& oor) {
        m_trace.fatal("### non-registered name " + name);
        return nullptr;
    }
}

INodeContext*
NodeDb::registerNode(INodeContext* node, const string& name) {
     m_allNodes[name] = std::unique_ptr<INodeContext>(node);
     return node;
}

NodeSingleInput*
NodeDb::registerNodeInput(INodeContext* node, const string& interfaceName,
                          std::function<void (const MessageBuf& msg, const std::string& interfaceName)> listen) {
    auto n = new NodeSingleInput(interfaceName, listen, node);
    // If NodePass don't use interfaceName
    if(interfaceName == "") {
        m_allInputs [node->getName()] = unique_ptr<NodeSingleInput>(n);
        m_trace.println(string("registering input ") + node->getName());
        return (NodeSingleInput*)n;
    } else {
        m_allInputs[node->getName() + ARRO_PAD_SEPARATOR + interfaceName] = unique_ptr<NodeSingleInput>(n);
        m_trace.println(("registering input ") + node->getName() + ARRO_PAD_SEPARATOR + interfaceName);
        return (NodeSingleInput*)n;
    }
}

NodeMultiOutput*
NodeDb::registerNodeOutput(INodeContext* node, const string& interfaceName) {
    auto n = new NodeMultiOutput(this);

    // If NodePass don't use interfaceName
    if(interfaceName == "") {
        m_allOutputs[node->getName()] = unique_ptr<NodeMultiOutput>(n);
        m_trace.println("registering output " + node->getName());
        return (NodeMultiOutput*)n;
    } else {
        m_allOutputs[node->getName() + ARRO_PAD_SEPARATOR + interfaceName] = unique_ptr<NodeMultiOutput>(n);
        m_trace.println("registering output " + node->getName() + ARRO_PAD_SEPARATOR + interfaceName);
        return (NodeMultiOutput*)n;
    }
}

NodeMultiOutput*
NodeDb::getOutput(const string& name) {
    return (NodeMultiOutput*)&(*((m_allOutputs[name])));
}

NodeSingleInput*
NodeDb::getInput(const std::string& name) {
    return (NodeSingleInput*)&(*(m_allInputs[name]));  // Since using unique_ptr, we have to get pointer first (*)
}


NodeDb::FullMsg::FullMsg(NodeMultiOutput* o /*string s*/, MessageBuf& m) {
    //target = s;
    m_output = o;
    m_msg = m;
}

void
NodeDb::toggleQueue() {
    queue<FullMsg*>* tmp = m_pOutQueue;
    m_pOutQueue = m_pInQueue;
    m_pInQueue = tmp;
}



void
NodeDb::runCycle(NodeDb* nm) {

    try {
        while(nm->m_running)
        {
            // Deliver all messages to the right nodes until empty
            {
                std::unique_lock<std::mutex> lock(nm->m_mutex);

                while(!(nm->m_pOutQueue->empty())) {
                    FullMsg* fm = nm->m_pOutQueue->front();
                    nm->m_trace.println("new msg");
                    nm->m_pOutQueue->pop();
                    if(fm != nullptr) {
                        //MessageBuf msg = fm->m_msg;
                        fm->m_output->forwardMessage(fm->m_msg);
                        // delete msg;
                        delete fm;
                    }
                }
            } // make sure mutex is unlocked here

            /* Then trigger all runCycle methods on nodes */
            for(auto it = nm->m_allNodes.begin(); it != nm->m_allNodes.end(); ++it) {
                INodeContext* an = &(*(it->second));
                ((RealNode*)(an))->runCycle();

                //it->second->runCycle();
            }

            /* And switch the queues */
            nm->toggleQueue();

            if(nm->m_pOutQueue->empty()) {
                std::unique_lock<std::mutex> lock(nm->m_mutex);

                // And wait until new message arrive in queue
                nm->m_condition.wait(lock);  // keep waiting if queue empty
            } // make sure mutex is unlocked here
        }
    } catch (std::runtime_error& e) {
        // If exception, for instance Python (syntax) error, then thread exits here.
        // User can stop NodeDb after that.
        nm->m_trace.println("Execution stopped, error " + string(e.what()));
        SendToConsole(string(e.what()));
    }
}

void
NodeDb::start() {
    m_running = true;

    m_trace.println("Starting ...");

    m_thrd = new thread(&NodeDb::runCycle, this);

    NodeTimer::start();
}

void
NodeDb::stop() {
    if(m_running) {
        m_trace.println("Stopping ...");

        m_running = false;

        m_condition.notify_one();

        NodeTimer::stop();

        m_thrd->join();
        delete m_thrd;
        m_thrd = nullptr;
    }
}


void
NodeDb::connect(string& output, string& input) {
    NodeMultiOutput* out = nullptr;
    NodeSingleInput* in = nullptr;

    try {
        out = &(*(m_allOutputs[output]));
    }
    catch (std::out_of_range) {
        m_trace.println("### non-registered output " + output);
    }
    try {
        in = &(*(m_allInputs[input]));
    }
    catch (std::out_of_range) {
        m_trace.println("### non-registered input " + input);
    }
    if(in == 0)
    {
        m_trace.println("### non-registered input " + input);
    }
    else if(out == 0)
    {
        m_trace.println("### non-registered output " + output);
    }
    else
    {
        out->connectInput((NodeSingleInput*)in);
    }
}


