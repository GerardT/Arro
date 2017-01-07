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


using namespace Arro;
using namespace std;


Process::Process(NodeDb& db, const string& instance):
    AbstractNode{instance},
    m_trace{"Process", true},
    m_nodeDb{db},
    m_enableRunCycle{false},
    m_device{nullptr},
    m_doRunCycle{false} {

    m_trace.println("Creating sfc " + instance + "._sfc");

    m_trace.println("---> Never used?? <--- ");

    db.registerNode(this, instance + "._sfc");

}

Process::Process(NodeDb& db, const string& url, const string& instance, ConfigReader::StringMap params, TiXmlElement* elt):
    AbstractNode{instance},
    m_trace{"Process", true},
    m_nodeDb{db},
    m_enableRunCycle{false},
    m_device{nullptr},
    m_doRunCycle{false} {

    m_trace.println("Creating instance of " + url);


    getPrimitive(url, instance, params, elt);

    std::map<std::string, std::string>::iterator iter;

    for (iter = params.begin(); iter != params.end(); ++iter) {
        m_trace.println("    parameter " + iter->first + " " + iter->second);

        auto kv = new arro::KeyValuePair();
        kv->set_key(iter->first.c_str());
        kv->set_value(iter->second.c_str());
        string s = kv->SerializeAsString();
        MessageBuf msg(s);
        free(kv);
        m_device->handleMessage(&msg, "config");
    }

    db.registerNode(this, instance);

}

Process::~Process() {
    if(m_device) delete m_device;
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
    //m_interfaceName = interfName;
    m_enableRunCycle = enableRunCycle;
    m_nodeDb.registerNodeInput(this, interfName, [this](MessageBuf* msg, const std::string& interfaceName) {
        if(m_enableRunCycle) {
            m_doRunCycle = true;
        }
        m_device->handleMessage(msg, interfaceName);
    });
}

void
Process::registerOutput(const string& interfaceName) {
    m_nodeDb.registerNodeOutput(this, interfaceName);
}

NodeDb::NodeSingleInput*
Process::getInput(const string& name) const {
    auto in = m_nodeDb.getInput(getName() + "." + name);
    if(in) {
        return in;
    } else {
        m_trace.println("no such input registered: " + getName() + "." + name);
        throw std::runtime_error("No such input registered: " + getName() + "." + name);
    }
}

NodeDb::NodeMultiOutput*
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
Process::getPrimitive(const string& url, const string& instance, ConfigReader::StringMap& params, TiXmlElement* elt) {
    m_device = nullptr;
    ServerEngine::Factory factory;

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
    } else if(url.find("Sfc:") == 0) {
        ConfigReader::StringMap params{};
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

//            if(className == "pid") {
//                m_trace.println("new Pid(" + instance + ")");
//                m_device = new NodePid(this, instance, params, nullptr);
//            }
//            else if(className == "Servo") {
//               m_trace.println("new NodeServo(" + instance + ")");
//                m_device = new NodeServo(this, instance, params, nullptr);
//            }
//            else if(className == "DCMotor") {
//               m_trace.println("new NodeDCMotor(" + instance + ")");
//                m_device = new NodeDCMotor(this, instance, params, nullptr);
//            }
//            else if(className == "Linear") {
//                trace.println("new NodeLinear(" + instance + ")");
//                device = new NodeLinear(this, instance, params, nullptr);
//            }
//            else if(className == "TsReader") {
//                trace.println("new NodeTsReader(" + instance + ")");
//                //device = new NodeTsReader(instance, params, nullptr);
//            }
//            else if(className == "TsSection") {
//                trace.println("new NodeTsSection(" + instance + ")");
//                //device = new NodeTsSection(instance, params, nullptr);
//            }
//            else
            if(className == "pass") {
            }
            else if(ServerEngine::getFactory(className, factory)) {
                m_trace.println("new " + className + "(" + instance + ")");
                m_device = factory(this, instance, params, nullptr);
            }
            else {
                m_trace.println("unknown node" + instance );
                ServerEngine::console(string("unknown node ") + className);
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



