#ifndef PYTHON_H
#define PYTHON_H


#include "Python.h"

#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include <arro.pb.h>
#include "../ConfigReader.h"
#include "../NodeDb.h"
#include "../Process.h"
#include "NodePython.h"

using namespace std;
using namespace google;
using namespace protobuf;
using namespace arro;

/**
 * C <-> Python interface. This clas should be used as singleton.
 * Construction it will setup Python interfaces.
 */
class PythonGlue {
private:
    PyObject *pModule, *pDict;

	PyObject* loadModule(char* filename);
	void insertFunctionToModule();

    std::map<PyObject *, NodePython*> instanceMap;

public:
	PythonGlue(char* filename);
	~PythonGlue();

	static PyObject* getDict();
	static void registerInstance(PyObject* instance, NodePython* node);
	static PyObject* getMessage(PyObject *self, PyObject *args);
	static PyObject* sendMessage(PyObject *self, PyObject *args);
	static void captureError();
};


#endif


