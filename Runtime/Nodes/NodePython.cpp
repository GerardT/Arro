#include <arro.pb.h>
#include <NodeDb.h>
#include <PythonGlue.h>
#include <NodePython.h>
#include <ServerEngine.h>

using namespace std;
using namespace Arro;

static RegisterMe<NodePython> registerMe("Python");

/**
 * Create Process instance that executes Python code.
 * Will instantiate an object of class className inside Python that
 * will be used for the lifetime of the Process.
 */
NodePython::NodePython(AbstractNode* d, const string& className, StringMap& /*params*/, TiXmlElement*):
    m_trace("NodePython", true),
    m_device(d)
{
    PyObject *pDict = PythonGlue::getDict();

    // Build the name of a callable class
    m_pClass = PyDict_GetItemString(pDict, className.c_str());  // Return value: Borrowed reference

    // Create an instance of the class
    if (PyCallable_Check(m_pClass))  // Return value: int
    {
        m_pInstance = PyObject_CallObject(m_pClass, nullptr);  // Return value: New reference.
        if(m_pInstance == nullptr) {
            throw std::runtime_error("Failed to instantiate Python class");
        }
        PythonGlue::registerInstance(m_pInstance, this);
    }
}

NodePython::~NodePython() {
    while(!m_messages.empty()) {
        m_messages.pop();  // FIXME messages should be deleted properly
    }

    Py_DECREF(m_pInstance);
}

/**
 * Store incoming messages into a temp queue so Python code can read them
 * when running execution cycle. Convert message into Python type (tuple) before
 * storing in temp queue.
 */
void
NodePython::handleMessage(const MessageBuf& msg, const string& padName) {
    PyObject* tuple = Py_BuildValue("s s", padName.c_str(), msg->c_str());  // Return value: New reference.

    m_messages.push(tuple);
}

/**
 * C -> Python. Call runCycle for the Python object that was created for
 * this Process.
 */
void
NodePython::runCycle() {
    m_pValue = PyObject_CallMethod(m_pInstance, (char*)"runCycle", nullptr); // no parameters, Return value: New reference.
    if (m_pValue != nullptr)
    {
        Py_DECREF(m_pValue);
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
    if(!m_messages.empty()) {
        PyObject* tuple = m_messages.front();
        m_trace.println("====================> new msg");
        m_messages.pop();
        return tuple;
    } else {
        // insert None object
        Py_INCREF(Py_None);
        return Py_None;
    }
}

/**
 * Python -> C. Returns a message from input.
 */
PyObject*
NodePython::getInputData(const string& pad) {
    MessageBuf data = m_device->getInputData(pad);

    PyObject* tuple = Py_BuildValue("s", data->c_str());  // Return value: New reference.

    return tuple;
}

/**
 * Python -> C. Send message to output Pad of this Process.
 */
PyObject*
NodePython::sendMessage(char* padName, char* message) {
    NodeMultiOutput* pad = m_device->getOutput(padName);

    if(pad) {
        pad->submitMessageBuffer(message);

        Py_INCREF(Py_None);
        return Py_None;
    }
    m_trace.println(string("Unknown Pad ") + padName);
    return nullptr;
}



