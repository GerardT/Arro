
#include <iostream>
#include <vector>
#include <exception>

#include <Trace.h>
#include <ConfigReader.h>
#include <NodeDb.h>
#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <unordered_map>

// Debugger wants to have 'real' main.
extern "C" {
   int main();
}

using namespace std;
using namespace Arro;

#include <ServerEngine.h>

int main()
{
    cout << "Running Arro" << endl;

    ServerEngine::start();

    // Never exit, run as server. stop_server();
    sleep(-1);

    return 0;
}

