
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

        void timer ();
        static void init ();
        static void start ();
        static void stop ();

    private:
        Trace m_trace;
        int m_ticks;
        INodeContext* m_elemBlock;
        INodeContext::ItRef m_tick;
    };
}

#endif

using namespace std;
using namespace Arro;
using namespace arro;

#define ARRO_TIMEOUT 1000

static list<NodeTimer*> timers;
static bool running = false;

static RegisterMe<NodeTimer> registerMe("Timer");

static bool instantiated = false;

void registerTimerStartStop(std::function<void()> start, std::function<void()> stop);

NodeTimer::NodeTimer(INodeContext* d, const string& /*name*/, StringMap& /* params */, TiXmlElement*):
    m_trace("NodePid", true),
    m_elemBlock(d) {

    if(!instantiated) {
        instantiated = true;

        NodeTimer::init ();
        registerTimerStartStop(NodeTimer::start, NodeTimer::stop);
    }

    m_ticks = stod(d->getParameter("ms"));

    // Add this instance to array of timers.
    timers.push_back(this);
}

void
NodeTimer::finishConstruction() {
    m_tick = m_elemBlock->end(m_elemBlock->getOutputPad("aTick"));
}


NodeTimer::~NodeTimer() {
    timers.remove(this);
}

void NodeTimer::runCycle() {
    // empty
}


void NodeTimer::timer () {
    Tick tick;

    tick.set_ms(ARRO_TIMEOUT /* elapsed time in ms */);

    try {
        m_tick->setRecord(tick);
    }
    catch(runtime_error&) {
        m_trace.println("Timer failed to update");
    }
}


static void refresh() {
    while(true)
    {
        if(running) {
            for_each(timers.begin(), timers.end(), [](NodeTimer* t) {t->timer(); });
        }
        std::chrono::milliseconds timespan(ARRO_TIMEOUT);
        std::this_thread::sleep_for(timespan);
    }
}


void NodeTimer::init () {
    // thread will run forever
    /* std::thread* first = */new std::thread(refresh);     // spawn new thread that calls refresh()
}

void NodeTimer::start() {
    running = true;
}
void NodeTimer::stop() {
    running = false;
}

