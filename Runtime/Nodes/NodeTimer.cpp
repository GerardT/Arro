#include <NodeDb.h>
#include <NodeTimer.h>

// hack, see http://stackoverflow.com/questions/12523122/what-is-glibcxx-use-nanosleep-all-about
#define _GLIBCXX_USE_NANOSLEEP 1
#include <chrono>
#include <thread>

using namespace std;
using namespace Arro;

#define TIMEOUT 1000

static std::list<NodeTimer*> timers;
static bool running = false;

NodeTimer::NodeTimer(Process* d, string /*name*/, ConfigReader::StringMap& params):
    trace("NodePid", true),
	device(d) {

    try {
        ticks = stoi(params.at("ms"));
    }
    catch (std::out_of_range) {
        trace.println("### param not found ms ");
    }

    // Add this instance to array of timers.
    timers.push_back(this);
}

NodeTimer::~NodeTimer() {
	timers.remove(this);
}

void NodeTimer::handleMessage(MessageBuf* m, std::string padName) {
	//Class<?> cl = msg.getClass();
    //assert(msg->GetTypeName() == "tutorial.Value");

	if (padName == "mode") {
		Mode* msg = new Mode();
		msg->ParseFromString(m->c_str());

		assert(msg->GetTypeName() == "tutorial.Mode");
		actual_mode = ((Mode*)msg)->mode();
    }
}

void NodeTimer::runCycle() {

}


void NodeTimer::timer () {
    Tick* tick = new Tick();

    tick->set_ms(TIMEOUT /* elapsed time in ms */);

    try {
    	device->getOutput("aTick")->submitMessage(tick);
    }
    catch(runtime_error&) {

    }
}


static void refresh() {
    // int ticks;

    while(true)
    {
    	if(running) {
            for(std::list<NodeTimer*>::iterator it = timers.begin(); it != timers.end() ; ++it) {
        		(*it)->timer();
            }
    	}
        std::chrono::milliseconds timespan(TIMEOUT);
        std::this_thread::sleep_for(timespan);
        //usleep(t->ticks /*ms*/);
    }
}


void NodeTimer::init () {
    // std::thread* first;
	/* first = */new std::thread(refresh);     // spawn new thread that calls refresh()
}

void NodeTimer::start() {
	running = true;
}
void NodeTimer::stop() {
	running = false;
}

