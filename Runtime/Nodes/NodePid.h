#ifndef NODE_PID_H
#define NODE_PID_H


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

namespace Arro {
class NodePid: public IDevice {
public:
    Trace trace;
	
    double previous_error;
    double integral;
    double derivative;
    double setpoint;
    double actual_position;
    string actual_mode;
    int ms_elapsed;

    double Kp;	// factor for "proportional" control
    double Ki;	// factor for "integral" control
    double Kd;	// factor for "derivative" control
	
    ConfigReader::StringMap params;
    Process* device;
	
    NodePid(Process* device, std::string name, ConfigReader::StringMap p_params);
    void doIt (int ms_elapsed);
    void handleMessage(MessageBuf* msg, std::string padName);
    void runCycle();
};
}

#endif

