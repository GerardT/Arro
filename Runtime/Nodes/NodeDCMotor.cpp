
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

#include "Nodes/MotorHat.h"


namespace Arro {
class NodeDCMotor: public INodeDefinition {
    public:

        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeDCMotor(INodeContext* d, const std::string& name, Arro::StringMap& params, TiXmlElement*);
        virtual ~NodeDCMotor() {};

        // Copy and assignment is not supported.
        NodeDCMotor(const NodeDCMotor&) = delete;
        NodeDCMotor& operator=(const NodeDCMotor& other) = delete;

        virtual void finishConstruction();

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

        void run(MotorHAT::dir command);

        void setSpeed(int speed);

        void setPin(int pin, int value);



        Trace m_trace;
        INodeContext* m_elemBlock;
        int m_PWMpin;
        int m_IN1pin;
        int m_IN2pin;
        int m_Ch;
        StringMap m_params;

        bool m_running;

        INodeContext::ItRef m_speed;
        INodeContext::ItRef m_direction;
        INodeContext::ItRef m_action;

    };
}

using namespace std;
using namespace Arro;
using namespace arro;
static RegisterMe<NodeDCMotor> registerMe("DCMotor");



NodeDCMotor::NodeDCMotor(INodeContext* d, const string& /*name*/, Arro::StringMap&, TiXmlElement*):
    m_trace{"NodeDCMotor", true},
    m_elemBlock{d},
    m_Ch{0},
    m_running{true},
    m_speed{nullptr},
    m_direction{nullptr},
    m_action{nullptr}
{

    m_Ch = stod(d->getParameter("Motor"));
    m_Ch--; // It's 0 based, but named M1, M2, M3, M4 on PCB.

    int pwm;
    int in1;
    int in2;

    m_trace.println(string("Init motor ") + std::to_string(m_Ch));
    if (m_Ch == 0) {
        pwm = 8;
        in2 = 9;
        in1 = 10;
    }
    else if (m_Ch == 1) {
        pwm = 13;
        in2 = 12;
        in1 = 11;
    }
    else if (m_Ch == 2) {
        pwm = 2;
        in2 = 3;
        in1 = 4;
    }
    else if (m_Ch == 3) {
        pwm = 7;
        in2 = 6;
        in1 = 5;
    }
    else
    {
        throw std::runtime_error("MotorHAT Motor must be between 1 and 4 inclusive");
    }

    m_PWMpin = pwm;
    m_IN1pin = in1;
    m_IN2pin = in2;

}
void
NodeDCMotor::run(MotorHAT::dir command) {
    m_trace.println("NodeDCMotor::run");
    if(MotorHAT::getInstance()) {
        if (command == MotorHAT::FORWARD) {
            MotorHAT::getInstance()->setPin(m_IN2pin, 0);
            MotorHAT::getInstance()->setPin(m_IN1pin, 1);
        }
        if (command == MotorHAT::BACKWARD) {
            MotorHAT::getInstance()->setPin(m_IN1pin, 0);
            MotorHAT::getInstance()->setPin(m_IN2pin, 1);
        }
        if (command == MotorHAT::RELEASE) {
            MotorHAT::getInstance()->setPin(m_IN1pin, 0);
            MotorHAT::getInstance()->setPin(m_IN2pin, 0);
        }
    }
}

void
NodeDCMotor::setSpeed(int speed) {
    m_trace.println(std::string("NodeDCMotor::setSpeed ") + to_string(speed));
    if (speed < 0) {
        speed = 0;
    }
    if (speed > 255) {
        speed = 255;
    }
    MotorHAT::getInstance()->setPWM(m_PWMpin, 0, speed * 16);
}




void
NodeDCMotor::finishConstruction() {
    m_trace.println("finishConstruction");

    m_speed = m_elemBlock->begin(m_elemBlock->getInputPad("speed"), 0, INodeContext::DELTA);
    m_direction = m_elemBlock->begin(m_elemBlock->getInputPad("direction"), 0, INodeContext::DELTA);
    m_action = m_elemBlock->begin(m_elemBlock->getInputPad("_action"), 0, INodeContext::DELTA);

//    m_statePad = m_elemBlock->end(m_elemBlock->getOutputPad("_step"));
//
//    Step* step = new Step();
//    step->set_node(m_elemBlock->getName());
//    step->set_name("_ready");
//    m_elemBlock->setOutputData(m_statePad, step);

}

void
NodeDCMotor::runCycle() {
    m_trace.println("NodeDCMotor::runCycle");

    MessageBuf m1;
    if(m_speed->getNext(m1)) {
        Value* speed = new Value();
        speed->ParseFromString(m1->c_str());

        m_trace.println("Speed " + std::to_string(speed->value()));

        setSpeed(speed->value());
    }

    MessageBuf m2;
    if(m_direction->getNext(m2)) {
        Value* direction = new Value();
        direction->ParseFromString(m2->c_str());

        m_trace.println("Dir (1, 2, 4) " + std::to_string(direction->value()));

        int dir = direction->value();
        if(dir >= 0 && dir <= 4) {
            run((MotorHAT::dir)dir);
        }
    }

    MessageBuf m3;
    if(*m3 != "") {
        Action* action = new Action();
        action->ParseFromString(m3->c_str());

        string a = action->action();

        if(a == "_terminated") {
            m_trace.println("Received _terminated");
            m_running = false;
            // Switch off the motor
            run(MotorHAT::RELEASE);
        }
    }

}
