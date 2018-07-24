
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
class NodeStepperMotor: public INodeDefinition {
    public:

        //MICROSTEPS = 16
        // a sinusoidal curve NOT LINEAR!
        //MICROSTEP_CURVE = [0, 25, 50, 74, 98, 120, 141, 162, 180, 197, 212, 225, 236, 244, 250, 253, 255]


        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeStepperMotor(INodeContext* d, const std::string& name, Arro::StringMap& params, TiXmlElement*);
        virtual ~NodeStepperMotor() {};

        // Copy and assignment is not supported.
        NodeStepperMotor(const NodeStepperMotor&) = delete;
        NodeStepperMotor& operator=(const NodeStepperMotor& other) = delete;

        virtual void finishConstruction();


        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

        void run(MotorHAT::dir command);

        void setSpeed(int speed);

        void setPin(int pin, int value);

        int oneStep(int dir, int style);

        void step(int steps, int direction, int stepstyle);

        Trace m_trace;
        INodeContext* m_elemBlock;

        //
        int m_PWMA;
        int m_AIN2;
        int m_AIN1;
        int m_PWMB;
        int m_BIN2;
        int m_BIN1;

        int m_dir;
        int m_revsteps;
        float m_sec_per_step;
        int m_currentstep;

        int m_Ch;
        StringMap m_params;

        bool m_running;

        InputPad* m_speed;
        InputPad* m_direction;
        InputPad* m_steps;
        InputPad* m_action;
    };
}

using namespace std;
using namespace Arro;
using namespace arro;
static RegisterMe<NodeStepperMotor> registerMe("StepperMotor");

static const int MICROSTEPS = 8;
static const int MICROSTEP_CURVE[] = {0, 50, 98, 142, 180, 212, 236, 250, 255};



NodeStepperMotor::NodeStepperMotor(INodeContext* d, const string& /*name*/, Arro::StringMap& /* params */, TiXmlElement*):
    m_trace("NodeStepperMotor", true),
    m_elemBlock(d),
    m_Ch(0),
    m_running(true),
    m_speed{nullptr},
    m_direction{nullptr},
    m_steps{nullptr},
    m_action{nullptr}
{

    m_Ch = stod(d->getParameter("Motor"));
    m_Ch--; // It's 0 based, but named M1, M2 on PCB.

    m_trace.println(string("Init motor ") + std::to_string(m_Ch));

    //m_MC = controller
    m_revsteps = 200;  // For now, maybe parameter?
    m_sec_per_step = 0.1;
    m_currentstep = 0;
    m_dir = MotorHAT::BRAKE;

    if (m_Ch == 0) {
        m_PWMA = 8;
        m_AIN2 = 9;
        m_AIN1 = 10;
        m_PWMB = 13;
        m_BIN2 = 12;
        m_BIN1 = 11;
    }
    else if(m_Ch == 1) {
        m_PWMA = 2;
        m_AIN2 = 3;
        m_AIN1 = 4;
        m_PWMB = 7;
        m_BIN2 = 6;
        m_BIN1 = 5;
    }
    else {
        throw std::runtime_error("MotorHAT Stepper Motor must be between 1 and 2 inclusive");
    }

}


