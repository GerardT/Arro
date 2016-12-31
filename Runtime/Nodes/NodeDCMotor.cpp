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

#include "NodeDb.h"
#include "NodeDCMotor.h"

using namespace std;
using namespace Arro;
using namespace arro;

// To stub out
#ifndef RPI
    #define i2c_smbus_write_byte_data(file, mode, data) 1
    #define i2c_smbus_read_byte_data(file, command) 1
#endif

// Registers/etc.
#define  __MODE1               0x00
#define  __MODE2               0x01
#define  __SUBADR1             0x02
#define  __SUBADR2             0x03
#define  __SUBADR3             0x04
#define  __PRESCALE            0xFE
#define  __LED0_ON_L           0x06
#define  __LED0_ON_H           0x07
#define  __LED0_OFF_L          0x08
#define  __LED0_OFF_H          0x09
#define  __ALLLED_ON_L         0xFA
#define  __ALLLED_ON_H         0xFB
#define  __ALLLED_OFF_L        0xFC
#define  __ALLLED_OFF_H        0xFD

// Bits
#define __RESTART              0x80
#define __SLEEP                0x10
#define __ALLCALL              0x01
#define __INVRT                0x10
#define __OUTDRV               0x04

static RegisterMe<NodeDCMotor> registerMe("DCMotor");




unsigned char
NodeDCMotor::MotorHAT::i2c_readU8(unsigned char command) {
    // Using I2C Read, equivalent of i2c_smbus_read_byte(file)
    // if (read(file, buf, 1) != 1) {
    __s32 ret  = i2c_smbus_read_byte_data(m_file, command);
    m_trace.println("---- i2c_read returned ", ret);
    if (ret == -1) {
        m_trace.fatal("Failed i2c_read errno", errno);
    } else {
        // buf[0] contains the read byte
    }
    return ret;
}

void
NodeDCMotor::MotorHAT::i2c_write8(unsigned char command, unsigned short value) {
    // Using I2C Write, equivalent of
    // i2c_smbus_write_word_data(file, register, 0x6543)

    // unsigned char buf[10];
    // buf[0] = register;
    // buf[1] = value & 0x000000FF; // 0x43;
    // value = value >> 8;
    // buf[2] = value & 0x000000FF; // 0x65;
    // if (write(file, buf, 3) !=3) {

    __s32 ret = i2c_smbus_write_byte_data(m_file, command, value);
    m_trace.println("---- i2c_write8 Wrote", value);
    m_trace.println("to ", (int)command);
    if (ret == -1) {
        m_trace.fatal("Failed i2c_write8 errno", errno);
    }
}

void
NodeDCMotor::MotorHAT::setPWM(int channel, int on, int off) {
    i2c_write8(__LED0_ON_L+4*channel, on & 0xFF);
    i2c_write8(__LED0_ON_H+4*channel, on >> 8);
    i2c_write8(__LED0_OFF_L+4*channel, off & 0xFF);
    i2c_write8(__LED0_OFF_H+4*channel, off >> 8);
}

void
NodeDCMotor::MotorHAT::setPWMFreq(double freq) {
    m_prescaleval = 25000000.0;   // 25MHz
    m_prescaleval /= 4096.0;      // 12-bit
    m_prescaleval /= float(freq);
    m_prescaleval -= 1.0;

    m_trace.println("Setting PWM frequency to %d Hz", freq);
    m_trace.println("Estimated pre-scale: %d", m_prescaleval);

    int prescale = (int)(m_prescaleval + 0.5);
    m_trace.println("Final pre-scale", prescale);

    int oldmode = i2c_readU8(__MODE1);
    int newmode = (oldmode & 0x7F) | 0x10;  // sleep
    i2c_write8(__MODE1, newmode);           // go to sleep
    i2c_write8(__PRESCALE, prescale);
    i2c_write8(__MODE1, oldmode);
    //time.sleep(0.005)
    std::chrono::milliseconds timespan(5);
    std::this_thread::sleep_for(timespan);
    i2c_write8(__MODE1, oldmode | 0x80);
}

