#include <unistd.h>

#include <chrono>
#include <thread>

#include <dlfcn.h>

#include "ServerEngine.h"
#include "SocketClient.h"
#include "ConfigReader.h"
#include "PythonGlue.h"
#include "NodeDb.h"



// Debugger wants to have 'real' main.
extern "C" {
   int main();
}

using namespace std;
using namespace Arro;

Trace* pTrace;

static int newsockfd = -1;
static NodeDb* nodeDb = nullptr;
static PythonGlue* pg = nullptr;
static Trace trace("ServerEngine", true);
static std::map<std::string, Factory > m_elemBlockRegister;
static void* dlib = nullptr;


/**
 * Cleanup after exception of terminate command.
 */
static void cleanup()
{
    trace.println("Cleanup");



    if(nodeDb) {
        /* 1: request change to _terminated */
        Process* mainNode = nodeDb->getMainSfc();
        if(mainNode) {
            mainNode->sendTerminate();

            // FIXME Now sleep 1 sec
            std::chrono::milliseconds timespan(10000);
            std::this_thread::sleep_for(timespan);
        }
        else {
            trace.println("Cleanup failed!");
        }

        /* 1: stop message flow */
        trace.println("-- nodeDb");
        nodeDb->stop();

        /* 2: delete node database */
        delete nodeDb; // will automatically stop timers etc.
        nodeDb = nullptr;
    }

    /* 3: stop python */
    if(pg) {
        trace.println("-- PythonGlue");
        delete pg;
        pg = nullptr;
    }

    dlclose(dlib);

    /* 4: close socket - probably already closed by Eclipse client */
    if(newsockfd != -1) {
        trace.println("-- socket");
        close(newsockfd);
        newsockfd = -1;
    }
    trace.println("Cleanup done");
}

void Arro::SendToConsole(const string& in)
{
    if(newsockfd >= 0) {
        std::string s = in + "\n";
        send(newsockfd, s.c_str(), s.length(), 0);
    }
}

void Arro::registerFactory(const std::string& name, Factory factory) {
    m_elemBlockRegister[name] = factory;
}

bool ServerEngine::getFactory(const std::string& name, Factory& factory) {
    if(m_elemBlockRegister.find(name) != m_elemBlockRegister.end()) {
        factory =  m_elemBlockRegister.at(name);
        return true;
    }
    return false;
}



void startSystem() {
    if(nodeDb)
    {
        SendToConsole("run failed, engine running, terminate first");
    }
    else
    {
        nodeDb = new NodeDb();

        dlib = dlopen("./libnodes.so", RTLD_NOW);
        if(dlib == NULL){
            SendToConsole(dlerror());
            trace.println(string("Runtime error ") + dlerror());

            cleanup();
        }

        try {
            pg = new PythonGlue();

            ConfigReader reader(ARRO_CONFIG_FILE, *nodeDb);
            SendToConsole("loading successful");

            nodeDb->start();
            SendToConsole("run successful");
        } catch ( const std::runtime_error& e ) {
            SendToConsole(e.what());
            trace.println(string("Runtime error ") + e.what());

            cleanup();
        }
    }
}

void stopSystem() {
    cleanup();
}


int main()
{
    pTrace = new Trace("Main", true);

    SocketClient c{"localhost" , 9000};

    startSystem();

    delete pTrace;

    return 0;
}

