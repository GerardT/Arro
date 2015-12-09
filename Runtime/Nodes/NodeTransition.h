#ifndef NODE_TRANSITION_H
#define NODE_TRANSITION_H



#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include <arro.pb.h>
#include "../ConfigReader.h"
#include "../NodeDb.h"
#include "../Process.h"

using namespace std;
using namespace google;
using namespace protobuf;
using namespace arro;


class NodeTransition: public IDevice {
private:
	Process* device;
public:
    Trace trace;

    NodeDb::NodeMultiOutput* result;
    NodeDb::NodeMultiOutput* p_mode;
	
    double value;
    bool hasToken;
	
    NodeTransition(Process* d, string name, ConfigReader::StringMap params);
    void doIt ();
    void handleMessage(MessageBuf* msg, std::string padName);
    void runCycle();
};

#endif

