#ifndef ARRO_NODE_PASS_H
#define ARRO_NODE_PASS_H

#include "Trace.h"

namespace Arro {
    class NodePass: public IDevice {
    public:
        /**
         * Constructor
         *
         * \param datatype Data type that this node will pass.
         * \param name Name of this node.
         */
        NodePass(std::string& datatype, std::string& name);
        virtual ~NodePass() {};

        // Copy and assignment is not supported.
        NodePass(const NodePass&) = delete;
        NodePass& operator=(const NodePass& other) = delete;

        /**
         * Handle a message that is sent to this node.
         *
         * \param msg Message sent to this node.
         * \param padName name of pad that message was sent to.
         */
        void handleMessage(MessageBuf* msg, const std::string& padName);

        /**
         * Make the node execute a processing cycle.
         * Empty for a NodePass.
         */
        void runCycle();
    private:
        Trace trace;
        NodeDb::NodeMultiOutput* result;
    };
}

#endif
