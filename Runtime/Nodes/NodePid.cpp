#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"

namespace Arro {
    class NodePid: public INodeDefinition {
    public:
        /**
         * Constructor
         *
         * \param elemBlock The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodePid(INodeContext* elemBlock, const std::string& name, StringMap& params, TiXmlElement*);
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
        void handleMessage(const MessageBuf& msg, const std::string& padName);

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
        StringMap m_params;
        INodeContext* m_elemBlock;
    };
}
#include "arro.pb.h"

using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodePid> registerMe("pid");

NodePid::NodePid(INodeContext* d, const string& /*name*/, StringMap& params, TiXmlElement*):
    m_trace("NodePid", true),
    m_elemBlock(d) {

    m_previous_error = 0;
    m_integral = 0;
    m_derivative = 0;
    m_setpoint = 100;
    m_actual_position = 0;
    m_actual_mode = "Idle";
    m_ms_elapsed = 0;

    try {
        m_Kp = stod(params.at("Kp"));
        m_Ki = stod(params.at("Ki"));
        m_Kd = stod(params.at("Kd"));
    }
    catch (std::out_of_range) {
        m_trace.println("### param not found Kp, Ki, Kd ");
    }
}

void
NodePid::handleMessage(const MessageBuf& m, const std::string& padName) {
    if(padName == "actualValue") {
        Value* msg = new Value();
        msg->ParseFromString(m->c_str());

        assert(msg->GetTypeName() == "arro.Value");
        m_actual_position = ((Value*)msg)->value();
    } else if(padName == "targetValue") {
        Value* msg = new Value();
        msg->ParseFromString(m->c_str());

        assert(msg->GetTypeName() == "arro.Value");
        m_setpoint = ((Value*)msg)->value();
    } else if(padName == "aTick") {
        Tick* msg = new Tick();
        msg->ParseFromString(m->c_str());

        m_trace.println(string(msg->GetTypeName()));
        assert(msg->GetTypeName() == "arro.Tick");
        Tick* tick = (Tick*)msg;
        m_ms_elapsed = tick->ms();

    } else if (padName == "mode") {
        Mode* msg = new Mode();
        msg->ParseFromString(m->c_str());

        assert(msg->GetTypeName() == "arro.Mode");
        m_actual_mode = ((Mode*)msg)->mode();
    } else {
        m_trace.println(string("Message received from ") + padName);
    }
}

void
NodePid::runCycle() {

    m_trace.println(string("NodePid input = ") + to_string((long double)m_actual_position));

    if(true /*actual_mode == "Active"*/) {
        m_ms_elapsed /= 100;


        // calculate the difference between
        // the desired value and the actual value
        double error = m_setpoint - m_actual_position;
        // track error over time, scaled to the timer interval
        m_integral = m_integral + (error * m_ms_elapsed);
        // determine the amount of change from the last time checked
        m_derivative = (error - m_previous_error) / m_ms_elapsed;
        // calculate how much to drive the output in order to get to the
        // desired setpoint.
        double output = (m_Kp * error) + (m_Ki * m_integral) + (m_Kd * m_derivative);
        // remember the error for the next time around.
        m_previous_error = error;

        m_trace.println(string("NodePid output = ") + to_string((long double)output));

        Value* value = new Value();

        value->set_value(output);

        m_elemBlock->setOutputData(m_elemBlock->getOutput("output"), value);
    }
}
