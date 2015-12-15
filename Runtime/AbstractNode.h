#ifndef ARRO_ABSTRACT_NODE_H
#define ARRO_ABSTRACT_NODE_H

#include <string>

namespace Arro
{
    /**
     * \brief Abstract base class for Pad and Process.
     *
     * The name is the instance name: ".node.subnode.subnode"
     *
     * AbstractNode is the NodeDb-internal representation of a Node.
     *
     * A typical Eclipse diagram consists of nodes and pads. The pads are the
     * connection points to other nodes in parent diagrams.
     * Both pads and nodes are implemented as nodes that receive and send messages
     * to each other. The use NodeSingleInput and NodeMultiOutput objects to connect
     * to each other.
     */
    class AbstractNode {
    public:
        /**
         * Constructor.
         *
         * \param n Name of the node.
         */
        AbstractNode(const std::string& n): name(n) {};
        virtual ~AbstractNode() {};

        /**
         * Get name of the node.
         *
         * \return name Name of the node.
         */
        const std::string& getName() const { return name; };

        /**
         * Make the node execute one cycle.
         */
        virtual void runCycle() = 0;

    private:
        std::string name;
    };
}


#endif