NodeDCMotor::MotorHAT::MotorHAT(int address, const char* filename, int freq):
    m_trace("DCMotor", true),
    m_prescaleval(0),
    m_i2caddr(address),
    m_frequency(freq) {
    m_trace.println("NodeDCMotor::MotorHAT::MotorHAT");

    if ((m_file = open(filename,O_RDWR)) < 0) {
        m_trace.fatal("Failed to open the bus errno", errno);
    }

    if (ioctl(m_file, I2C_SLAVE, m_i2caddr) < 0) {
        m_trace.fatal("Failed to acquire bus access and/or talk to slave");
    }

    /* Reseting PCA9685 */
    i2c_smbus_write_byte_data(m_file, __MODE2, __OUTDRV);
    i2c_smbus_write_byte_data(m_file, __MODE1, __ALLCALL);
    std::chrono::milliseconds timespan(5);
    std::this_thread::sleep_for(timespan);




    //    self.motors = [ Adafruit_DCMotor(self, m) for m in range(4) ]
    //    self.steppers = [ Adafruit_StepperMotor(self, 1), Adafruit_StepperMotor(self, 2) ]
    //    self._pwm =  PWM(addr, debug=False)


}

void
NodeDCMotor::MotorHAT::setPin(int pin, int value) {
    m_trace.println("NodeDCMotor::MotorHAT::setPin");

    if ((pin < 0) or (pin > 15)) {
        throw std::runtime_error("PWM pin must be between 0 and 15 inclusive");
    }
    if ((value != 0) and (value != 1)) {
        throw std::runtime_error("Pin value must be 0 or 1!");
    }
    if (value == 0) {
        setPWM(pin, 0, 4096);
    }
    if (value == 1) {
        setPWM(pin, 4096, 0);
    }
}


NodeDCMotor::MotorHAT* NodeDCMotor::m_pMotorHAT = nullptr;


NodeDCMotor::NodeDCMotor(Process* d, const string& /*name*/, ConfigReader::StringMap& params):
    m_trace("NodeDCMotor", true),
    m_device(d),
    m_Ch(0) {

    if(!m_pMotorHAT) {
        m_pMotorHAT = new MotorHAT();
    }

    try {
        m_Ch = stod(params.at("Motor"));
        m_Ch = 0;
    }
    catch (std::out_of_range) {
        m_trace.println("### param not found Ch ");
    }
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
NodeDCMotor::run(NodeDCMotor::MotorHAT::dir command) {
    m_trace.println("NodeDCMotor::run");
    if(m_pMotorHAT) {
        if (command == MotorHAT::FORWARD) {
            m_pMotorHAT->setPin(m_IN2pin, 0);
            m_pMotorHAT->setPin(m_IN1pin, 1);
        }
        if (command == MotorHAT::BACKWARD) {
            m_pMotorHAT->setPin(m_IN1pin, 0);
            m_pMotorHAT->setPin(m_IN2pin, 1);
        }
        if (command == MotorHAT::RELEASE) {
            m_pMotorHAT->setPin(m_IN1pin, 0);
            m_pMotorHAT->setPin(m_IN2pin, 0);
        }
    }
}

void
NodeDCMotor::setSpeed(int speed) {
    m_trace.println("NodeDCMotor::setSpeed");
    if (speed < 0) {
        speed = 0;
    }
    if (speed > 255) {
        speed = 255;
    }
    m_pMotorHAT->setPWM(m_Ch, 0, speed * 16);
}



void
NodeDCMotor::handleMessage(MessageBuf* m, const std::string& padName) {
    m_trace.println("NodeDCMotor::handleMessage");
    if(padName == "speed") {
        auto msg = new Value();
        msg->ParseFromString(m->c_str());

        assert(msg->GetTypeName() == "arro.Value");

        setSpeed(((Value*)msg)->value());
        run(MotorHAT::FORWARD);

    } else if(padName == "direction") {
        auto msg = new Value();
        msg->ParseFromString(m->c_str());

        int dir = ((Value*)msg)->value();
        if(dir >= 0 && dir <= 4) {
            run((MotorHAT::dir)dir);
        }

        assert(msg->GetTypeName() == "arro.Value");

    } else {
        m_trace.println(string("Message received from ") + padName);
    }
}

void
NodeDCMotor::runCycle() {
    m_trace.println("NodeDCMotor::runCycle");

}
