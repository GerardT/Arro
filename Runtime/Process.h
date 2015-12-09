#ifndef NODE_DEVICE_H
#define NODE_DEVICE_H

#include <Trace.h>
#include <ConfigReader.h>
#include "NodeDb.h"

class INode;
class NodeMultiOutput;
class IPadListener;
class NodeDb;

class null_pointer: public exception
{
  virtual const char* what() const throw()
  {
    return "My exception happened";
  }
};


class Process: public INode {
private:
    Trace trace;
    NodeDb& nodeDb;
    IDevice* device;

    IPadListener* listener;

    bool doRunCycle;

    void getPrimitive(const string* url, string* instance, ConfigReader::StringMap& params);

public:

    Process(NodeDb& nodeDb, const string* url, string* instance, ConfigReader::StringMap params);
    ~Process();
    void registerInput(string interfaceName, bool enableRunCycle);
    void registerOutput(string interfaceName);
    void setListener(IPadListener* listener) { this->listener = listener; };
    NodeDb::NodeMultiOutput* getOutput(string name);
    void runCycle();
};

#endif
