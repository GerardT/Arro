
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

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

    private:
        class Servo {
        public:
            Servo(int address = 0x40, const char* filename = "/dev/i2c-1");
            // Servo(int address = 0x60, const char* filename = "/dev/i2c-1");
            virtual ~Servo() {};
            void start(int ch, int val);

            /**
             * Read 1 byte from specified register.
             *
             * \param command Register to read from.
             */
            unsigned char i2c_readU8(unsigned char command);

            /**
             * Write 1 byte to register.
             *
             * \param command Register to write to.
             */
            void i2c_write8(unsigned char command, unsigned short value);

            /**
             * Sets a single PWM channel
             *
             * \param channel PWM channel
             * \param on  On?
             * \param off Offset?
             */
            void setPWM(int channel, int on, int off);

            /**
             * Sets the PWM frequency.
             *
             * \param freq Frequency.
             */
            void setPWMFreq(double freq);

        private:
            Trace m_trace;
            double m_prescaleval;
            char m_filename[40];
            int m_addr;           // The I2C address
            int m_file;
        };

        Trace m_trace;
        INodeContext* m_elemBlock;
        double m_previous_position;
        double m_actual_position;
        int m_ms_elapsed;
        std::string m_actual_mode;
        int m_Ch;
        StringMap m_params;

        INodeContext::ItRef m_sub1;
        INodeContext::ItRef m_timer;
        INodeContext::ItRef m_mode;

        static Servo* m_pServo;
    };
}

using namespace std;
using namespace Arro;
using namespace arro;

// To stub out
#ifndef RPI
    #define i2c_smbus_write_byte_data(file, mode, data) 1
    #define i2c_smbus_read_byte_data(file, command) 1
#endif


// Registers/etc.
#define  __SUBADR1             0x02
#define  __SUBADR2             0x03
#define  __SUBADR3             0x04
#define  __MODE1               0x00
#define  __PRESCALE            0xFE
#define  __LED0_ON_L           0x06
#define  __LED0_ON_H           0x07
#define  __LED0_OFF_L          0x08
#define  __LED0_OFF_H          0x09
#define  __ALLLED_ON_L         0xFA
#define  __ALLLED_ON_H         0xFB
#define  __ALLLED_OFF_L        0xFC
#define  __ALLLED_OFF_H        0xFD


static RegisterMe<NodeServo> registerMe("Servo");

unsigned char
NodeServo::Servo::i2c_readU8(unsigned char command) {
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
NodeServo::Servo::i2c_write8(unsigned char command, unsigned short value) {
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
NodeServo::Servo::setPWM(int channel, int on, int off) {
    i2c_write8(__LED0_ON_L+4*channel, on & 0xFF);
    i2c_write8(__LED0_ON_H+4*channel, on >> 8);
    i2c_write8(__LED0_OFF_L+4*channel, off & 0xFF);
    i2c_write8(__LED0_OFF_H+4*channel, off >> 8);
}

void
NodeServo::Servo::setPWMFreq(double freq) {
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

NodeServo::Servo::Servo(int address, const char* filename):
    m_trace("Servo", true),
    m_prescaleval(0),
    m_addr(address){

    if ((m_file = open(filename,O_RDWR)) < 0) {
        m_trace.fatal("Failed to open the bus errno", errno);
    }

    if (ioctl(m_file,I2C_SLAVE,m_addr) < 0) {
        m_trace.fatal("Failed to acquire bus access and/or talk to slave");
    }

    /* Reseting PCA9685 */
    i2c_smbus_write_byte_data(m_file, __MODE1, 0x00);

    setPWMFreq(50.0);

}

void
NodeServo::Servo::start(int ch, int val) {
    setPWM(ch, 0, val);
}

NodeServo::Servo* NodeServo::m_pServo = nullptr;


NodeServo::NodeServo(INodeContext* d, const string& /*name*/, StringMap& /* params */, TiXmlElement*):
    m_trace("NodeServo", true),
    m_elemBlock(d),
    m_previous_position(0),
    m_actual_position(0),
    m_ms_elapsed(0),
    m_actual_mode("Idle"),
    m_Ch(0),
    m_sub1{nullptr},
    m_timer{nullptr},
    m_mode{nullptr}
    {

    if(!m_pServo) {
        m_pServo = new Servo();
    }

    m_Ch = stod(d->getParameter("Channel"));
}


void
NodeServo::finishConstruction() {
    m_trace.println("finishConstruction");

    m_sub1 = m_elemBlock->begin(m_elemBlock->getInputPad("sub1"), 0, INodeContext::DELTA);
    m_timer = m_elemBlock->begin(m_elemBlock->getInputPad("timer"), 0, INodeContext::DELTA);
    m_mode = m_elemBlock->begin(m_elemBlock->getInputPad("mode"), 0, INodeContext::DELTA);

}


void
NodeServo::runCycle() {

    MessageBuf m1;
    if(m_sub1->getNext(m1)) {
        Value* position = new Value();
        position->ParseFromString(m1->c_str());

        m_actual_position = position->value();
    }

    MessageBuf m2;
    if(m_timer->getNext(m1)) {
        Tick* tick = new Tick();
        tick->ParseFromString(m2->c_str());

        m_ms_elapsed = tick->ms();
    }

    MessageBuf m3;
    if(m_mode->getNext(m3)) {
        Mode* mode = new Mode();
        mode->ParseFromString(m3->c_str());

        m_actual_mode = mode->mode();
    }

    m_trace.println(string("NodeServo input = ") + to_string((long double)m_actual_position));

    if(true /*actual_mode == "Active"*/
    && m_actual_position != m_previous_position
    && m_actual_position > 0 && m_actual_position < 400) {
        m_pServo->start(m_Ch, 200 + (m_actual_position * 2));
        m_previous_position = m_actual_position;
        m_ms_elapsed /= 1000;
    }
}
