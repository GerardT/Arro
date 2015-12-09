#ifndef IDEVICE
#define IDEVICE

#include <string>

#include <google/protobuf/message.h>

typedef std::string MessageBuf;

using namespace google;
using namespace protobuf;

/**
 * Listener for updates to NodeSingleInput objects, invoked by
 * NodeSingleInput::handleMessage().
 */
class IPadListener {
public:
	virtual ~IPadListener() {};
	virtual void handleMessage(MessageBuf* msg, std::string padName) = 0;
};

/**
 * Interface to be implemented by all devices.
 * - handleMessage will pass data to the device.
 * - runCycle will trigger running one execution cycle.
 */
class IDevice {
public:
	virtual ~IDevice() {};
    virtual void handleMessage(MessageBuf* msg, std::string padName) = 0;
    virtual void runCycle() = 0;
};

#endif


