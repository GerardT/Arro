#ifndef ARRO_PAD_H
#define ARRO_PAD_H

#include "Trace.h"
#include "NodeDb.h"

namespace Arro
{

    /**
     * \brief Input or output pad.
     *
     * An instance of this class will just forward messages to/from the associated node.
     */
    class Pad: public AbstractNode {
    public:

        /**
         * Constructor.
         *
         * \param nodeDb Node database.
         * \param datatype Datatype of messages that this pad should pass.
         * \param name Name of the pad.
         */
        Pad(NodeDb& nodeDb, const std::string& datatype, const std::string& name);
        virtual ~Pad() {};

        // Copy and assignment is not supported.
        Pad(const Pad&) = delete;
        Pad& operator=(const Pad& other) = delete;

        /**
         * Dummy implementation of virtual function since a Pad cannot run.
         */
        void runCycle() {};

    private:
        Trace trace;
        NodeDb::NodeMultiOutput* result;
        NodeDb::NodeSingleInput* in;
        NodeDb::NodeMultiOutput* out;
    };
}

#endif
