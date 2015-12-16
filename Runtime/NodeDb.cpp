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
    outQueue(),
    pOutQueue(&outQueue),
    running(false),
	thrd(nullptr) {
}

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


    // Not sure below can be done with std::for_each loop.
    for(map<string, NodeMultiOutput*>::iterator it = allOutputs.begin(); it != allOutputs.end() ; ++it) {
        pair<string, NodeMultiOutput*> elt = *it;
        delete elt.second;
    }
    for(map<string, NodeSingleInput*>::iterator it = allInputs.begin(); it != allInputs.end() ; ++it) {
        pair<string, NodeSingleInput*> elt = *it;
        delete elt.second;
    }

    for(map<string, AbstractNode*>::iterator it = allNodes.begin(); it != allNodes.end() ; ++it) {
        pair<string, AbstractNode*> elt = *it;
        delete elt.second;
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
    MessageBuf* s = new MessageBuf(msg);
    FullMsg* fm = new FullMsg(this, s);
    nm->pInQueue->push(fm);
}



AbstractNode*
NodeDb::getNode(const string& name) {
    try {
        AbstractNode* n = allNodes[name];
        return (AbstractNode*)n;
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
    NodeDb::NodeSingleInput* n = new NodeDb::NodeSingleInput(/*interfaceName, */listen, node);
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
    NodeDb::NodeMultiOutput* n = new NodeDb::NodeMultiOutput(/*interfaceName*/this);
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
NodeDb::toggleQueue() {
      queue<FullMsg*>* tmp = pOutQueue;
      pOutQueue = pInQueue;
      pInQueue = tmp;
}



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
                if(fm != nullptr) {
                    MessageBuf* msg = fm->msg;
                    fm->output->forwardMessage(msg);
                    delete msg;
                    delete fm;

                }
            }

            /* Then trigger all runCycle methods on nodes */
            for_each(nm->allNodes.begin(), nm->allNodes.end(), [](pair<string, AbstractNode*> n) { n.second->runCycle(); });

            /* And switch the queues */
            nm->toggleQueue();
        }
    } catch (std::runtime_error& e) {
    }
}

void
NodeDb::start() {
    running = true;

    thrd = new thread(&NodeDb::runCycle, this);

    NodeTimer::start();
}

void
NodeDb::stop() {
    if(running) {
        running = false;

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


