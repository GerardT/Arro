#include <linux/i2c-dev.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#if defined(_MSC_VER)
#include <windows.h>
#else
// hack, see http://stackoverflow.com/questions/12523122/what-is-glibcxx-use-nanosleep-all-about
#define _GLIBCXX_USE_NANOSLEEP 1
#include <chrono>
#include <thread>
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

class Servo {
    double prescaleval;
    bool debug;
    char filename[40];
    int addr;
    int file;

public:


    unsigned char i2c_readU8(unsigned char command) {
        /* Using I2C Read, equivalent of i2c_smbus_read_byte(file) */
        /* if (read(file, buf, 1) != 1) { */
        __s32 ret  = i2c_smbus_read_byte_data(file, command);
        printf("---- i2c_read returned 0x%x\n", command);
        if (ret == -1) {
          /* ERROR HANDLING: i2c transaction failed */
            printf("Failed i2c_write8 errno %d.\n", errno);
            exit(1);
        } else {
          /* buf[0] contains the read byte */
          return ret;
        }
    }
    void i2c_write8(unsigned char command, unsigned short value) {
        /* Using I2C Write, equivalent of 
           i2c_smbus_write_word_data(file, register, 0x6543) */
        /*
        unsigned char buf[10];
        buf[0] = register;
        buf[1] = value & 0x000000FF; // 0x43;
        value = value >> 8;
        buf[2] = value & 0x000000FF; // 0x65;
        if (write(file, buf, 3) !=3) {
        */
        __s32 ret = i2c_smbus_write_byte_data(file, command, value);
        printf("---- i2c_write8 Wrote 0x%x to 0x%x\n", value, command);
        if (ret == -1) {
          /* ERROR HANDLING: i2c transaction failed */
          printf("Failed i2c_write8 errno %d.\n", errno);
            exit(1);
        }
    }
    void setPWM(int channel, int on, int off) {
        //"Sets a single PWM channel"
        i2c_write8(__LED0_ON_L+4*channel, on & 0xFF);
        i2c_write8(__LED0_ON_H+4*channel, on >> 8);
        i2c_write8(__LED0_OFF_L+4*channel, off & 0xFF);
        i2c_write8(__LED0_OFF_H+4*channel, off >> 8);
    }
    void setPWMFreq(double freq) {
        //"Sets the PWM frequency"
        prescaleval = 25000000.0;   // 25MHz
        prescaleval /= 4096.0;      // 12-bit
        prescaleval /= float(freq);
        prescaleval -= 1.0;
        if (debug) {
          //print "Setting PWM frequency to %d Hz" % freq
          //print "Estimated pre-scale: %d" % prescaleval
        }
        int prescale = (int)(prescaleval + 0.5);
        if (debug) {
          //print "Final pre-scale: %d" % prescale
        }

        int oldmode = i2c_readU8(__MODE1);
        int newmode = (oldmode & 0x7F) | 0x10;            // sleep
        i2c_write8(__MODE1, newmode);       // go to sleep
        i2c_write8(__PRESCALE, prescale);
        i2c_write8(__MODE1, oldmode);
        //time.sleep(0.005)
        std::chrono::milliseconds timespan(5);
        std::this_thread::sleep_for(timespan);
        i2c_write8(__MODE1, oldmode | 0x80);
    }
    Servo() {
       debug = false;

        addr = 0x40; /* The I2C address */

        sprintf(filename,"/dev/i2c-1");
        if ((file = open(filename,O_RDWR)) < 0) {
            printf("Failed to open the bus errno %d.", errno);
            /* ERROR HANDLING; you can check errno to see what went wrong */
            exit(1);
        }

        if (ioctl(file,I2C_SLAVE,addr) < 0) {
            printf("Failed to acquire bus access and/or talk to slave.\n");
            /* ERROR HANDLING; you can check errno to see what went wrong */
            exit(1);
        }


        /* Reseting PCA9685 */
        __s32 ret = i2c_smbus_write_byte_data(file, __MODE1, 0x00);

        setPWMFreq(50.0);

    }
    void start(int ch, int val) {
        setPWM(ch, 0, val);
    }
};


#include "../NodeDb.h"

#include <NodeServo.h>

static Servo* pServo = NULL;

NodeServo::NodeServo(string name, ConfigReader::StringMap params):
    trace(string("NodeServo"), true) {

    if(!pServo) {
        pServo = new Servo();
    }

    actual_position = 0;
    previous_position = 0;
    actual_mode = "Idle";

    try {
        Ch = stod(params.at("Channel"));
    }
    catch (std::out_of_range) {
        trace.println("### param not found Ch ");
        exit(1);
    }

    //INode* n = NodeDb::getRef()->registerNode(name);

    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class AnonValue: public IPadListener {
        NodeServo* owner;
    public:
        AnonValue(NodeServo* n){owner = n; };

        void handleMessage(MessageBuf* msg, string padName) {
        	Value* msg = new Value();
        	msg->ParseFromString(m);

        	//Class<?> cl = msg.getClass();
            assert(msg->GetTypeName() == "tutorial.Value");

            owner->actual_position = ((Value*)msg)->value();
        }
    };

    //n->registerNodeInput("Value", "sub1", new AnonValue(this));

    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class AnonTick: public IPadListener {
        NodeServo* owner;
    public:
        AnonTick(NodeServo* n){owner = n; };

        void handleMessage(MessageBuf* msg, string padName) {
        	Tick* msg = new Tick();
        	msg->ParseFromString(m);

        	//Class<?> cl = msg.getClass();
            assert(msg->GetTypeName() == "tutorial.Tick");

            Tick* tick = (Tick*)msg;

            owner->doIt(tick->ms_elapsed());
        }
    };

    //n->registerNodeInput("Tick", "timer", new AnonTick(this));

    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class AnonMode: public IPadListener {
        NodeServo* owner;
    public:
        AnonMode(NodeServo* n){owner = n; };

        void handleMessage(MessageBuf* msg, string padName) {
        	Mode* msg = new Mode();
        	msg->ParseFromString(m);

        	//Class<?> cl = msg.getClass();
            assert(msg->GetTypeName() == "tutorial.Mode");

            owner->actual_mode = ((Mode*)msg)->mode();

            owner->trace.println("NodeServo changed mode to " + owner->actual_mode);
        }
    };
    //n->registerNodeInput("Mode", "mode", new AnonMode(this));

    //pServo->start(Ch, 200);
    //std::chrono::milliseconds timespan(1000);
    //std::this_thread::sleep_for(timespan);
    //pServo->start(Ch, 500);
}

void NodeServo::handleMessage(MessageBuf* msg, std::string padName) {
	Value* msg = new Value();
	msg->ParseFromString(m);

	//Class<?> cl = msg.getClass();
    assert(msg->GetTypeName() == "tutorial.Value");

    actual_position = ((Value*)msg)->value();
}

void NodeServo::runCycle() {

}

void NodeServo::doIt (int ms_elapsed) {
		
    trace.println(string("NodeServo input = ") + to_string((long double)actual_position));

    if(actual_mode == "Active"  
	&& actual_position != previous_position
	&& actual_position > 0 && actual_position < 400) {
        pServo->start(Ch, 200 + (actual_position * 2));
		previous_position = actual_position;
        ms_elapsed /= 1000;


    }

}

