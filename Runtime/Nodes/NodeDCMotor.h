#ifndef ARRO_NODE_DCMOTOR_H
#define ARRO_NODE_DCMOTOR_H


#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include "arro.pb.h"
#include "ConfigReader.h"
#include "Process.h"
#include "NodeDb.h"
#include "ServerEngine.h"

namespace Arro {
    class NodeDCMotor: public IDevice {
    public:

        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeDCMotor(Arro::Process* d, const std::string& name, Arro::ConfigReader::StringMap& params);
        virtual ~NodeDCMotor() {};

        // Copy and assignment is not supported.
        NodeDCMotor(const NodeDCMotor&) = delete;
        NodeDCMotor& operator=(const NodeDCMotor& other) = delete;

        /**
         * Handle a message that is sent to this node.
         *
         * \param msg Message sent to this node.
         * \param padName name of pad that message was sent to.
         */
        void handleMessage(MessageBuf* msg, const std::string& padName);

        /**
         * Make the node execute a processing cycle.
         */
        void runCycle();

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

            MotorHAT(int address = 0x60, const char* filename = "/dev/i2c-1", int freq = 1600);
            ~MotorHAT() {};
            void setPin(int pin, int value);

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
            int m_i2caddr;           // The I2C address
            int m_frequency;           // default @1600Hz PWM freq
            int m_file;
        };

        void run(MotorHAT::dir command);

        void setSpeed(int speed);

        void setPin(int pin, int value);



        Trace m_trace;
        Process* m_device;
        int m_PWMpin;
        int m_IN1pin;
        int m_IN2pin;
        int m_Ch;
        ConfigReader::StringMap m_params;

        static MotorHAT* m_pMotorHAT;
    };
}

#endif

