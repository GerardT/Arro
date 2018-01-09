#ifndef ARRO_NODE_TIMER_H
#define ARRO_NODE_TIMER_H

#include "arro.pb.h"
#include "Trace.h"
#include "AbstractNode.h"

namespace Arro
{
    class NodeTimer: public IElemBlock {
    public:
        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeTimer(AbstractNode* d, const std::string& name, StringMap& params, TiXmlElement*);
        virtual ~NodeTimer();

        // Copy and assignment is not supported.
        NodeTimer(const NodeTimer&) = delete;
        NodeTimer& operator=(const NodeTimer& other) = delete;

        /**
         * Handle a message that is sent to this node.
         *
         * \param msg Message sent to this node.
         * \param padName name of pad that message was sent to.
         */
        void handleMessage(const MessageBuf& msg, const std::string& padName);

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

        void timer ();
        static void init ();
        static void start ();
        static void stop ();

    private:
        Trace m_trace;
        int m_ticks;
        AbstractNode* m_elemBlock;
        std::string m_actual_mode;
    };
}

#endif
