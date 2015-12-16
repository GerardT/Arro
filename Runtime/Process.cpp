#include <iostream>
#include <vector>
#include <exception>

#include "Trace.h"
#include "ServerEngine.h"
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


Process::Process(NodeDb& db, const string& url, string& instance, ConfigReader::StringMap params):
    AbstractNode(instance),
    trace("Process", true),
    nodeDb(db),
    doRunCycle(false) {

    trace.println("Creating instance of " + url + " parameters: ");


    getPrimitive(url, instance, params);

    std::map<std::string, std::string>::iterator iter;

    for (iter = params.begin(); iter != params.end(); ++iter) {
        trace.println("    " + iter->first + " " + iter->second);

        arro::KeyValuePair* kv = new arro::KeyValuePair();
        kv->set_key(iter->first.c_str());
        kv->set_value(iter->second.c_str());
        string s = kv->SerializeAsString();
        MessageBuf msg(s);
        free(kv);
        device->handleMessage(&msg, "config");
    }

    db.registerNode(this, instance);

}

Process::~Process() {
    delete device;
}

void
Process::runCycle() {
    if(doRunCycle) {
        device->runCycle();
        doRunCycle = false;
    }
}

void
Process::registerInput(const string& interfaceName, bool enableRunCycle) {
    // Almost anonymous class (if 'Anon' removed), but needed constructor.

    class Anon: public NodeDb::NodeSingleInput::IListener {
        Process* owner;
        string name;
        bool enableRunCycle;
    public:
        Anon(Process* n, string name, bool enableRunCycle){owner = n; this->name = name; this->enableRunCycle = enableRunCycle; };

        void handleMessage(MessageBuf* msg) {
            if(this->enableRunCycle) {
                owner->doRunCycle = true;
            }
            owner->device->handleMessage(msg, name);
        }
    };
    nodeDb.registerNodeInput(this, interfaceName, new Anon(this, interfaceName, enableRunCycle));
}

void
Process::registerOutput(const string& interfaceName) {
    nodeDb.registerNodeOutput(this, interfaceName);
}

NodeDb::NodeMultiOutput*
Process::getOutput(const string& name) {
    NodeDb::NodeMultiOutput* out = nodeDb.getOutput(getName() + "." + name);
    if(out) {
        return out;
    } else {
        trace.println("no such output registered: " + name);
        throw std::runtime_error("Unknown name");
    }
}


void
Process::getPrimitive(const string& url, string& instance, ConfigReader::StringMap& params) {
    if(url.find("python:") == 0) {
        trace.println("new NodePython(" + instance + ")");
        try {
            string className = url.substr(7);
            device = new NodePython(this, className, params);
        } catch(out_of_range &) {

        }
    }
    else if(url.find("native:") == 0) {
        trace.println("new NodePython(" + instance + ")");
        try {
            string className = url.substr(7);

            if(className == "pid") {
                trace.println("new NodePid(" + instance + ")");
                device = new NodePid(this, instance, params);
            }
            else if(className == "Servo") {
               trace.println("new NodeServo(" + instance + ")");
                new NodeServo(this, instance, params);
            }
//            else if(className == "Linear") {
//                trace.println("new NodeLinear(" + instance + ")");
//                device = new NodeLinear(this, instance, params);
//            }
//            else if(className == "TsReader") {
//                trace.println("new NodeTsReader(" + instance + ")");
//                //device = new NodeTsReader(instance, params);
//            }
//            else if(className == "TsSection") {
//                trace.println("new NodeTsSection(" + instance + ")");
//                //device = new NodeTsSection(instance, params);
//            }
            else if(className == "Timer") {
                trace.println("new NodeTimer(" + instance + ")");
                device = new NodeTimer(this, instance, params);
            }
            else if(className == "pass") {
            }
            else {
                trace.println("unknown node" + instance );
                ServerEngine::console(string("unknown node ") + className);
            }
        } catch(out_of_range &) {
            trace.println("native node not found");
        }
    }
}