int NodeStepperMotor::oneStep(int dir, int style) {
    int pwm_a = 255;
    int pwm_b = 255;

    // first determine what sort of stepping procedure we're up to
    if (style == MotorHAT::SINGLE) {
        if ((m_currentstep/(MICROSTEPS/2)) % 2) {
            // we're at an odd step, weird
            if (dir == MotorHAT::FORWARD) {
                m_currentstep += MICROSTEPS/2;
            }
            else {
                m_currentstep -= MICROSTEPS/2;
            }
        }
        else {
            // go to next even step
            if (dir == MotorHAT::FORWARD) {
                m_currentstep += MICROSTEPS;
            }
            else {
                m_currentstep -= MICROSTEPS;
            }
        }
    }

    if (style == MotorHAT::DOUBLE) {
        if (not (m_currentstep/(MICROSTEPS/2) % 2)) {
            // we're at an even step, weird
            if (dir == MotorHAT::FORWARD) {
                m_currentstep += MICROSTEPS/2;
            }
            else {
                m_currentstep -= MICROSTEPS/2;
            }
        }
        else {
            // go to next odd step
            if (dir == MotorHAT::FORWARD) {
                m_currentstep += MICROSTEPS;
            }
            else {
                m_currentstep -= MICROSTEPS;
            }
        }
    }
    if (style == MotorHAT::INTERLEAVE) {
        if (dir == MotorHAT::FORWARD) {
            m_currentstep += MICROSTEPS/2;
        }
        else {
            m_currentstep -= MICROSTEPS/2;
        }
    }

    if (style == MotorHAT::MICROSTEP) {
        if (dir == MotorHAT::FORWARD) {
            m_currentstep += 1;
        }
        else {
            m_currentstep -= 1;

            // go to next 'step' and wrap around
            m_currentstep += MICROSTEPS * 4;
            m_currentstep %= MICROSTEPS * 4;
        }

        pwm_a = pwm_b = 0;
        if ((m_currentstep >= 0) && (m_currentstep < MICROSTEPS)) {
            pwm_a = MICROSTEP_CURVE[MICROSTEPS - m_currentstep];
            pwm_b = MICROSTEP_CURVE[m_currentstep];
        }
        else if ((m_currentstep >= MICROSTEPS) && (m_currentstep < MICROSTEPS * 2)) {
            pwm_a = MICROSTEP_CURVE[m_currentstep - MICROSTEPS];
            pwm_b = MICROSTEP_CURVE[MICROSTEPS*2 - m_currentstep];
        }
        else if ((m_currentstep >= MICROSTEPS*2) && (m_currentstep < MICROSTEPS*3)) {
            pwm_a = MICROSTEP_CURVE[MICROSTEPS*3 - m_currentstep];
            pwm_b = MICROSTEP_CURVE[m_currentstep - MICROSTEPS*2];
        }
        else if ((m_currentstep >= MICROSTEPS*3) && (m_currentstep < MICROSTEPS*4)) {
            pwm_a = MICROSTEP_CURVE[m_currentstep - MICROSTEPS*3];
            pwm_b = MICROSTEP_CURVE[MICROSTEPS*4 - m_currentstep];
        }
    }


    // go to next 'step' and wrap around
    m_currentstep += MICROSTEPS * 4;
    m_currentstep %= MICROSTEPS * 4;

    // only really used for microstepping, otherwise always on!
    MotorHAT::getInstance()->setPWM(m_PWMA, 0, pwm_a*16);
    MotorHAT::getInstance()->setPWM(m_PWMB, 0, pwm_b*16);

    // set up coil energizing!
    vector<int> coils{0, 0, 0, 0};

    if (style == MotorHAT::MICROSTEP) {
        if ((m_currentstep >= 0) && (m_currentstep < MICROSTEPS)) {
            coils = {1, 1, 0, 0};
        }
        else if ((m_currentstep >= MICROSTEPS) && (m_currentstep < MICROSTEPS*2)) {
            coils = {0, 1, 1, 0};
        }
        else if ((m_currentstep >= MICROSTEPS*2) && (m_currentstep < MICROSTEPS*3)) {
            coils = {0, 0, 1, 1};
        }
        else if ((m_currentstep >= MICROSTEPS*3) && (m_currentstep < MICROSTEPS*4)) {
            coils = {1, 0, 0, 1};
        }
    }
    else {
        int step2coils[][4] = {
            {1, 0, 0, 0},
            {1, 1, 0, 0},
            {0, 1, 0, 0},
            {0, 1, 1, 0},
            {0, 0, 1, 0},
            {0, 0, 1, 1},
            {0, 0, 0, 1},
            {1, 0, 0, 1} };
        for(int i = 0; i < 4; i++) {
            coils[i] = step2coils[m_currentstep/(MICROSTEPS/2)][i];
        }
    }

    //print "coils state = " + str(coils)
    MotorHAT::getInstance()->setPin(m_AIN2, coils[0]);
    MotorHAT::getInstance()->setPin(m_BIN1, coils[1]);
    MotorHAT::getInstance()->setPin(m_AIN1, coils[2]);
    MotorHAT::getInstance()->setPin(m_BIN2, coils[3]);

    return m_currentstep;
}

