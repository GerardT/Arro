// hack, see http://stackoverflow.com/questions/12523122/what-is-glibcxx-use-nanosleep-all-about
#define _GLIBCXX_USE_NANOSLEEP 1
#include <chrono>
#include <thread>
#include <list>
#include <algorithm>

#include "NodeTimer.h"

using namespace std;
using namespace Arro;
using namespace arro;

#define ARRO_TIMEOUT 1000

static list<NodeTimer*> timers;
static bool running = false;

static RegisterMe<NodeTimer> registerMe("Timer");



NodeTimer::NodeTimer(AbstractNode* d, const string& /*name*/, StringMap& params, TiXmlElement*):
    m_trace("NodePid", true),
    m_device(d) {

    try {
        m_ticks = stoi(params.at("ms"));
    }
    catch (std::out_of_range) {
        m_trace.println("### param not found ms ");
    }

    // Add this instance to array of timers.
    timers.push_back(this);
}

NodeTimer::~NodeTimer() {
    timers.remove(this);
}

void NodeTimer::handleMessage(const MessageBuf& m, const std::string& padName) {
    if (padName == "mode") {
        Mode* msg = new Mode();
        msg->ParseFromString(m->c_str());

        assert(msg->GetTypeName() == "tutorial.Mode");
        m_actual_mode = ((Mode*)msg)->mode();
    }
}

void NodeTimer::runCycle() {
    // empty
}


void NodeTimer::timer () {
    Tick* tick = new Tick();

    tick->set_ms(ARRO_TIMEOUT /* elapsed time in ms */);

    try {
        m_device->setOutputData(m_device->getOutput("aTick"), tick);
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

