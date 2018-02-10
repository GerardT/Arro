
#include "Nodes/MotorHat.h"
#include <string>


// To stub out
#ifndef RPI
typedef signed char s8;
typedef unsigned char u8;

typedef signed short s16;
typedef unsigned short u16;

typedef signed int s32;
typedef unsigned int u32;

typedef signed long long s64;

s32 i2c_smbus_read_byte(int /*client*/){
    return 0;
}
s32 i2c_smbus_write_byte(int/*client*/, u8 /*value*/){
    return 0;
}
s32 i2c_smbus_read_byte_data(int/*client*/, u8 /*command*/){
    return 0;
}
s32 i2c_smbus_write_byte_data(int/*client*/, u8 /*command*/, u8 /*value*/){
    return 0;
}
s32 i2c_smbus_read_word_data(int/*client*/, u8 /*command*/){
    return 0;
}
s32 i2c_smbus_write_word_data(int/*client*/,  u8 /*command*/, u16 /*value*/){
    return 0;
}
#endif

namespace Arro {




MotorHAT* MotorHAT::getInstance() {
    static MotorHAT* pMotorHAT = nullptr;

    if(pMotorHAT == nullptr) {
        pMotorHAT = new MotorHAT{};
    }
    return pMotorHAT;
}

unsigned char
MotorHAT::i2c_readU8(unsigned char command) {
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
MotorHAT::i2c_write8(unsigned char command, unsigned short value) {
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
MotorHAT::setAllPWM(int on, int off) {
    i2c_write8(__ALL_LED_ON_L, on & 0xFF);
    i2c_write8(__ALL_LED_ON_H, on >> 8);
    i2c_write8(__ALL_LED_OFF_L, off & 0xFF);
    i2c_write8(__ALL_LED_OFF_H, off >> 8);
}


void
MotorHAT::setPWM(int channel, int on, int off) {
    i2c_write8(__LED0_ON_L+4*channel, on & 0xFF);
    i2c_write8(__LED0_ON_H+4*channel, on >> 8);
    i2c_write8(__LED0_OFF_L+4*channel, off & 0xFF);
    i2c_write8(__LED0_OFF_H+4*channel, off >> 8);
}

void
MotorHAT::setPWMFreq(double freq) {
    m_prescaleval = 25000000.0;   // 25MHz
    m_prescaleval /= 4096.0;      // 12-bit
    m_prescaleval /= float(freq);
    m_prescaleval -= 1.0;

    m_trace.println("Setting PWM frequency to %d Hz", freq);
    m_trace.println("Estimated pre-scale: %d", m_prescaleval);

    int prescale = (int)(m_prescaleval + 0.5);
    m_trace.println("Final pre-scale", prescale);

    int oldmode = i2c_readU8(__MODE1);
    int newmode = (oldmode & 0x7F) | __SLEEP;  // sleep
    i2c_write8(__MODE1, newmode);           // go to sleep
    i2c_write8(__PRESCALE, prescale);
    i2c_write8(__MODE1, oldmode);
    //time.sleep(0.005)
    std::chrono::milliseconds timespan(5);
    std::this_thread::sleep_for(timespan);
    i2c_write8(__MODE1, oldmode | 0x80);
}

MotorHAT::MotorHAT(int address, const char* filename, int freq):
    m_trace("DCMotor", false),
    m_prescaleval(0),
    m_i2caddr(address),
    m_frequency(freq) {
    m_trace.println("NodeDCMotor::MotorHAT::MotorHAT");

#ifdef RPI
    if ((m_file = open(filename,O_RDWR)) < 0) {
        m_trace.fatal("Failed to open the bus errno", errno);
    }

    if (ioctl(m_file, I2C_SLAVE, m_i2caddr) < 0) {
        m_trace.fatal("Failed to acquire bus access and/or talk to slave");
    }
#else
    m_trace.println(std::string("Not opening") + filename);
#endif

    /* Reseting PCA9685 */
    setAllPWM(0, 0);
    i2c_smbus_write_byte_data(m_file, __MODE2, __OUTDRV);
    i2c_smbus_write_byte_data(m_file, __MODE1, __ALLCALL);
    std::chrono::milliseconds timespan(5);
    std::this_thread::sleep_for(timespan);

    int oldmode = i2c_readU8(__MODE1);
    int newmode = oldmode & ~(__SLEEP);  // reset sleep
    i2c_write8(__MODE1, newmode);
    std::this_thread::sleep_for(timespan);



    //    self.motors = [ Adafruit_DCMotor(self, m) for m in range(4) ]
    //    self.steppers = [ Adafruit_StepperMotor(self, 1), Adafruit_StepperMotor(self, 2) ]
    //    self._pwm =  PWM(addr, debug=False)


    setPWMFreq(m_frequency);
}

void
MotorHAT::setPin(int pin, int value) {
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

}

