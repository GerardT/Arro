#include <PythonGlue.h>  // include before anything else
#include <NodeDb.h>
#include <Nodes/NodePython.h>
#include <ServerEngine.h>

using namespace std;
using namespace Arro;

static RegisterMe<NodePython> registerMe("Python");


std::string hexdump(const char* input, int n)
{
    char dump[100];
    char* d = dump;

    for(int i = 0; i < n && i < 100; i++) {
        sprintf(d, "0x%02X ", input[i]);
        d+=4;
    }
    *d = '\0';
    return std::string(dump);
}

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
    m_pInstance{nullptr},
    m_className{className}
{
    PyObject *pDict = PythonGlue::getDict();

    // Build the name of a callable class
    m_pClass = PyDict_GetItemString(pDict, className.c_str());  // Return value: Borrowed reference

}

void
NodePython::finishConstruction() {

    // Since during construction of the object we don't have its pointer yet...
    PythonGlue::registerTempInstance(this);

    // Create an instance of the class
    if (PyCallable_Check(m_pClass))  // Return value: int
    {
        m_pInstance = PyObject_CallObject(m_pClass, nullptr);  // Return value: New reference.
        if(m_pInstance == nullptr) {
            throw std::runtime_error("Failed to instantiate Python class" + m_className);
        }
        if(PythonGlue::fatal()) {
            m_pInstance = nullptr;
            throw std::runtime_error("Failed to instantiate Python class" + m_className);
        }
        PythonGlue::registerInstance(m_pInstance, this);
    }
    PythonGlue::registerTempInstance(nullptr);
}

NodePython::~NodePython() {
    if(m_pInstance) Py_DECREF(m_pInstance);
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
    // if iterator not found, create it
    if(m_inputs.find(pad) == m_inputs.end()) {
        m_inputs.insert(make_pair(pad, m_elemBlock->begin(m_elemBlock->getInputPad(pad), 0, INodeContext::DELTA)));
    }

    MessageBuf data;
    if(m_inputs.at(pad)->getNext(data)) {
        // m_trace.println(std::string("Get string from ") + pad + " size " + std::to_string(data->length()) + " data " + hexdump(data->c_str(), data->length()));
        if(data->length() == 0 /* MessageBuf may not be initialized */) {
            // insert None object
            Py_INCREF(Py_None);
            return Py_None;
        } else {
            PyObject* tuple = Py_BuildValue("s#", data->c_str(), data->length());  // Return value: New reference.

            return tuple;
        }
    }

    return nullptr; // to remove compiler warning.
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
NodePython::sendMessage(char* padName, char* message, int size) {
    OutputPad* pad = m_elemBlock->getOutputPad(padName);

    // m_trace.println(std::string("Send string to ") + padName + " size " + std::to_string(size) + " data " + hexdump(message, strlen(message)));
    if(pad) {
        MessageBuf s(new string(message, size));
        pad->submitMessage(s);

        Py_INCREF(Py_None);
        return Py_None;
    }
    m_trace.println(string("Unknown Pad ") + padName);
    return nullptr;
}



