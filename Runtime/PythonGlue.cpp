#include "PythonGlue.h"
#include "ServerEngine.h"

using namespace Arro;
using namespace std;

static PythonGlue* instance = nullptr;


PythonGlue::PythonGlue():
    trace("PythonGlue", true),
    pModule(nullptr),
    pDict(nullptr),
    pDictApi(nullptr)
{
    if(instance) {
        trace.fatal("Tried to instantiate PythonGlue more than once.");
    }

    instance = this;

    // Initialize the Python Interpreter
    Py_Initialize();

    /*
     * Setup for C -> Python.
     */
    PyObject *dot = PyString_FromString(".");
    PyObject *sys = PyImport_ImportModule("sys");
    PyObject *path = PyObject_GetAttrString(sys, "path");
    PyList_Append(path, dot);
    Py_DECREF(sys);
    Py_DECREF(path);
    Py_DECREF(dot);

    if(loadModule() == nullptr) {
        // Undo Py_Initialize
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

    /*
     * Cleanup for Python -> C.
     */

    /*
     * Cleanup for C -> Python.
     */
    // Don't Py_DECREF pDict, pDictApi, pModule, pModuleApi since it will crash ModuleImport after restart.

    // Finish the Python Interpreter
    Py_Finalize();
}


PyObject*
PythonGlue::getMessage(PyObject * /*self*/, PyObject *args)
{
    PyObject *obj;

    if(!PyArg_ParseTuple(args, "O", &obj))
        return nullptr;

    NodePython* np = instance->instanceMap[obj];
    if(!np) {
        return nullptr; // FIXME: do i need to set an error?
    } else {
        return np->getMessage();
    }
}

PyObject*
PythonGlue::sendMessage(PyObject * /*self*/, PyObject *args)
{
    PyObject *obj;
    char* pad;
    char* string;

    if(!PyArg_ParseTuple(args, "Oss", &obj, &pad, &string)) {
        return nullptr;
    }

    NodePython* np = instance->instanceMap[obj];
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
    {"sendMessage",  PythonGlue::sendMessage, METH_VARARGS, "Send a message into the queue."},
    {nullptr, nullptr, 0, nullptr}        /* Sentinel */
};

PyObject*
PythonGlue::loadModule() {
    // Build the name object
    PyObject *pName = PyString_FromString(ARRO_PROGRAM_FILE);

    // Load the arro module object
    pModule = PyImport_Import(pName);
    Py_DECREF(pName);

    if (pModule != nullptr) {
        pDict = PyModule_GetDict(pModule);
    }
    else {
        captureError();
        return nullptr;
    }

    // Build the name object
    PyObject *pNameApi = PyString_FromString(ARRO_API_FILE);
    // Load the arro_api module object
    pModuleApi = PyImport_Import(pNameApi);
    Py_DECREF(pNameApi);

    if (pModuleApi != nullptr) {
        pDictApi = PyModule_GetDict(pModuleApi);
    }
    else {
        captureError();
        return nullptr;
    }

    return pDict;
}

void
PythonGlue::insertFunctionToModule() {
    for(PyMethodDef* def = ArroMethods; def->ml_name != nullptr; def++) {
        PyObject *func = PyCFunction_New(def, nullptr);
        PyDict_SetItemString(pDictApi, def->ml_name, func);
        Py_DECREF(func);
    }
}

void
PythonGlue::registerInstance(PyObject* obj, NodePython* node) {
    instance->instanceMap[obj] = node;
}

PyObject*
PythonGlue::getDict() {
    return instance->pDict;
};

PyObject*
PythonGlue::getDictApi() {
    return instance->pDictApi;
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
    PyErr_Fetch(&type, &value, &traceback);
    if(traceback == 0) {
        Py_INCREF(Py_None);
        traceback = Py_None;
    }
    mod = PyImport_ImportModule("traceback");
    if (!mod) {
        /* print some error */
        return;
    }

    list= PyObject_CallMethod(mod, "format_exception", "OOO", type,    value, traceback);

    Py_ssize_t size = PyList_Size(list);

    for(Py_ssize_t i = 0; i < size; i++) {
        PyObject *item = PyList_GetItem(list, i);

        ServerEngine::console(string("Python ") + PyString_AsString(item));

        // Doing DECREF on item makes Python crash next time. Not sure why...
        // Py_DECREF(item);
    }

    Py_DECREF(list);
    Py_DECREF(mod);
    Py_DECREF(type);
    Py_DECREF(value);
    Py_DECREF(traceback);
    // PyErr_Clear();
}

