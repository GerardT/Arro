#ifndef NODE_START_TOKEN_H
#define NODE_START_TOKEN_H


#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include <arro.pb.h>
#include "../ConfigReader.h"
#include "../Process.h"
#include "../NodeDb.h"

using namespace std;
using namespace google;
using namespace protobuf;
using namespace arro;


class NodeStartToken: public IDevice {
private:
	Process* device;
public:
    Trace trace;

    NodeDb::NodeMultiOutput* result;
    NodeDb::NodeMultiOutput* port_mode;
	
    NodeStartToken(Process* d, string name, ConfigReader::StringMap& params);
    void doIt ();
    void handleMessage(MessageBuf* msg, std::string padName);
    void runCycle();
};

#endif
