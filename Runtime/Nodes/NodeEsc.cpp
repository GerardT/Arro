
// hack, see http://stackoverflow.com/questions/12523122/what-is-glibcxx-use-nanosleep-all-about
#define _GLIBCXX_USE_NANOSLEEP 1
#include <chrono>
#include <thread>

#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"


/**
 * The PCA9685 PWM generator (used on the Esc Hat) has four main layers of structure:
 *
 * - a 25MHz internal clock
 * - a prescaler that emits 1 tick for every N ticks of the 25MHz clock
 * - a 12-bit counter triggered by the prescaler
 * - a set of comparison registers for each output channel
 *
 * The 12-bit counter resets to zero every 4096 ticks of the prescaler, and the number
 * of times that happens per second is the PWM frequency. See setPWMFreq().
 *
 * The output goes high when the 12-bit clock equals the value in the first comparison
 * register, and goes low when the 12-bit clock equals the value in the second comparison register.
 * See setPWM()
 *
 */
namespace Arro {
    class NodeEsc: public INodeDefinition {
    public:

        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeEsc(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);
        virtual ~NodeEsc() {};

        // Copy and assignment is not supported.
        NodeEsc(const NodeEsc&) = delete;
        NodeEsc& operator=(const NodeEsc& other) = delete;

        virtual void finishConstruction();

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

    private:

        Trace m_trace;
        INodeContext* m_elemBlock;
        int m_speed;
        int m_Ch;
        int m_maxPulse;
        int m_minPulse;
        int m_zeroPulse;

        INodeContext::ItRef m_speedPad;
        INodeContext::ItRef m_control;
        INodeContext::ItRef m_request;
        INodeContext::ItRef m_timer;

        bool m_started;
    };
}

using namespace std;
using namespace Arro;
using namespace arro;


static RegisterMe<NodeEsc> registerMe("Esc");


/*
def __init__(self, controller=None,
                   max_pulse=300,
                   min_pulse=490,
                   zero_pulse=350):

    self.controller = controller
    self.max_pulse = max_pulse
    self.min_pulse = min_pulse
    self.zero_pulse = zero_pulse

    #send zero pulse to calibrate ESC
    print("Init ESC")
    self.controller.set_pulse(self.max_pulse)
    time.sleep(0.01)
    self.controller.set_pulse(self.min_pulse)
    time.sleep(0.01)
    self.controller.set_pulse(self.zero_pulse)
    time.sleep(1)
*/

NodeEsc::NodeEsc(INodeContext* d, const string& /*name*/, StringMap& /* params */, TiXmlElement*):
    m_trace("NodeEsc", true),
    m_elemBlock(d),
    m_speed(0),
    m_Ch(0),
    m_speedPad{nullptr},
    m_started{false}
{

    m_Ch = stod(d->getParameter("Channel"));
    m_maxPulse = stod(d->getParameter("MaxPulse"));
    m_minPulse = stod(d->getParameter("MinPulse"));
    m_zeroPulse = stod(d->getParameter("ZeroPulse"));
#if 0
    // Init ESC
    m_trace.println("Init ESC");
    std::chrono::milliseconds timespan(10);
    m_pPWM->setPulse(m_Ch, m_maxPulse);
    std::this_thread::sleep_for(timespan);
    m_pPWM->setPulse(m_Ch, m_minPulse);
    std::this_thread::sleep_for(timespan);
    m_pPWM->setPulse(m_Ch, m_zeroPulse);
    std::chrono::milliseconds timespan1(1000);
    std::this_thread::sleep_for(timespan1);
#endif
}


void
NodeEsc::finishConstruction() {
    m_trace.println("finishConstruction");

    m_speedPad = m_elemBlock->begin(m_elemBlock->getInputPad("speed"), 0, INodeContext::DELTA);
    m_timer    = m_elemBlock->begin(m_elemBlock->getInputPad("timer"), 0, INodeContext::DELTA);

    m_control = m_elemBlock->end(m_elemBlock->getOutputPad("control"));
    m_request = m_elemBlock->end(m_elemBlock->getOutputPad("timerRequest"));


}


void
NodeEsc::runCycle() {

    PwmControl control;
    TimerRequest request;

    MessageBuf m1;
    try {
        if(!m_started) {
            m_started = true;

            m_trace.println("Init ESC");
            request.set_ms(10);
            request.set_tag(1);
            m_request->setRecord(request);
        }
        else if(m_timer->getNext(m1)) {
            Tick* timer = new Tick();
            timer->ParseFromString(m1->c_str());

<<<<<<< HEAD
            if(timer->tag() == 1) {
                // step 1
                control.set_channel(m_Ch);
                control.set_pulsewidth(m_maxPulse);
                m_control->setRecord(control);
                request.set_ms(10);
                request.set_tag(2);
                m_request->setRecord(request);
            }
            else if(timer->tag() == 2) {
                // step 1
                control.set_channel(m_Ch);
                control.set_pulsewidth(m_minPulse);
                m_control->setRecord(control);
                request.set_ms(10);
                request.set_tag(3);
                m_request->setRecord(request);
            }
            else if(timer->tag() == 3) {
                // step 1
                control.set_channel(m_Ch);
                control.set_pulsewidth(m_zeroPulse);
                m_control->setRecord(control);
                request.set_ms(10);
                request.set_tag(4);
                m_request->setRecord(request);
            }
            else if(timer->tag() == 4) {
                // step 1
                request.set_ms(1000);
                request.set_tag(5 /* done */);
                m_request->setRecord(request);
            }
        }
        else if(m_speedPad->getNext(m1)) {
=======
        // Limit speed
        if(m_speed > 0) {
            m_speed = std::min(m_speed, 30);
        }
        else
        {
            m_speed = std::max(m_speed, -30);
        }
        m_speed += m_zeroPulse;


        m_trace.println(string("NodeEsc speed = ") + to_string(m_speed));
>>>>>>> branch 'master' of https://github.com/GerardT/Arro.git

            Value* speed = new Value();
            speed->ParseFromString(m1->c_str());

            m_speed = speed->value();

            // Limit speed
            if(m_speed > 0) {
                m_speed = std::min(m_speed, 30);
            }
            else
            {
                m_speed = std::max(m_speed, -30);
            }
            m_speed += m_zeroPulse;


            m_trace.println(string("NodeEsc speed = ") + to_string(m_speed));

            control.set_channel(m_Ch);
            control.set_pulsewidth(m_speed);
            m_control->setRecord(control);
        }
    }
    catch(runtime_error&) {
        m_trace.println("Timer failed to update");
    }
}
