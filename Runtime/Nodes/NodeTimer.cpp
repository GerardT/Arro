
// hack, see http://stackoverflow.com/questions/12523122/what-is-glibcxx-use-nanosleep-all-about
#define _GLIBCXX_USE_NANOSLEEP 1
#include <chrono>
#include <thread>
#include <list>
#include <algorithm>

#include <Nodes/NodeTimer.h>

using namespace std;
using namespace Arro;
using namespace arro;

#define ARRO_TIMEOUT 1000

static list<NodeTimer*> timers;
static bool running = false;

static RegisterMe<NodeTimer> registerMe("Timer");



NodeTimer::NodeTimer(INodeContext* d, const string& /*name*/, StringMap& /* params */, TiXmlElement*):
    m_trace("NodePid", true),
    m_elemBlock(d) {

    m_ticks = stod(d->getParameter("ms"));

    // Add this instance to array of timers.
    timers.push_back(this);
}

NodeTimer::~NodeTimer() {
    timers.remove(this);
}

void NodeTimer::runCycle() {
    // empty
}


void NodeTimer::timer () {
    Tick* tick = new Tick();

    tick->set_ms(ARRO_TIMEOUT /* elapsed time in ms */);

    try {
        m_elemBlock->setOutputData(m_elemBlock->getOutputPad("aTick"), tick);
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

