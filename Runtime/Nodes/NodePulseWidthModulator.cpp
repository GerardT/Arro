
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


/**
 * The PCA9685 NodePulseWidthModulator generator (used on the Esc Hat) has four main layers of structure:
 *
 * - a 25MHz internal clock
 * - a prescaler that emits 1 tick for every N ticks of the 25MHz clock
 * - a 12-bit counter triggered by the prescaler
 * - a set of comparison registers for each output channel
 *
 * The 12-bit counter resets to zero every 4096 ticks of the prescaler, and the number
 * of times that happens per second is the NodePulseWidthModulator frequency. See setPWMFreq().
 *
 * The output goes high when the 12-bit clock equals the value in the first comparison
 * register, and goes low when the 12-bit clock equals the value in the second comparison register.
 * See setPWM()
 *
 */
namespace Arro {
    class NodePulseWidthModulator: public INodeDefinition {
    public:
        // Esc(int address = 0x60, const char* filename = "/dev/i2c-1");
        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodePulseWidthModulator(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);
        NodePulseWidthModulator(double freq, int address = 0x40, const char* filename = "/dev/i2c-1");

        virtual ~NodePulseWidthModulator() {};

        // Copy and assignment is not supported.
        NodePulseWidthModulator(const NodePulseWidthModulator&) = delete;
        NodePulseWidthModulator& operator=(const NodePulseWidthModulator& other) = delete;

        virtual void finishConstruction();

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

        void setPulse(int ch, int val);

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
         * Sets a single NodePulseWidthModulator channel
         *
         * \param channel NodePulseWidthModulator channel
         * \param on  On?
         * \param off Offset?
         */
        void setPWM(int channel, int on, int off);

        /**
         * Sets the NodePulseWidthModulator frequency.
         *
         * \param freq Frequency.
         */
        void setPWMFreq(double freq);

    private:
        Trace m_trace;
        INodeContext* m_elemBlock;
        double m_prescaleval;
        char m_filename[40];
        int m_addr;           // The I2C address
        int m_file;
        INodeContext::ItRef m_controlPad;
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


static RegisterMe<NodePulseWidthModulator> registerMe("PulseWidthModulator");

unsigned char
NodePulseWidthModulator::i2c_readU8(unsigned char command) {
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
NodePulseWidthModulator::i2c_write8(unsigned char command, unsigned short value) {
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
NodePulseWidthModulator::setPWM(int channel, int on, int off) {
    i2c_write8(__LED0_ON_L+4*channel, on & 0xFF);
    i2c_write8(__LED0_ON_H+4*channel, on >> 8);
    i2c_write8(__LED0_OFF_L+4*channel, off & 0xFF);
    i2c_write8(__LED0_OFF_H+4*channel, off >> 8);
}

void
NodePulseWidthModulator::setPWMFreq(double freq) {
    m_prescaleval = 25000000.0;   // 25MHz
    m_prescaleval /= 4096.0;      // 12-bit
    m_prescaleval /= float(freq);
    m_prescaleval -= 1.0;

    m_trace.println("Setting NodePulseWidthModulator frequency to %d Hz", freq);
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

void
NodePulseWidthModulator::setPulse(int ch, int val) {
    setPWM(ch, 0, val);
}

NodePulseWidthModulator::NodePulseWidthModulator(INodeContext* d, const string& /*name*/, StringMap& /* params */, TiXmlElement*):
    m_trace("NodePulseWidthModulator", true),
    m_elemBlock(d),
    m_controlPad{nullptr}
{

    int address = 0x40;
    std::string filename{"/dev/i2c-1"};
    
    int freq = stod(d->getParameter("Freq"));

// To stub out
#ifdef RPI
    if ((m_file = open(filename.c_str(),O_RDWR)) < 0) {
        m_trace.fatal("Failed to open the bus errno", errno);
    }

    if (ioctl(m_file,I2C_SLAVE,address) < 0) {
        m_trace.fatal("Failed to acquire bus access and/or talk to slave");
    }
#endif
    /* Reseting PCA9685 */
    i2c_smbus_write_byte_data(m_file, __MODE1, 0x00);

    setPWMFreq(freq);
}


void
NodePulseWidthModulator::finishConstruction() {
    m_trace.println("finishConstruction");

    m_controlPad = m_elemBlock->begin(m_elemBlock->getInputPad("control"), 0, INodeContext::DELTA);

}


void
NodePulseWidthModulator::runCycle() {

    MessageBuf m1;
    while(m_controlPad->getNext(m1)) {
        PwmControl* control = new PwmControl();
        control->ParseFromString(m1->c_str());

        int channel = control->channel();
        int pulse = control->pulsewidth();

        m_trace.println(string("NodePulseWidthModulator pulse = ") + to_string(pulse));

        setPulse(channel, pulse);
    }
}
