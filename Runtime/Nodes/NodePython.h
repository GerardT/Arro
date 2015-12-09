#ifndef NODE_PYTHON_H
#define NODE_PYTHON_H


#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>
#include <queue>
#include <arro.pb.h>

#include "Python.h"

#include "../ConfigReader.h"
#include "../NodeDb.h"
#include "../Process.h"

using namespace std;
using namespace google;
using namespace protobuf;
using namespace arro;


class NodePython: public IDevice {
private:
    PyObject *pFunc, *pValue, *pArgs, *pClass, *pInstance;

    queue<PyObject*> messages;

public:
    Trace trace;

    ConfigReader::StringMap params;
    Process* device;

    NodePython(Process* device, string& className, ConfigReader::StringMap p_params);
    ~NodePython();
    void handleMessage(MessageBuf* msg, std::string padName);
    void runCycle();
    PyObject* getMessage();
    PyObject* sendMessage(char* pad, char* string);
};

#endif

