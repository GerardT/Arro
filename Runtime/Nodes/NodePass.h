#ifndef NODE_PASS_H
#define NODE_PASS_H

#include <Trace.h>


class NodePass: public IDevice {
public:
    Trace trace;
    NodeDb::NodeMultiOutput* result;
	
    NodePass(string datatype, string name);
    void handleMessage(MessageBuf* msg, std::string padName);
    void runCycle();
};

#endif
