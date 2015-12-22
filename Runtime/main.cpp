#include <unistd.h>

#include "ServerEngine.h"

// Debugger wants to have 'real' main.
extern "C" {
   int main();
}

using namespace std;
using namespace Arro;


int main()
{
    ServerEngine::start();

    // Never exit, run as server. stop_server();
    sleep(-1);

    return 0;
}

