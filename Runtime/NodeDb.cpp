#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <unordered_map>
#include <map>
#include <queue>
#include <algorithm>

#include "ServerEngine.h"
#include "NodeDb.h"
#include "NodeTimer.h"


using namespace Arro;
using namespace std;

NodeDb::NodeDb():
    m_trace{"NodeDb", true},
    m_allInputs{},
    m_allOutputs{},
    m_allNodes{},
    m_inQueue{},
    m_pInQueue{&m_inQueue},
    m_running{false},
    m_thrd{nullptr} {
}

NodeDb::~NodeDb() {
    while(m_inQueue.empty() != true) {
        auto fm = m_inQueue.front();
        m_inQueue.pop();
        delete fm;
    }
}

void
NodeDb::NodeSingleInput::handleMessage(MessageBuf* msg) {
    m_callback(msg, m_interfaceName);
}

NodeDb::NodeMultiOutput::NodeMultiOutput(NodeDb* n):
    m_nm{n},
    m_inputs{} {
}

void
NodeDb::NodeMultiOutput::connectInput(NodeSingleInput* i) {
    if(i) {
        m_inputs.push_back((NodeSingleInput*)i);
    }
    else {
        m_nm->m_trace.println("### cannot connect ");
    }
}

void
NodeDb::NodeMultiOutput::forwardMessage(MessageBuf* msg) {
    for_each(m_inputs.begin(), m_inputs.end(), [msg](NodeSingleInput* i) { i->handleMessage(msg); });
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

    std::lock_guard<std::mutex> lock(m_nm->m_mutex);
    m_nm->m_pInQueue->push(fm);

    m_nm->m_condition.notify_one();

}



AbstractNode*
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

AbstractNode*
NodeDb::registerNode(AbstractNode* node, const string& name) {
     m_allNodes[name] = std::unique_ptr<AbstractNode>(node);
     return node;
}

NodeDb::NodeSingleInput*
NodeDb::registerNodeInput(AbstractNode* node, const string& interfaceName,
                          std::function<void (MessageBuf* msg, const std::string& interfaceName)> listen) {
    auto n = new NodeDb::NodeSingleInput(interfaceName, listen, node);
    // If NodePass don't use interfaceName
    if(interfaceName == "") {
        m_allInputs [node->getName()] = unique_ptr<NodeSingleInput>(n);
        m_trace.println(string("registering input ") + node->getName());
        return (NodeDb::NodeSingleInput*)n;
    } else {
        m_allInputs[node->getName() + ARRO_NAME_SEPARATOR + interfaceName] = unique_ptr<NodeSingleInput>(n);
        m_trace.println(("registering input ") + node->getName() + ARRO_NAME_SEPARATOR + interfaceName);
        return (NodeDb::NodeSingleInput*)n;
    }
}

NodeDb::NodeMultiOutput*
NodeDb::registerNodeOutput(AbstractNode* node, const string& interfaceName) {
    auto n = new NodeDb::NodeMultiOutput(this);

    // If NodePass don't use interfaceName
    if(interfaceName == "") {
        m_allOutputs[node->getName()] = unique_ptr<NodeMultiOutput>(n);
        m_trace.println("registering output " + node->getName());
        return (NodeMultiOutput*)n;
    } else {
        m_allOutputs[node->getName() + ARRO_NAME_SEPARATOR + interfaceName] = unique_ptr<NodeMultiOutput>(n);
        m_trace.println("registering output " + node->getName() + ARRO_NAME_SEPARATOR + interfaceName);
        return (NodeMultiOutput*)n;
    }
}

NodeDb::NodeMultiOutput*
NodeDb::getOutput(const string& name) {
    return (NodeMultiOutput*)&(*((m_allOutputs[name])));
}

NodeDb::NodeSingleInput*
NodeDb::getInput(const std::string& name) {
    return (NodeSingleInput*)&(*(m_allInputs[name]));  // Since using unique_ptr, we have to get pointer first (*)
}


NodeDb::FullMsg::FullMsg(NodeMultiOutput* o /*string s*/, MessageBuf* m) {
    //target = s;
    m_output = o;
    m_msg = m;
}



void
NodeDb::runCycle(NodeDb* nm) {

    try {
        while(nm->m_running)
        {
            // Deliver all messages to the right nodes until empty
            {
                std::unique_lock<std::mutex> lock(nm->m_mutex);

                while(!(nm->m_pInQueue->empty())) {
                    FullMsg* fm = nm->m_pInQueue->front();
                    nm->m_trace.println("new msg");
                    nm->m_pInQueue->pop();
                    if(fm != nullptr) {
                        MessageBuf* msg = fm->m_msg;
                        fm->m_output->forwardMessage(msg);
                        delete msg;
                        delete fm;
                    }
                }
            } // make sure mutex is unlocked here

            /* Then trigger all runCycle methods on nodes */
            for(auto it = nm->m_allNodes.begin(); it != nm->m_allNodes.end(); ++it) {
                it->second->runCycle();
            }

            {
                std::unique_lock<std::mutex> lock(nm->m_mutex);

                // And wait until new message arrive in queue
                nm->m_condition.wait(lock);  // keep waiting if queue empty
            } // make sure mutex is unlocked here
        }
    } catch (std::runtime_error& e) {
        // If exception, for instance Python (syntax) error, then thread exits here.
        // User can stop NodeDb after that.
        nm->m_trace.println("Execution stopped, error " + string(e.what()));
        ServerEngine::console(string(e.what()));
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


