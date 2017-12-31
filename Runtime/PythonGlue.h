#ifndef ARRO_PYTHON_GLUE_H
#define ARRO_PYTHON_GLUE_H


#include <Python.h>

#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include "arro.pb.h"
#include "ConfigReader.h"
#include "ElemBlock/NodePython.h"
#include "NodeDb.h"
#include "Process.h"

namespace Arro {

    /**
     * \brief C --> Python interface.
     *
     * This class should be used as singleton.
     * Construction it will setup Python interfaces.
     */
    class PythonGlue {
    public:
        /**
         * Constructor, setup the Python interpreter and interface to C.
         * Created and deleted by ServerEngine.
         *
         * Only max one instance of this class may exist at any time.
         *
         * \param filename Name of Python module to load.
         */
        PythonGlue();
        ~PythonGlue();

        // Copy and assignment is not supported.
        PythonGlue(const PythonGlue&) = delete;
        PythonGlue& operator=(const PythonGlue& other) = delete;

        /**
         * Get Python dictionary of the loaded module.
         * Don't Py_DECREF it.
         *
         * \return Python dictionary.
         */
        static PyObject* getDict();

        /**
         * Get Python dictionary of the loaded module.
         * Don't Py_DECREF it.
         *
         * \return Python dictionary.
         */
        static PyObject* getDictApi();

        /**
         * Let NodePython objects register themselves to allow getMessage and sendMessage
         * to forward Python calls to them.
         *
         * \param instance Python instance.
         * \param node NodePython instance that corresponds to Python instance.
         */
        static void registerInstance(PyObject* instance, NodePython* node);

        /**
         * This function getMessage is inserted in Python module and callable by
         * Python code. This function looks up the NodePython object that created the
         * Python object, and will call getMessage on that object.
         *
         * \param self Python self object.
         * \args Python arguments.
         */
        static PyObject* getMessage(PyObject *self, PyObject *args);
        static PyObject* getInput(PyObject * /*self*/, PyObject *args);

        /**
         * This function sendMessage is inserted in Python module and callable by
         * Python code. This function looks up the NodePython object that created the
         * Python object, and will call getMessage on that object.
         *
         * \param self Python self object.
         * \args Python arguments.
         */
        static PyObject* sendMessage(PyObject *self, PyObject *args);

        /**
         * In case of Python error, capture the error code and send to Eclipse client.
         */
        static void captureError();

    private:

        /**
         * Insert all functions from ArroMethods into Python module.
         */
        void insertFunctionToModule();

        /**
         * Load Python program and return its module dictionary
         * so few extra C functions can be inserted into it.
         *
         * \param filename Name of Python module to load.
         */
        PyObject* loadModule();

        Trace m_trace;
        PyObject *m_pModule, *m_pModuleApi, *m_pDict, *m_pDictApi;
        std::map<PyObject *, NodePython*> m_instanceMap;
    };
}

#endif


