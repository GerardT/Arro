#include <iostream>
#include <vector>
#include <exception>

#include "Trace.h"
#include "ServerEngine.h"
#include "NodeSfc.h"
#include "NodeDCMotor.h"
#include "NodePid.h"
#include "NodePython.h"
#include "NodePass.h"
#include "NodeTimer.h"
#include "NodeServo.h"
#include "ConfigReader.h"
#include "NodeDb.h"
#include "Process.h"
#include "SocketClient.h"


using namespace Arro;
using namespace std;


//Process::Process(NodeDb& db, const string& instance):
//    RealNode{},
//    m_trace{"Process", true},
//    m_nodeDb{db},
//    m_device{nullptr},
//    m_doRunCycle{false},
//    m_name{instance},
//    m_webComponents(nullptr) {
//
//    m_trace.println("Creating sfc " + instance + "._sfc");
//
//    m_trace.println("---> Never used?? <--- ");
//
//    db.registerNode(this, instance + "._sfc");
//
//}

Process::Process(NodeDb& db, const string& url, const string& instance, StringMap params, TiXmlElement* elt):
    RealNode{},
    m_trace{"Process", true},
    m_nodeDb{db},
    m_device{nullptr},
    m_doRunCycle{false},
    m_name{instance} {

    m_trace.println("Creating instance of " + url);


    getPrimitive(url, instance, params, elt);

    std::map<std::string, std::string>::iterator iter;

    for (iter = params.begin(); iter != params.end(); ++iter) {
        m_trace.println("    parameter " + iter->first + " " + iter->second);

        auto kv = new arro::KeyValuePair();
        kv->set_key(iter->first.c_str());
        kv->set_value(iter->second.c_str());
        MessageBuf msg(new string(kv->SerializeAsString()));
        free(kv);
        m_device->handleMessage(msg, "config");
    }

    db.registerNode(this, instance);


}

Process::~Process() {
    if(m_device) delete m_device;
}

void
Process::sendParameters(StringMap& params) {
    std::map<std::string, std::string>::iterator iter;

    auto block = new arro::ParameterBlock();

    for (iter = params.begin(); iter != params.end(); ++iter) {
        m_trace.println("    parameter " + iter->first + " " + iter->second);

        auto kv = block->add_kv();

        kv->set_key(iter->first.c_str());
        kv->set_value(iter->second.c_str());
    }

    MessageBuf msg(new string(block->SerializeAsString()));
    free(block);

    // get _config input and send a message to it.
    auto input = getInput("_config");
    input->handleMessage(msg);
}


void
Process::runCycle() {
    if(m_doRunCycle) {
        m_device->runCycle();
        m_doRunCycle = false;
    }
}

void
Process::registerInput(const string& interfName, bool enableRunCycle) {
    // only need to capture enableRunCycle
    m_nodeDb.registerNodeInput(this, interfName, [=](const MessageBuf& msg, const std::string& interfaceName) {
        if(enableRunCycle) {
            m_doRunCycle = true;
        }
        m_device->handleMessage(msg, interfaceName);
    });
}

void
Process::registerOutput(const string& interfaceName) {
    m_nodeDb.registerNodeOutput(this, interfaceName);
}


MessageBuf
Process::getInputData(NodeSingleInput* input) const {
    return input->getData();
}


NodeSingleInput*
Process::getInput(const string& name) const {
    auto in = m_nodeDb.getInput(getName() + "." + name);
    if(in) {
        return in;
    } else {
        m_trace.println("no such input registered: " + getName() + "." + name);
        throw std::runtime_error("No such input registered: " + getName() + "." + name);
    }
}

NodeMultiOutput*
Process::getOutput(const string& name) const {
    auto out = m_nodeDb.getOutput(getName() + "." + name);
    if(out) {
        return out;
    } else {
        m_trace.println("no such output registered: " + getName() + "." + name);
        throw std::runtime_error("No such output registered: " + getName() + "." + name);
    }
}

void
Process::setOutputData(NodeMultiOutput* output, google::protobuf::MessageLite* value) const {
    output->submitMessage(value);
}


void
Process::getPrimitive(const string& url, const string& instance, StringMap& params, TiXmlElement* elt) {
    m_device = nullptr;
    Factory factory;

    if(url.find("Python:") == 0) {
        m_trace.println("new NodePython(" + instance + ")");
        try {
            string className = url.substr(7);
            if(ServerEngine::getFactory("Python", factory)) {
                m_device = factory(this, className, params, nullptr);
            }
        } catch(out_of_range &) {
            throw std::runtime_error("Invalid URL for Python node " + url);
        }
    } else if(url.find("UiIn:") == 0) {
        m_trace.println("new NodeUiIn(" + instance + ")");
        try {
            if(ServerEngine::getFactory("_UiUserInput", factory)) {
                m_device = factory(this, "", params, elt);
            }
        } catch(out_of_range &) {
            throw std::runtime_error("Invalid URL for SFC node " + url);
        }
    } else if(url.find("UiOut:") == 0) {
        m_trace.println("new NodeUiOut(" + instance + ")");
        try {
            if(ServerEngine::getFactory("_UiUserDisplay", factory)) {
                m_device = factory(this, "", params, elt);
            }
        } catch(out_of_range &) {
            throw std::runtime_error("Invalid URL for SFC node " + url);
        }
    } else if(url.find("Sfc:") == 0) {
        StringMap params{};
        m_trace.println("new NodeSfc(" + instance + ")");
        try {
            if(ServerEngine::getFactory("_SFC", factory)) {
                m_device = factory(this, "", params, elt);
            }
        } catch(out_of_range &) {
            throw std::runtime_error("Invalid URL for SFC node " + url);
        }
    }
    else if(url.find("Native:") == 0) {
        try {
            string className = url.substr(7);

            if(className == "pass") {
            }
            else if(ServerEngine::getFactory(className, factory)) {
                m_trace.println("new " + className + "(" + instance + ")");
                m_device = factory(this, instance, params, nullptr);
            }
            else {
                m_trace.println("unknown node" + instance );
                SendToConsole(string("unknown node ") + className);
            }
        } catch(out_of_range &) {
            m_trace.println("native node not found");
        }
    }
    if(m_device == nullptr) {
        m_trace.println("Node module not found " + url);
        throw std::runtime_error("Node module not found " + url);
    }
}

/**
 * TODO this is not the happiest function, it is for SFC only. Should be something more elegant.
 * @param sfc
 */
void
Process::registerSfc(const std::string& name, Process* sfc) {
    ((NodeSfc*)m_device)->registerSfc(name, (NodeSfc*)(sfc->m_device));
}