void NodeStepperMotor::step(int steps, int direction, int stepstyle) {
    int s_per_s = m_sec_per_step;
    int lateststep = 0;

    if (stepstyle == MotorHAT::INTERLEAVE) {
        s_per_s = s_per_s / 2.0;
    }
    if (stepstyle == MotorHAT::MICROSTEP) {
        s_per_s /= MICROSTEPS;
        steps *= MICROSTEPS;
    }

    m_trace.println("seconds per step " + std::to_string(s_per_s) + " steps " + std::to_string(steps) + " direction " + std::to_string(direction) + " stepstyle " + std::to_string(stepstyle));

    for (int s = 0; s < steps; s++) {
        lateststep = oneStep(direction, stepstyle);
        //time.sleep(s_per_s);
        std::chrono::milliseconds timespan(s_per_s);
        std::this_thread::sleep_for(timespan);
    }

    if (stepstyle == MotorHAT::MICROSTEP) {
        // this is an edge case, if we are in between full steps, lets just keep going
        // so we end on a full step
        while ((lateststep != 0) and (lateststep != MICROSTEPS)) {
            lateststep = oneStep(direction, stepstyle);
            //time.sleep(s_per_s);
            std::chrono::milliseconds timespan(s_per_s);
            std::this_thread::sleep_for(timespan);
        }
    }
}


void
NodeStepperMotor::setSpeed(int rpm) {
    m_trace.println(std::string("NodeStepperMotor::setSpeed ") + to_string(rpm));
    m_sec_per_step = 60.0 / (m_revsteps * rpm);
    m_trace.println("m_sec_per_step " + std::to_string(m_sec_per_step));
}


void
NodeStepperMotor::finishConstruction() {
    m_trace.println("finishConstruction");

    m_speed = m_elemBlock->getInputPad("speed");
    m_direction = m_elemBlock->getInputPad("direction");
    m_steps = m_elemBlock->getInputPad("steps");
    m_action = m_elemBlock->getInputPad("_action");

}

void
NodeStepperMotor::runCycle() {
    m_trace.println("NodeStepperMotor::runCycle");

    Value* speed = new Value();
    MessageBuf m1 = m_elemBlock->getInputData(m_speed);
    if(*m1 != "") {
        speed->ParseFromString(m1->c_str());

        m_trace.println("Speed " + std::to_string(speed->value()));

        setSpeed(speed->value());
    }

    Value* direction = new Value();
    MessageBuf m2 = m_elemBlock->getInputData(m_direction);
    if(*m2 != "") {
        direction->ParseFromString(m2->c_str());

        m_trace.println("Dir (1, 2, 4) " + std::to_string(direction->value()));

        int dir = direction->value();
        if(dir >= 0 && dir <= 4) {
            m_dir = dir;
        }
    }

    Value* steps = new Value();
    MessageBuf m3 = m_elemBlock->getInputData(m_steps);
    if(*m3 != "") {
        steps->ParseFromString(m3->c_str());

        m_trace.println("Steps " + std::to_string(steps->value()));

        if(steps->value() >= 0) {
            step(steps->value(), m_dir, MotorHAT::SINGLE);
        }
    }

    Action* action = new Action();
    MessageBuf m4 = m_elemBlock->getInputData(m_action);
    if(*m4 != "") {
        action->ParseFromString(m4->c_str());

        string a = action->action();

        if(a == "_terminated") {
            m_trace.println("Received _terminated");
            m_running = false;
            // Switch off the motor
            // FIXME: reset?
        }
    }

}
