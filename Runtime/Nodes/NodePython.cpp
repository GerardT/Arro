#include <arro.pb.h>
#include <NodeDb.h>
#include <PythonGlue.h>
#include <NodePython.h>
#include <ServerEngine.h>

using namespace std;
using namespace Arro;


/**
 * Create Process instance that executes Python code.
 * Will instantiate an object of class className inside Python that
 * will be used for the lifetime of the Process.
 */
NodePython::NodePython(Process* d, string& className, ConfigReader::StringMap /*params*/):
    trace("NodePython", true),
	device(d)
{
	PyObject *pDict = PythonGlue::getDict();

	// Build the name of a callable class
	pClass = PyDict_GetItemString(pDict, className.c_str());

	// Create an instance of the class
	if (PyCallable_Check(pClass))
	{
		pInstance = PyObject_CallObject(pClass, nullptr);
		if(pInstance == nullptr) {
	        throw std::runtime_error("Failed to instantiate Python class");
		}
		PythonGlue::registerInstance(pInstance, this);
	}
}

/**
 * Destructor.
 */
NodePython::~NodePython() {
    while(!messages.empty()) {
        messages.pop();  // FIXME messages should be deleted properly
    }
}

/**
 * Store incoming messages into a temp queue so Python code can read them
 * when running execution cycle. Convert message into Python type (tuple) before
 * storing in temp queue.
 */
void
NodePython::handleMessage(MessageBuf* msg, std::string padName) {
	PyObject* tuple = Py_BuildValue("s s", padName.c_str(), msg->c_str());

	messages.push(tuple);
}

/**
 * C -> Python. Call runCycle for the Python object that was created for
 * this Process.
 */
void
NodePython::runCycle() {
	pValue = PyObject_CallMethod(pInstance, "runCycle", nullptr); // no parameters
	if (pValue != nullptr)
	{
		printf("Return of call : %ld\n", PyInt_AsLong(pValue));
		Py_DECREF(pValue);
	}
	else
	{
		PythonGlue::captureError();

	    throw std::runtime_error("Failed to call Python method");
	}
}

/**
 * Python -> C. Returns a message from temp queue to Python.
 */
PyObject*
NodePython::getMessage() {
    if(!messages.empty()) {
        PyObject* tuple = messages.front();
        trace.println("====================> new msg");
        messages.pop();
        return tuple;
    } else {
    	// insert None object
        Py_INCREF(Py_None);
        return Py_None;
    }
}

/**
 * Python -> C. Send message to output Pad of this Process.
 */
PyObject*
NodePython::sendMessage(char* padName, char* message) {
	NodeDb::NodeMultiOutput* pad = device->getOutput(padName);

    if(pad) {
    	pad->submitMessageBuffer(message);

	    Py_INCREF(Py_None);
	    return Py_None;
    }
    return nullptr;
}



