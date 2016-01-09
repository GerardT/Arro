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
NodePython::NodePython(Process* d, string& className, ConfigReader::StringMap& /*params*/):
    trace("NodePython", true),
    device(d)
{
    PyObject *pDict = PythonGlue::getDict();

    // Build the name of a callable class
    pClass = PyDict_GetItemString(pDict, className.c_str());  // Return value: Borrowed reference

    // Create an instance of the class
    if (PyCallable_Check(pClass))  // Return value: int
    {
        pInstance = PyObject_CallObject(pClass, nullptr);  // Return value: New reference.
        if(pInstance == nullptr) {
            throw std::runtime_error("Failed to instantiate Python class");
        }
        PythonGlue::registerInstance(pInstance, this);
    }
}

NodePython::~NodePython() {
    while(!messages.empty()) {
        messages.pop();  // FIXME messages should be deleted properly
    }

    Py_DECREF(pInstance);
}

/**
 * Store incoming messages into a temp queue so Python code can read them
 * when running execution cycle. Convert message into Python type (tuple) before
 * storing in temp queue.
 */
void
NodePython::handleMessage(MessageBuf* msg, const string& padName) {
    PyObject* tuple = Py_BuildValue("s s", padName.c_str(), msg->c_str());  // Return value: New reference.

    messages.push(tuple);
}

/**
 * C -> Python. Call runCycle for the Python object that was created for
 * this Process.
 */
void
NodePython::runCycle() {
    pValue = PyObject_CallMethod(pInstance, "runCycle", nullptr); // no parameters, Return value: New reference.
    if (pValue != nullptr)
    {
        Py_DECREF(pValue);
    }
    else
    {
        PythonGlue::captureError();

        throw std::runtime_error("Calling runCycle inside Python resulted in error");
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



