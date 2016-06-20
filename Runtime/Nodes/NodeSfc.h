#ifndef ARRO_NODE_SFC_H
#define ARRO_NODE_SFC_H

#include "arro.pb.h"
#include "ConfigReader.h"
#include "NodeDb.h"
#include "Process.h"

namespace Arro {
    class NodeSfc: public IDevice {
    public:
        /**
         * Constructor
         *
         * \param device The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeSfc(Process* device, TiXmlElement* elt);
        virtual ~NodeSfc() {};

        // Copy and assignment is not supported.
        NodeSfc(const NodeSfc&) = delete;
        NodeSfc& operator=(const NodeSfc& other) = delete;

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

    private:
        Trace trace;
        Process* device;
    };
}

#endif

