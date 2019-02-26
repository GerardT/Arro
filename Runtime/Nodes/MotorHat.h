
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

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"


namespace Arro {
class MotorHAT {
public:
    enum dir {
        FORWARD = 1,
        BACKWARD = 2,
        BRAKE = 3,
        RELEASE = 4
    };
    enum step {
        SINGLE = 1,
        DOUBLE = 2,
        INTERLEAVE = 3,
        MICROSTEP = 4
    };

    MotorHAT(int address = 0x60, const char* filename = "/dev/i2c-1", float freq = 50.0);
    ~MotorHAT() {};
    void setPin(int pin, int value);
    static MotorHAT* getInstance();

private:
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

public:

    /**
     * Sets a single PWM channel
     *
     * \param channel PWM channel
     * \param on  On?
     * \param off Offset?
     */
    void setPWM(int channel, int on, int off);

    void setAllPWM(int on, int off);
    /**
     * Sets the PWM frequency.
     *
     * \param freq Frequency.
     */
    void setPWMFreq(double freq);


private:
    Arro::Trace m_trace;
    double m_prescaleval;
    char m_filename[40];
    int m_i2caddr;           // The I2C address
    int m_frequency;           // default @1600Hz PWM freq
    int m_file;
};
}

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
#define  __ALL_LED_ON_L        0xFA
#define  __ALL_LED_ON_H        0xFB
#define  __ALL_LED_OFF_L       0xFC
#define  __ALL_LED_OFF_H       0xFD

// Bits
#define __RESTART              0x80
#define __SLEEP                0x10
#define __ALLCALL              0x01
#define __INVRT                0x10
#define __OUTDRV               0x04


