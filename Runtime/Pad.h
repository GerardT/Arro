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

        /**
         * More dummy implementations of virtual functions.
         */
        virtual void registerInput(const std::string& /*interfaceName*/, bool /*enableRunCycle*/) {};
        virtual void registerOutput(const std::string& /*interfaceName*/) {};
        virtual MessageBuf getInputData(const std::string& /*name*/) const { return MessageBuf{}; };
        virtual NodeSingleInput*  getInput(const std::string& /*name*/) const {return nullptr; };
        virtual NodeMultiOutput* getOutput(const std::string& /*name*/) const {return nullptr; };
        virtual void sendParameters(StringMap& /*params*/)  {};
        virtual void submitMessage(std::string /*pad*/, google::protobuf::MessageLite* /*msg*/) const {};

    private:
        Trace trace;
        NodeMultiOutput* m_result;
        NodeSingleInput* m_in;
        NodeMultiOutput* m_out;
    };
}

#endif
