#ifndef ARRO_REAL_NODE_H
#define ARRO_REAL_NODE_H

#include "INodeContext.h"


namespace Arro
{
    /**
     * \brief Abstract base class for Pad and Process.
     *
     * The name is the instance name: ".node.subnode.subnode"
     *
     * INodeContext is the NodeDb-internal representation of a Node.
     *
     * A typical Eclipse diagram consists of nodes and pads. The pads are the
     * connection points to other nodes in parent diagrams.
     * Both pads and nodes are implemented as nodes that receive and send messages
     * to each other. The use InputPad and OutputPad objects to connect
     * to each other.
     */
    class RealNode : public INodeContext {
    public:
        /**
         * Constructor.
         *
         * \param n Name of the node.
         */
        virtual ~RealNode() {};

        /**
         * Make the node execute one cycle.
         */
        virtual void runCycle() = 0;

        virtual void finishConstruction() {};

    };
}



#endif
