#ifndef NODE_LINEAR_H
#define NODE_LINEAR_H


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


class NodeLinear: public IDevice {
private:
    Trace trace;
	Process* device;
public:
	
    double previous_position;
    double setpoint;
    string actual_mode;

    double Inc;
	
    NodeDb::NodeMultiOutput* result;
    ConfigReader::StringMap params;
	
    NodeLinear(Process* d, string name, ConfigReader::StringMap p_params);
    void doIt (int ms_elapsed);
    void handleMessage(MessageBuf* msg, std::string padName);
    void runCycle();
};

#endif

