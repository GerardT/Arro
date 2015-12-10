#ifndef NODE_PASS_H
#define NODE_PASS_H

#include <Trace.h>

#include "NodeDb.h"

class Pad: public INode {
public:
    Trace trace;
    NodeDb::NodeMultiOutput* result;
    NodeDb::NodeSingleInput* in;
    NodeDb::NodeMultiOutput* out;

    Pad(NodeDb& nodeDb, const string& datatype, const string& name);
    void runCycle() {};
};

#endif
