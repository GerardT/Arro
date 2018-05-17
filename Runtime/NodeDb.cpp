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
    m_trace{"NodeDb", false},
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
InputPad::handleMessage(const MessageBuf& msg) {
    m_msg = msg;
    // TODO remove sending message (handlemessage)
    m_callback(msg, m_interfaceName);
}

OutputPad::OutputPad(unsigned int padId, NodeDb* n):
    m_nm{n},
    m_inputs{},
    m_padId{padId} {
}

void
OutputPad::connectInput(InputPad* i) {
    if(i) {
        // Another input is listening to this output pad.
        m_inputs.push_back((InputPad*)i);
        i->addOutput(m_padId);
    }
    else {
        m_nm->m_trace.println("### cannot connect ");
    }
}

void
OutputPad::forwardMessage(const MessageBuf& msg) {
    for_each(m_inputs.begin(), m_inputs.end(), [msg](InputPad* i) { i->handleMessage(msg); });
}
void
OutputPad::submitMessage(unsigned int nodeId, google::protobuf::MessageLite* msg) {
    MessageBuf s(new std::string(msg->SerializeAsString()));

    m_nm->m_database.store(nodeId, s);

    auto fm = new NodeDb::FullMsg(this, s);

    std::lock_guard<std::mutex> lock(m_nm->m_mutex);
    m_nm->m_pInQueue->push(fm);

    m_nm->m_condition.notify_one();
    free(msg);
}

// Seem necessary for Python only..
void
OutputPad::submitMessageBuffer(const char* msg) {
    MessageBuf s(new string(msg));
    auto fm = new NodeDb::FullMsg(this, s);

    std::lock_guard<std::mutex> lock(m_nm->m_mutex);
    m_nm->m_pInQueue->push(fm);

    m_nm->m_condition.notify_one();

}



RealNode*
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

RealNode*
NodeDb::registerNode(RealNode* node, const string& name) {
     m_allNodes[name] = std::unique_ptr<RealNode>(node);
     return node;
}

InputPad*
NodeDb::registerNodeInput(RealNode* node, const string& interfaceName,
                          std::function<void (const MessageBuf& msg, const std::string& interfaceName)> listen) {
    auto n = new InputPad(interfaceName, listen, node);
    // If NodePass don't use interfaceName
    if(interfaceName == "") {
        m_allInputs [node->getName()] = unique_ptr<InputPad>(n);
        m_trace.println(string("registering input ") + node->getName());
        return (InputPad*)n;
    } else {
        m_allInputs[node->getName() + ARRO_PAD_SEPARATOR + interfaceName] = unique_ptr<InputPad>(n);
        m_trace.println(("registering input ") + node->getName() + ARRO_PAD_SEPARATOR + interfaceName);
        return (InputPad*)n;
    }
}

OutputPad*
NodeDb::registerNodeOutput(RealNode* node, unsigned int padId, const string& interfaceName) {
    auto n = new OutputPad(padId, this);

    // If NodePass don't use interfaceName
    if(interfaceName == "") {
        m_allOutputs[node->getName()] = unique_ptr<OutputPad>(n);
        m_trace.println("registering output " + node->getName());
        return (OutputPad*)n;
    } else {
        m_allOutputs[node->getName() + ARRO_PAD_SEPARATOR + interfaceName] = unique_ptr<OutputPad>(n);
        m_trace.println("registering output " + node->getName() + ARRO_PAD_SEPARATOR + interfaceName);
        return (OutputPad*)n;
    }
}

OutputPad*
NodeDb::getOutputPad(const string& name) {
    return (OutputPad*)&(*((m_allOutputs[name])));
}

InputPad*
NodeDb::getInputPad(const std::string& name) {
    return (InputPad*)&(*(m_allInputs[name]));  // Since using unique_ptr, we have to get pointer first (*)
}


NodeDb::FullMsg::FullMsg(OutputPad* o /*string s*/, MessageBuf& m) {
    //target = s;
    m_output = o;
    m_msg = m;
}

void
NodeDb::toggleQueue() {
    queue<FullMsg*>* tmp = m_pOutQueue;
    m_pOutQueue = m_pInQueue;
    m_pInQueue = tmp;

    m_database.incRunCycle();
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
                RealNode* an = &(*(it->second));
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
    OutputPad* out = nullptr;
    InputPad* in = nullptr;

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
        out->connectInput((InputPad*)in);
    }
}


