#ifndef ARRO_PAD_H
#define ARRO_PAD_H

#include "Trace.h"
#include "NodeDb.h"
#include "RealNode.h"

namespace Arro
{

    /**
     * \brief Input or output pad. (better name: gateway?)
     *
     * An instance of this class will just forward messages to/from the associated node.
     */
    class Pad: public RealNode {
    public:

        /**
         * Constructor.
         *
         * \param nodeDb Node database.
         * \param datatype Datatype of messages that this pad should pass.
         * \param name Name of the pad.
         */
        Pad(NodeDb& nodeDb, const std::string& datatype, const std::string& name, unsigned int padId);
        virtual ~Pad() {};

        // Copy and assignment is not supported.
        Pad(const Pad&) = delete;
        Pad& operator=(const Pad& other) = delete;

        virtual const std::string& getName() const { return m_name; };

        /**
         * Dummy implementation of virtual function since a Pad cannot run.
         */
        void runCycle() {};

        /**
         * More dummy implementations of virtual functions.
         */
        virtual MessageBuf getInputData(InputPad* /*input*/) const { return MessageBuf{}; };
        virtual InputPad* getInputPad(const std::string& /*name*/) const {return nullptr; };
        virtual OutputPad* getOutputPad(const std::string& /*name*/) const {return nullptr; };
        virtual void setOutputData(OutputPad* /*output*/, google::protobuf::MessageLite* /*msg*/) const {};
        virtual std::string getParameter(const std::string&) { return ""; };

        unsigned int getInputPadId() { return m_in->m_outputPadId; };

//        virtual void fixInputs() {
//            unsigned int connectedPad = m_in->m_outputPadId;
//
//            // for the input pads connected to this output...
//            for(auto it = m_out->getInputPads().begin(); it != m_out->getInputPads().end(); ++it) {
//                (*it)->addOutput(connectedPad);
//            }
//        };

    private:
        Trace trace;
        InputPad* m_in;
        OutputPad* m_out;
        std::string m_name;
    };
}

#endif
