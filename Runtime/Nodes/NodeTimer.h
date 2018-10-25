#ifndef ARRO_NODE_TIMER_H
#define ARRO_NODE_TIMER_H

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"

namespace Arro
{
    class NodeTimer: public INodeDefinition {
    public:
        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeTimer(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);
        virtual ~NodeTimer();

        // Copy and assignment is not supported.
        NodeTimer(const NodeTimer&) = delete;
        NodeTimer& operator=(const NodeTimer& other) = delete;

        virtual void finishConstruction();
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
        INodeContext* m_elemBlock;
        INodeContext::ItRef m_tick;
    };
}

#endif
