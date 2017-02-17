#include <unistd.h>

#include <chrono>
#include <thread>
#include "ServerEngine.h"
#include "SocketClient.h"


// Debugger wants to have 'real' main.
extern "C" {
   int main();
}

using namespace std;
using namespace Arro;


int main()
{
    ServerEngine::start();

    SocketClient c{"localhost" , 9000};

    // Never exit, run as server. stop_server();
    sleep(-1);

    return 0;
}

