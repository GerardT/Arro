#ifndef NODE_SERVO_H
#define NODE_SERVO_H


#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include <arro.pb.h>
#include "../ConfigReader.h"
#include "../NodeDb.h"

using namespace std;
using namespace google;
using namespace protobuf;
using namespace arro;

namespace Arro {
class NodeServo: public IDevice {
public:
    Trace trace;
	
    double previous_position;
    double actual_position;
    string actual_mode;

    int Ch;
	
    NodeDb::NodeMultiOutput* result;
    ConfigReader::StringMap params;
	
    NodeServo(std::string name, ConfigReader::StringMap p_params);
    void doIt (int ms_elapsed);
    void handleMessage(MessageBuf* msg, std::string padName);
    void runCycle();
};
}

#endif

