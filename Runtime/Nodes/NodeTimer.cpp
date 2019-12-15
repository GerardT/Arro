
// hack, see http://stackoverflow.com/questions/12523122/what-is-glibcxx-use-nanosleep-all-about
#define _GLIBCXX_USE_NANOSLEEP 1
#include <chrono>
#include <thread>
#include <list>
#include <algorithm>

#ifndef ARRO_NODE_TIMER_H
#define ARRO_NODE_TIMER_H

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"

namespace Arro
{

    class NodeTimer: public INodeDefinition {
    public:
        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeTimer(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);
        virtual ~NodeTimer();

        // Copy and assignment is not supported.
        NodeTimer(const NodeTimer&) = delete;
        NodeTimer& operator=(const NodeTimer& other) = delete;

        virtual void finishConstruction();
        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();
        void sendMessage();
        void stop();


    private:
        Trace m_trace;
        int m_ms;
        int m_tag;
        INodeContext* m_elemBlock;
        INodeContext::ItRef m_request;
        INodeContext::ItRef m_tick;
        bool m_clear = false;
    };
}

#endif

using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeTimer> registerMe("Timer");


NodeTimer::NodeTimer(INodeContext* d, const string& /*name*/, StringMap& /* params */, TiXmlElement*):
    m_trace("NodeTimer", true),
    m_elemBlock(d) {
}

void
NodeTimer::finishConstruction() {
    m_request = m_elemBlock->begin(m_elemBlock->getInputPad("timerRequest"), 0, INodeContext::DELTA);
    m_tick = m_elemBlock->end(m_elemBlock->getOutputPad("aTick"));
}


NodeTimer::~NodeTimer() {
    stop();
}

void NodeTimer::runCycle() {

    MessageBuf m1;
    while(m_request->getNext(m1)) {
        TimerRequest* value = new TimerRequest();
        value->ParseFromString(m1->c_str());

        m_ms = value->ms();
        m_tag = value->tag();
        m_trace.println("Setting timer for ms: " + std::to_string(m_ms));

        std::thread t([=]() {
                if(this->m_clear) return;
                std::this_thread::sleep_for(std::chrono::milliseconds(m_ms));
                if(this->m_clear) return;
                sendMessage();
            });
        t.detach();
    }
}

void NodeTimer::stop() {
    this->m_clear = true;
}


void NodeTimer::sendMessage() {

    Tick tick;

    tick.set_ms(m_ms /* elapsed time in ms */);
    tick.set_tag(m_tag);

    try {
        m_tick->setRecord(tick);
    }
    catch(runtime_error&) {
        m_trace.println("Timer failed to update");
    }
}

