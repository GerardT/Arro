
// hack, see http://stackoverflow.com/questions/12523122/what-is-glibcxx-use-nanosleep-all-about
#define _GLIBCXX_USE_NANOSLEEP 1
#include <chrono>
#include <thread>
#include <linux/i2c-dev.h>
//#include <linux/i2c.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"

namespace Arro {
    class NodeServo: public INodeDefinition {
    public:

        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeServo(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);
        virtual ~NodeServo() {};

        // Copy and assignment is not supported.
        NodeServo(const NodeServo&) = delete;
        NodeServo& operator=(const NodeServo& other) = delete;

        virtual void finishConstruction();

        void setPosition(int ch, int val);
        void runCycle();

    private:

        Trace m_trace;
        INodeContext* m_elemBlock;
        double m_previous_position;
        double m_actual_position;
        int m_ms_elapsed;
        std::string m_actual_mode;
        int m_Ch;
        StringMap m_params;

        INodeContext::ItRef m_position;
        INodeContext::ItRef m_control;
    };
}

using namespace std;
using namespace Arro;
using namespace arro;


static RegisterMe<NodeServo> registerMe("Servo");


void
NodeServo::setPosition(int ch, int val) {
    PwmControl control;
    control.set_channel(ch);
    control.set_pulsewidth(val);
    m_control->setRecord(control);
    //setPWM(ch, 0, val);
}



NodeServo::NodeServo(INodeContext* d, const string& /*name*/, StringMap& /* params */, TiXmlElement*):
    m_trace("NodeServo", true),
    m_elemBlock(d),
    m_previous_position(0),
    m_actual_position(0),
    m_ms_elapsed(0),
    m_actual_mode("Idle"),
    m_Ch(0),
    m_position{nullptr} {

    //setPWMFreq(50.0);

    m_Ch = stod(d->getParameter("Channel"));
}


void
NodeServo::finishConstruction() {
    m_trace.println("finishConstruction");

    m_position = m_elemBlock->begin(m_elemBlock->getInputPad("position"), 0, INodeContext::DELTA);

    m_control = m_elemBlock->end(m_elemBlock->getOutputPad("control"));
}


void
NodeServo::runCycle() {

    MessageBuf m1;
    if(m_position->getNext(m1)) {
        Value* position = new Value();
        position->ParseFromString(m1->c_str());

        m_actual_position = position->value();
    }


    m_trace.println(string("NodeServo input = ") + to_string((long double)m_actual_position));

    if(true /*actual_mode == "Active"*/
    && m_actual_position != m_previous_position
    && m_actual_position > 0 && m_actual_position < 400) {
        setPosition(m_Ch, 200 + (m_actual_position * 2));
        m_previous_position = m_actual_position;
        m_ms_elapsed /= 1000;
    }
}
