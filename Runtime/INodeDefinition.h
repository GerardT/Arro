#ifndef ARRO_I_NODE_DEFINITION_H
#define ARRO_I_NODE_DEFINITION_H

#include <string>
#include <memory>

namespace Arro {
    typedef std::shared_ptr<const std::string> MessageBuf;


    /**
     * \brief Interface to be implemented by all Nodes.
     *
     * An ElemBlock is named 'Node' in this Runtime code.
     *
     * This interface is for implementing node instances:
     * - handleMessage -> obsolete
     * - runCycle will trigger running one execution cycle.
     */
    class INodeDefinition {
    public:
        virtual ~INodeDefinition() {};

        virtual void test() {};

        virtual void finishConstruction() {};

        /**
         * Only for SFC nodes
         * @param name
         * @param sfc
         */
        virtual void registerChildSfc(const std::string& /*name*/, INodeDefinition* /*sfc*/) {};
        virtual void sendTerminate() {}  // Only from 'Main' node

        /**
         * Handle a message that is sent to this node.
         *
         * \param msg Message sent to this node.
         * \param padName name of pad that message was sent to.
         */
        virtual void handleMessage(const MessageBuf& /*msg*/, const std::string& /*padName*/) {};

        /**
         * Make the node execute a processing cycle.
         */
        virtual void runCycle() = 0;
    };
}

#endif


