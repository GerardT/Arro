#ifndef ARRO_NODE_PID_H
#define ARRO_NODE_PID_H

#include "arro.pb.h"
#include "ConfigReader.h"
#include "NodeDb.h"
#include "Process.h"

namespace Arro {
    class NodePid: public IDevice {
    public:
        /**
         * Constructor
         *
         * \param device The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodePid(Process* device, const std::string& name, ConfigReader::StringMap& params);
        virtual ~NodePid() {};

        // Copy and assignment is not supported.
        NodePid(const NodePid&) = delete;
        NodePid& operator=(const NodePid& other) = delete;

        /**
         * Handle a message that is sent to this node.
         *
         * \param msg Message sent to this node.
         * \param padName name of pad that message was sent to.
         */
        void handleMessage(MessageBuf* msg, const std::string& padName);

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

    private:
        Trace trace;
        double previous_error;
        double integral;
        double derivative;
        double setpoint;
        double actual_position;
        std::string actual_mode;
        int ms_elapsed;
        double Kp;    // factor for "proportional" control
        double Ki;    // factor for "integral" control
        double Kd;    // factor for "derivative" control
        ConfigReader::StringMap params;
        Process* device;
    };
}

#endif

