#ifndef ARRO_NODE_SERVO_H
#define ARRO_NODE_SERVO_H


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
    class NodeServo: public IDevice {
    public:

        /**
         * Constructor
         *
         * \param d The Process node instance.
         * \param name Name of this node.
         * \param params List of parameters passed to this node.
         */
        NodeServo(Arro::Process* d, const std::string& name, Arro::ConfigReader::StringMap& params, TiXmlElement*);
        virtual ~NodeServo() {};

        // Copy and assignment is not supported.
        NodeServo(const NodeServo&) = delete;
        NodeServo& operator=(const NodeServo& other) = delete;

        /**
         * Handle a message that is sent to this node.
         *
         * \param msg Message sent to this node.
         * \param padName name of pad that message was sent to.
         */
        void handleMessage(const MessageBuf& msg, const std::string& padName);

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
        Process* m_device;
        double m_previous_position;
        double m_actual_position;
        int m_ms_elapsed;
        std::string m_actual_mode;
        int m_Ch;
        ConfigReader::StringMap m_params;

        static Servo* m_pServo;
    };
}

#endif

