#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <unordered_map>
#include <map>
#include <algorithm>

#include "ServerEngine.h"
#include "NodeDb.h"
#include "RealNode.h"


using namespace Arro;
using namespace std;

static std::function<void()> timerStart = nullptr;
static std::function<void()> timerStop = nullptr;


NodeDb::NodeDb():
    m_trace{"NodeDb", true},
    m_allInputs{},
    m_allOutputs{},
    m_allNodes{},
    m_running{false},
    m_thrd{nullptr},
    m_sfcNode{nullptr} {
}

NodeDb::~NodeDb() {
}

InputPad::InputPad(NodeDb* nm, const std::string& interfaceName,
        std::function<void ()> listenUpdate,
        RealNode* n):
    m_nm{nm},
    m_listenUpdate{listenUpdate},
    m_outputs{},
    m_node{n},
    m_interfaceName{interfaceName} {}


//
// get in order:
// * all messages of one input
// * then next input, etc.
/*deprecated*/ bool
InputPad::getData(unsigned int padId, MessageBuf& tmp) {
    // check if there is a message at m_outputPadId with latest runCycle
    return m_nm->getLatestMessage(this, padId, tmp);
};

INodeContext::ItRef
InputPad::begin(unsigned int connection, INodeContext::Mode mode) {
    return m_nm->begin(this, connection, mode);
}

const std::list<unsigned int>
InputPad::getConnections() {
    std::list<unsigned int> pads;
    for(auto it = m_outputs.begin(); it != m_outputs.end(); ++it) {
        pads.push_back((*it)->getPadId());
    }
    return pads;
};


OutputPad*
InputPad::getOutputPad(unsigned int padId)
{
    return m_nm->getOutputPad(padId);
};

OutputPad::OutputPad(unsigned int padId, NodeDb* db, RealNode* n):
    m_nm{db},
    m_node{n},
    m_inputs{},
    m_padId{padId},
    m_first{true} {
        m_it = m_nm->m_database.end(this, padId);
}

INodeContext::ItRef
OutputPad::end() {
    return m_nm->end(this, m_padId);
}

void
OutputPad::connectInput(InputPad* pad) {
    if(pad) {
        // Make 2-way connection
        addInput(pad);
        pad->addOutput(this);
    }
    else {
        m_nm->m_trace.println("### cannot connect ");
    }
}

// Change to ReplaceInto (as in SQLite)
/*deprecated*/ void
OutputPad::submitMessage(MessageBuf& s) {

    if(m_first) {
        m_it->insertRecord(s);
        m_first = false;
    } else {
        m_it->updateRecord(s);
    }

    m_nm->m_database.getConditionVariable().notify_one();
}

void
OutputPad::notifyInputs() {
    for(auto it = m_inputs.begin(); it != m_inputs.end(); ++it) {
        // notify listener
        (*it)->notifyUpdate();
    }
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
NodeDb::registerNodeInput(RealNode* node, const std::string& interfaceName,
        std::function<void ()> listenUpdate) {
    auto n = new InputPad(this, interfaceName, listenUpdate, node);
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
    auto n = new OutputPad(padId, this, node);

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
    OutputPad* out = nullptr;
    try {
        out = m_allOutputs[name].get();
    }
    catch (std::out_of_range&) {
        m_trace.println("### non-registered output " + name);
        throw std::runtime_error("### non-registered output " + name);
    }
    return out;
}

OutputPad*
NodeDb::getOutputPad(unsigned int padId) {
    for(auto it = m_allOutputs.begin(); it != m_allOutputs.end(); ++it) {
        OutputPad* op = it->second.get();

        if(op->getPadId() == padId) {
            return op;
        }
    }
    return nullptr;
}

InputPad*
NodeDb::getInputPad(const std::string& name) {
    return (InputPad*)&(*(m_allInputs[name]));  // Since using unique_ptr, we have to get pointer first (*)
}

void
NodeDb::runCycle(NodeDb* nm) {

    try {
        while(nm->m_running)
        {
            std::list<unsigned int> updates = nm->m_database.incRunCycle();

            // Complicated way to notify inputs if output(s) changed:
            // Find all inputs that are connected to updated outputs
            for(auto it = updates.begin(); it != updates.end(); ++it) {
                unsigned int output = *it;

                OutputPad* out = nm->getOutputPad(output);

                out->notifyInputs();
            }

            /* Then trigger all runCycle methods on nodes */
            for(auto it = nm->m_allNodes.begin(); it != nm->m_allNodes.end(); ++it) {
                RealNode* an = it->second.get();
                ((RealNode*)(an))->runCycle();
            }

            if(nm->m_database.noMoreUpdates()) {
                std::unique_lock<std::mutex> lock(nm->m_database.getDbLock());

                // And wait until new message arrive in db
                nm->m_database.getConditionVariable().wait(lock);  // keep waiting if db empty
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

    if(timerStart) {
        timerStart();
    } else {
        m_trace.println("timerStart not initialized ...");
    }
}

void
NodeDb::stop() {
    if(m_running) {
        m_trace.println("Stopping ...");

        m_running = false;

        m_database.getConditionVariable().notify_one();

        if(timerStop) {
            timerStop();
        } else {
            m_trace.println("timerStop not initialized ...");
        }

        m_thrd->join();
        delete m_thrd;
        m_thrd = nullptr;
    }
}


void
NodeDb::connect(const string& output, const string& input) {
    OutputPad* out = nullptr;
    InputPad* in = nullptr;

    try {
        out = m_allOutputs[output].get();
    }
    catch (std::out_of_range&) {
        m_trace.println("### non-registered output " + output);
        throw std::runtime_error("### non-registered output " + output);
    }
    try {
        in = &(*(m_allInputs[input]));
    }
    catch (std::out_of_range&) {
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

void
NodeDb::registerMainSfc(Process* sfcNode) {
    m_sfcNode = sfcNode;
}


void registerTimerStartStop(std::function<void()> start, std::function<void()> stop) {
    timerStart = start;
    timerStop = stop;
}



