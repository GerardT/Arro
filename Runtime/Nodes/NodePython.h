#ifndef ARRO_NODE_PYTHON_H
#define ARRO_NODE_PYTHON_H

#include <queue>
#include <Python.h>

#include "arro.pb.h"
#include "ConfigReader.h"
#include "NodeDb.h"
#include "Process.h"


namespace Arro {
    class NodePython: public IDevice {
    public:
        /**
         * Constructor
         *
         * \param device The Process node instance.
         * \param className Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodePython(Process* device, std::string& className, ConfigReader::StringMap& params);
        virtual ~NodePython();

        // Copy and assignment is not supported.
        NodePython(const NodePython&) = delete;
        NodePython& operator=(const NodePython& other) = delete;

        /**
         * Handle a message that is sent to this node.
         *
         * \param msg Message sent to this node.
         * \param padName name of pad that message was sent to.
         */
        void handleMessage(MessageBuf* msg, const std::string& padName);

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();
        PyObject* getMessage();
        PyObject* sendMessage(char* pad, char* message);

    private:
        Trace m_trace;
        ConfigReader::StringMap m_params;
        Process* m_device;
        PyObject *m_pFunc, *m_pValue, *m_pArgs, *m_pClass, *m_pInstance;
        std::queue<PyObject*> m_messages;
    };
}

#endif

