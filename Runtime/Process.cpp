#include <iostream>
#include <vector>
#include <exception>

#include <Trace.h>
#include <ServerEngine.h>
#include <NodePid.h>
#include <NodePython.h>
#include <NodeLinear.h>
#include <NodeServo.h>
#include <NodePass.h>
#include <NodeTimer.h>
#include <NodeStartToken.h>
#include <NodeTransition.h>

#include <ConfigReader.h>
#include <NodeDb.h>
#include <Process.h>

/**
 * Process, like Pad, implement interface INode.
 * Process is for functional process nodes, Pad is for non-functional connection pads.
 * This constructor creates (depending on url) one associated IDevice derived instance.
 */
Process::Process(NodeDb& db, const string* url, string* instance, ConfigReader::StringMap params):
    INode(*instance),
	trace(string("Process"), true),
	nodeDb(db),
	doRunCycle(false) {

	trace.println("Creating instance of " + *url + " parameters: ");


    getPrimitive(url, instance, params);

    // TODO pass parameters here.

    std::map<std::string, std::string>::iterator iter;

    for (iter = params.begin(); iter != params.end(); ++iter) {
    	trace.println("    " + iter->first + " " + iter->second);

        KeyValuePair* kv = new KeyValuePair();
        kv->set_key(iter->first.c_str());
        kv->set_value(iter->second.c_str());
    	string s = kv->SerializeAsString();
    	MessageBuf msg(s);
    	free(kv);
        device->handleMessage(&msg, "config");
    }

    db.registerNode(this, *instance);

}

/**
 * Destructor, cleanup.
 */
Process::~Process() {
	delete device;
}

/**
 * Let implementation of Process run one execution cycle. Only run a
 * cycle if at least one input configured to trigger a cycle received a
 * message.
 */
void
Process::runCycle() {
	if(doRunCycle) {
		device->runCycle();
		doRunCycle = false;
	}
}

/**
 * Called from ConfigReader in order to register an input Pad as input. Basically
 * it installs a listener for this Pad that handles incoming messages.
 */
void
Process::registerInput(const string& interfaceName, bool enableRunCycle) {
	/* Almost anonymous class (if 'Anon' removed), but needed constructor */
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

/**
 * Called from ConfigReader in order to register an output Pad as output.
 */
void
Process::registerOutput(const string& interfaceName) {
	nodeDb.registerNodeOutput(this, interfaceName);
}

/**
 * Lookup an output by its name, which is concatenated: "procesname.name".
 */
NodeDb::NodeMultiOutput*
Process::getOutput(const string& name) {
	NodeDb::NodeMultiOutput* out = nodeDb.getOutput(getName() + "." + name);
	if(out) {
		return out;
	} else {
        trace.println("no such output registered: " + name);
        throw new null_pointer();
	}
}


/**
 * Instantiate a node of the given primitive type.
 * If
 * @param url
 * @param instance
 */
void
Process::getPrimitive(const string* url, string* instance, ConfigReader::StringMap& params) {
	if(url->find("python:") == 0) {
        trace.println("new NodePython(" + *instance + ")");
        try {
        	string className = url->substr(7);
        	device = new NodePython(this, className, params);
        } catch(out_of_range &) {

        }
	}
    if(*url == "pid") {
        trace.println("new NodePid(" + *instance + ")");
        device = new NodePid(this, *instance, params);
    }
    //else if(*url == "servo") {
    //   trace.println("new NodeServo(" + *instance + ")");
    //    new NodeServo(*instance, params);
    //}
    else if(*url == "linear") {
        trace.println("new NodeLinear(" + *instance + ")");
        device = new NodeLinear(this, *instance, params);
    }
    else if(*url == "tsReader") {
        trace.println("new NodeTsReader(" + *instance + ")");
        //device = new NodeTsReader(instance, params);
    }
    else if(*url == "tsSection") {
        trace.println("new NodeTsSection(" + *instance + ")");
        //device = new NodeTsSection(instance, params);
    }
    else if(*url == "timer") {
        trace.println("new NodeTimer(" + *instance + ")");
        device = new NodeTimer(this, *instance, params);
    }
    else if(*url == "pass") {
    }
    else if(*url == "start") {
        trace.println("new NodeStartToken(" + *instance + ")");
        device = new NodeStartToken(this, *instance, params);
    }
    else if(*url == "transition") {
        trace.println("new NodeTransition(" + *instance + ")");
        device = new NodeTransition(this, *instance, params);
    }
    else {
        trace.println("unknown node" + *instance );
        ServerEngine::console(string("unknown node ") + *url);
    }
}




