#include "PythonGlue.h"
#include "ServerEngine.h"

using namespace Arro;
using namespace std;

static PythonGlue* instance = nullptr;


PythonGlue::PythonGlue():
    m_trace{"PythonGlue", true},
    m_pModule{nullptr},
    m_pDict{nullptr},
    m_pDictApi{nullptr}
{
    m_trace.println("Instantiating PythonGlue.");
    if(instance) {
        m_trace.fatal("Tried to instantiate PythonGlue more than once.");
    }

    instance = this;

    // Initialize the Python Interpreter
    Py_Initialize();

    /*
     * Setup for C -> Python.
     */
    PyObject *dot = PyUnicode_FromString(ARRO_FOLDER);  // Return value: New reference.
    PyObject *sys = PyImport_ImportModule("sys");  // Return value: New reference.
    PyObject *path = PyObject_GetAttrString(sys, "path");  //Return value: New reference.
    PyList_Append(path, dot); // Return value: int
    Py_DECREF(sys);
    Py_DECREF(path);
    Py_DECREF(dot);

    if(loadModule() == nullptr) {
        instance = nullptr;
        // Finish the Python interpreter since destructor is not called after throwing exception.
        Py_Finalize();
        throw std::runtime_error("Failed to load");
    }

    /*
     * Setup for Python -> C.
     * Py_Initialize() already done.
     */
    insertFunctionToModule();
}

PythonGlue::~PythonGlue() {
    instance = nullptr;

    m_trace.println("Deleting PythonGlue.");

    /*
     * Cleanup for Python -> C.
     */

    /*
     * Cleanup for C -> Python.
     */
    // Don't Py_DECREF pDict, pDictApi.
    Py_DECREF(m_pModuleApi);
    Py_DECREF(m_pModule);

    // Finish the Python Interpreter
    Py_Finalize();
}


PyObject*
PythonGlue::getMessage(PyObject * /*self*/, PyObject *args)
{
    PyObject *obj;

    if(!PyArg_ParseTuple(args, "O", &obj))  // Return value: int
        return nullptr;

    NodePython* np = instance->m_instanceMap[obj];
    if(!np) {
        return nullptr; // FIXME: do i need to set an error?
    } else {
        return np->getMessage();
    }
}


PyObject*
PythonGlue::getInput(PyObject * /*self*/, PyObject *args)
{
    PyObject *obj;
    const char* pad;

    if(!PyArg_ParseTuple(args, "Os", &obj, &pad))  // Return value: int
        return nullptr;

    instance->m_trace.println(string("parameter padname: ") + pad);

    NodePython* np = instance->m_instanceMap[obj];
    if(!np) {
        Py_INCREF(Py_None);
        return Py_None;
        //return nullptr; // FIXME: do i need to set an error?
    } else {
        PyObject* obj = np->getInputData(pad);
        if(obj == nullptr) {
            Py_INCREF(Py_None);
            return Py_None;
        } else {
            return obj;
        }
    }
}

PyObject*
PythonGlue::sendMessage(PyObject * /*self*/, PyObject *args)
{
    PyObject *obj;
    char* pad;
    char* string;

    if(!PyArg_ParseTuple(args, "Oss", &obj, &pad, &string)) {  // Return value: int
        return nullptr;
    }

    NodePython* np = instance->m_instanceMap[obj];
    if(!np) {
        return nullptr; // FIXME: do i need to set an error?
    } else {
        return np->sendMessage(pad, string);
    }

    Py_INCREF(Py_None);
    return Py_None;
}

/**
 * The table of Python funcions that is added to Python module.
 */
static PyMethodDef ArroMethods[] = {
    {"getMessage",  PythonGlue::getMessage, METH_VARARGS, "Get a message from the queue."},
    {"getInput",    PythonGlue::getInput, METH_VARARGS, "Get a message from the pad."},
    {"sendMessage", PythonGlue::sendMessage, METH_VARARGS, "Send a message into the queue."},
    {nullptr, nullptr, 0, nullptr}        /* Sentinel */
};

PyObject*
PythonGlue::loadModule() {
    // Build the name object
    PyObject *pName = PyUnicode_FromString(ARRO_PROGRAM_FILE);  // Return value: New reference.
    // Load the arro module object
    m_pModule = PyImport_Import(pName);  // Return value: New reference.
    Py_DECREF(pName);

    if (m_pModule != nullptr) {
        m_pDict = PyModule_GetDict(m_pModule);  // Return value: Borrowed reference.
    }
    else {
        captureError();
        return nullptr;
    }

    // Build the name object
    PyObject *pNameApi = PyUnicode_FromString(ARRO_API_FILE);
    // Load the arro_api module object
    m_pModuleApi = PyImport_Import(pNameApi);  // Return value: New reference.
    Py_DECREF(pNameApi);

    if (m_pModuleApi != nullptr) {
        m_pDictApi = PyModule_GetDict(m_pModuleApi);  // Return value: Borrowed reference.
    }
    else {
        captureError();
        return nullptr;
    }

    return m_pDict;
}

void
PythonGlue::insertFunctionToModule() {
    for(PyMethodDef* def = ArroMethods; def->ml_name != nullptr; def++) {
        PyObject *func = PyCFunction_New(def, nullptr);
        PyDict_SetItemString(m_pDictApi, def->ml_name, func);
        Py_DECREF(func);
    }
}

void
PythonGlue::registerInstance(PyObject* obj, NodePython* node) {
    instance->m_instanceMap[obj] = node;
}

PyObject*
PythonGlue::getDict() {
    return instance->m_pDict;
};

PyObject*
PythonGlue::getDictApi() {
    return instance->m_pDictApi;
};

void
PythonGlue::captureError() {
    /*
    # in python this would be...
    import traceback
    string = ' '.join(traceback.format_exc())
    */
    PyObject *type, *value, *traceback, *mod, *list;

    /* Save the current exception */
    PyErr_Fetch(&type, &value, &traceback);  // No return, seems the parameters should NOT be Py_DECREF-ed.
    if(traceback == 0) {
        Py_INCREF(Py_None);
        traceback = Py_None;
    }
    mod = PyImport_ImportModule("traceback");  // Return value: New reference.
    if (!mod) {
        /* print some error */
        return;
    }

    list= PyObject_CallMethod(mod, (char*)"format_exception", (char*)"OOO", type,    value, traceback);  // Return value: New reference.

    Py_ssize_t size = PyList_Size(list);

    for(Py_ssize_t i = 0; i < size; i++) {
        PyObject *item = PyList_GetItem(list, i);  // Return value: Borrowed reference.

#if 0 /* Python3 */
        SendToConsole(string("Python ") + PyUnicode_AsUTF8(item));
#else
        SendToConsole(string("Python ") + PyString_AsString(item));
#endif
    }

    Py_DECREF(list);
    Py_DECREF(mod);
    // PyErr_Clear();
}

