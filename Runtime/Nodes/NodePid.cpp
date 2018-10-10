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

        virtual void finishConstruction();


        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

    private:
        Trace m_trace;
        INodeContext::ItRef m_actualValue;
        INodeContext::ItRef m_targetValue;
        INodeContext::ItRef m_tick;
        INodeContext::ItRef m_mode;
        OutputPad* m_statePad;
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

NodePid::NodePid(INodeContext* d, const string& /*name*/, StringMap& /* params */, TiXmlElement*):
    m_trace("NodePid", true),
    m_actualValue{nullptr},
    m_targetValue{nullptr},
    m_tick{nullptr},
    m_mode{nullptr},
    m_statePad{nullptr},
    m_elemBlock(d) {

    m_previous_error = 0;
    m_integral = 0;
    m_derivative = 0;
    m_setpoint = 100;
    m_actual_position = 0;
    m_actual_mode = "Idle";
    m_ms_elapsed = 0;

    m_Kp = stod(d->getParameter("Kp"));
    m_Ki = stod(d->getParameter("Ki"));
    m_Kd = stod(d->getParameter("Kd"));

}

void
NodePid::finishConstruction() {
    m_trace.println("finishConstruction");

    m_actualValue = m_elemBlock->begin(m_elemBlock->getInputPad("actualValue"), 0, INodeContext::DELTA);
    m_targetValue = m_elemBlock->begin(m_elemBlock->getInputPad("targetValue"), 0, INodeContext::DELTA);
    m_tick = m_elemBlock->begin(m_elemBlock->getInputPad("aTick"), 0, INodeContext::DELTA);
    m_mode = m_elemBlock->begin(m_elemBlock->getInputPad("mode"), 0, INodeContext::DELTA);

    m_statePad = m_elemBlock->getOutputPad("_step");

    Step* step = new Step();
    step->set_node(m_elemBlock->getName());
    step->set_name("_ready");
    m_elemBlock->setOutputData(m_statePad, step);

}

void
NodePid::runCycle() {

    m_trace.println(string("NodePid input = ") + to_string((long double)m_actual_position));


    MessageBuf m1;
    if(m_actualValue->getNext(m1)) {
        Value* actualValue = new Value();
        actualValue->ParseFromString(m1->c_str());

        m_actual_position = actualValue->value();
    }

    MessageBuf m2;
    if(m_targetValue->getNext(m2)) {
        Value* targetValue = new Value();
        targetValue->ParseFromString(m2->c_str());

        m_setpoint = targetValue->value();
    }

    MessageBuf m3;
    if(m_tick->getNext(m3)) {
        Tick* tick = new Tick();
        tick->ParseFromString(m3->c_str());

        m_ms_elapsed = tick->ms();
    }


    MessageBuf m4;
    if(m_mode->getNext(m4)) {
        Mode* mode = new Mode();
        mode->ParseFromString(m4->c_str());

        m_actual_mode = mode->mode();
    }

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

        m_elemBlock->setOutputData(m_elemBlock->getOutputPad("output"), value);
    }
}
