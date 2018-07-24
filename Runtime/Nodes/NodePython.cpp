#include <arro.pb.h>
#include <NodeDb.h>
#include <Nodes/NodePython.h>
#include <PythonGlue.h>
#include <ServerEngine.h>

using namespace std;
using namespace Arro;

static RegisterMe<NodePython> registerMe("Python");

/**
 * Create Process instance that executes Python code.
 * Will instantiate an object of class className inside Python that
 * will be used for the lifetime of the Process.
 */
NodePython::NodePython(INodeContext* d, const string& className, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodePython", true},
    m_elemBlock{d},
    m_pFunc{nullptr},
    m_pValue{nullptr},
    m_pArgs{nullptr},
    m_pClass{nullptr},
    m_pInstance{nullptr}
{
    // Since during construction of the object we don't have its pointer yet...
    PythonGlue::registerTempInstance(this);

    PyObject *pDict = PythonGlue::getDict();

    // Build the name of a callable class
    m_pClass = PyDict_GetItemString(pDict, className.c_str());  // Return value: Borrowed reference

}

void
NodePython::finishConstruction() {

    // Create an instance of the class
    if (PyCallable_Check(m_pClass))  // Return value: int
    {
        m_pInstance = PyObject_CallObject(m_pClass, nullptr);  // Return value: New reference.
        if(m_pInstance == nullptr || PythonGlue::fatal()) {
            throw std::runtime_error("Failed to instantiate Python class");
        }
        PythonGlue::registerInstance(m_pInstance, this);
    }
    PythonGlue::registerTempInstance(nullptr);
}

NodePython::~NodePython() {
    Py_DECREF(m_pInstance);
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
 * Python -> C. Returns a message from input.
 */
PyObject*
NodePython::getInputData(const string& pad) {
    MessageBuf data = m_elemBlock->getInputData(m_elemBlock->getInputPad(pad));

    if(data->length() == 0 /* MessageBuf may not be initialized */) {
        // insert None object
        Py_INCREF(Py_None);
        return Py_None;
    } else {
        PyObject* tuple = Py_BuildValue("s", data->c_str());  // Return value: New reference.

        return tuple;
    }
}

/**
 * Python -> C. Returns parameter string.
 */
PyObject*
NodePython::getParameter(const std::string& parm) {
    std::string val = m_elemBlock->getParameter(parm);

    PyObject* tuple = Py_BuildValue("s", val.c_str());  // Return value: New reference.

    return tuple;
}



/**
 * Python -> C. Send message to output Pad of this Process.
 */
PyObject*
NodePython::sendMessage(char* padName, char* message) {
    OutputPad* pad = m_elemBlock->getOutputPad(padName);

    if(pad) {
        pad->submitMessageBuffer(message);

        Py_INCREF(Py_None);
        return Py_None;
    }
    m_trace.println(string("Unknown Pad ") + padName);
    return nullptr;
}



