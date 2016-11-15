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
        Trace m_trace;
        double m_previous_error;
        double m_integral;
        double m_derivative;
        double m_setpoint;
        double m_actual_position;
        std::string m_actual_mode;
        int m_ms_elapsed;
        double m_Kp;    // factor for "proportional" control
        double m_Ki;    // factor for "integral" control
        double m_Kd;    // factor for "derivative" control
        ConfigReader::StringMap m_params;
        Process* m_device;
    };
}

#endif

