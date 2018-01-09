#ifndef ARRO_IDEVICE
#define ARRO_IDEVICE

#include <string>
#include <memory>

namespace Arro {
    typedef std::shared_ptr<const std::string> MessageBuf;


    /**
     * \brief Interface to be implemented by all elemBlocks.
     *
     * This interface is for implementing elemBlock instances:
     * - handleMessage will pass data to the elemBlock.
     * - runCycle will trigger running one execution cycle.
     */
    class IElemBlock {
    public:
        virtual ~IElemBlock() {};

        virtual void test() {};

        virtual void finishConstruction() {};

        /**
         * Handle a message that is sent to this node.
         *
         * \param msg Message sent to this node.
         * \param padName name of pad that message was sent to.
         */
        virtual void handleMessage(const MessageBuf& msg, const std::string& padName) = 0;

        /**
         * Make the node execute a processing cycle.
         */
        virtual void runCycle() = 0;
    };
}

#endif


