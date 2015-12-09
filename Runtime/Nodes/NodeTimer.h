#ifndef NODE_TIMER_H
#define NODE_TIMER_H


#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>
#include <list>
#include <thread>

#include <arro.pb.h>
#include "../ConfigReader.h"
#include "../Process.h"

using namespace std;
using namespace google;
using namespace protobuf;
using namespace arro;


class NodeTimer: public IDevice {
    Trace trace;
private:
	
    int ticks;
    Process* device;
    string actual_mode;
    
public:

    NodeTimer(Process* d, string name, ConfigReader::StringMap& p_params);
    ~NodeTimer();
    void handleMessage(MessageBuf* msg, std::string padName);
    void runCycle();

    void timer ();
    static void init ();
    static void start ();
    static void stop ();
};

#endif
