#ifndef ARRO_NODE_PYTHON_H
#define ARRO_NODE_PYTHON_H

#include <queue>
#include <map>
#include <Python.h>

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"


namespace Arro {
    class NodePython: public INodeDefinition {
    public:
        /**
         * Constructor
         *
         * \param elemBlock The Process node instance.
         * \param className Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodePython(INodeContext* elemBlock, const std::string& className, StringMap& params, TiXmlElement*);
        virtual ~NodePython();

        // Copy and assignment is not supported.
        NodePython(const NodePython&) = delete;
        NodePython& operator=(const NodePython& other) = delete;

        virtual void finishConstruction();

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();
        PyObject* getMessage();
        PyObject* getInputData(const std::string& pad);
        PyObject* sendMessage(char* pad, char* message);
        PyObject* getParameter(const std::string& parm);

    private:
        Trace m_trace;
        StringMap m_params;
        INodeContext* m_elemBlock;
        PyObject *m_pFunc, *m_pValue, *m_pArgs, *m_pClass, *m_pInstance;
        std::map<std::string, INodeContext::ItRef> m_inputs;
    };
}

#endif

