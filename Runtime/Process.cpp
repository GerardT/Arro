#include <iostream>
#include <vector>
#include <exception>

#include "Trace.h"
#include "ServerEngine.h"
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
//    m_elemBlock{nullptr},
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
    m_elemBlock{nullptr},
    m_doRunCycle{false},
    m_name{instance} {

    m_trace.println("Creating instance of " + url);

    m_params = params;

    getPrimitive(url, instance, params, elt);

    /*
    #include "arro.pb.h"
    std::map<std::string, std::string>::iterator iter;

    for (iter = params.begin(); iter != params.end(); ++iter) {
        m_trace.println("    parameter " + iter->first + " " + iter->second);

        auto kv = new arro::KeyValuePair();
        kv->set_key(iter->first.c_str());
        kv->set_value(iter->second.c_str());
        MessageBuf msg(new string(kv->SerializeAsString()));
        free(kv);
#if PARAM_AS_CONFIG
        //m_elemBlock->handleMessage(msg, "config");
#endif
    }
    */

    db.registerNode(this, instance);


}

Process::~Process() {
    if(m_elemBlock) delete m_elemBlock;
}

//void
//Process::sendParameters(StringMap& /*params*/) {
//#if PARAM_AS_CONFIG
//    m_params = params;
//
//    std::map<std::string, std::string>::iterator iter;
//
//    auto config = new arro::_Config();
//
//    auto map = config->value();
//    for (iter = params.begin(); iter != params.end(); ++iter) {
//        m_trace.println("    parameter " + iter->first + " " + iter->second);
//
//        (*config->mutable_value())[iter->first] = iter->second;
//    }
//
//    MessageBuf msg(new string(config->SerializeAsString()));
//
//    free(config);
//    // get _config input and send a message to it.
//    auto input = getInputPad("_config");
//    input->handleMessage(msg);
//#endif
//}


void
Process::runCycle() {
    if(m_doRunCycle) {
        m_elemBlock->runCycle();
        m_doRunCycle = false;
    }
}

std::string
Process::getParameter(const std::string& parname) {
    std::string parval = "";
    try {
        parval = m_params.at(parname);
    }
    catch (std::out_of_range&) {
        throw std::runtime_error("Elementary Block " + m_name + " needs parameter \"" + parname + "\"");
    }
    return parval;
}

void
Process::registerInput(const string& interfName, bool enableRunCycle) {
    // only need to capture enableRunCycle
    m_nodeDb.registerNodeInput(this, interfName,
            [=]() {
                if(enableRunCycle) {
                    m_doRunCycle = true;
                }
            });
}

void
Process::registerOutput(unsigned int padId, const string& interfaceName) {
    m_nodeDb.registerNodeOutput(this, padId, interfaceName);
}

const std::list<unsigned int>
Process::getConnections(InputPad* input) {
    return input->getConnections();
};

INodeContext::ItRef
Process::begin(InputPad* input, unsigned int connection, INodeContext::Mode mode) {
    return input->begin(connection, mode);
}

INodeContext::ItRef
Process::end(OutputPad* input) {
    return input->end();
}

InputPad*
Process::getInputPad(const string& name) const {
    auto in = m_nodeDb.getInputPad(getName() + ARRO_PAD_SEPARATOR + name);
    if(in) {
        return in;
    } else {
        m_trace.println("no such input registered: " + getName() + ARRO_PAD_SEPARATOR + name);
        SendToConsole("Program uses unknown input: " + getName() + ARRO_PAD_SEPARATOR + name);
        throw std::runtime_error("No such input registered: " + getName() + ARRO_PAD_SEPARATOR + name);
    }
}

OutputPad*
Process::getOutputPad(const string& name) const {
    auto out = m_nodeDb.getOutputPad(getName() + ARRO_PAD_SEPARATOR + name);
    if(out) {
        return out;
    } else {
        m_trace.println("no such output registered: " + getName() + ARRO_PAD_SEPARATOR + name);
        SendToConsole("Program uses unknown output: " + getName() + ARRO_PAD_SEPARATOR + name);
        throw std::runtime_error("No such output registered: " + getName() + ARRO_PAD_SEPARATOR + name);
    }
}

void
Process::setOutputData(OutputPad* output, google::protobuf::MessageLite* value) const {
    MessageBuf s(new std::string(value->SerializeAsString()));
    output->submitMessage(s);


}


void
Process::getPrimitive(const string& url, const string& instance, StringMap& params, TiXmlElement* elt) {
    m_elemBlock = nullptr;
    Factory factory;

    if(url.find("Python:") == 0) {
        m_trace.println("new NodePython(" + instance + ")");
        try {
            string className = url.substr(7);
            if(ServerEngine::getFactory("Python", factory)) {
                m_elemBlock = factory(this, className, params, nullptr);
            }
        } catch(out_of_range &) {
            throw std::runtime_error("Invalid URL for Python node " + url);
        }
    } else if(url.find("UiIn:") == 0) {
        try {
            string className = url.substr(5);

            if(ServerEngine::getFactory(className, factory)) {
                m_trace.println("new " + className + "(" + instance + ")");
                m_elemBlock = factory(this, "", params, elt);
            }
            else {
                m_trace.println("unknown node" + instance );
                SendToConsole("unknown node " + className);
            }
        } catch(out_of_range &) {
            m_trace.println("UiIn node not found");
        }
    } else if(url.find("UiOut:") == 0) {
        try {
            string className = url.substr(6);

            if(ServerEngine::getFactory(className, factory)) {
                m_trace.println("new " + className + "(" + instance + ")");
                m_elemBlock = factory(this, "", params, elt);
            }
            else {
                m_trace.println("unknown node" + instance );
                SendToConsole("unknown node " + className);
            }
        } catch(out_of_range &) {
            m_trace.println("UiOut node not found");
        }
    } else if(url.find("Sfc:") == 0) {
        StringMap params{};
        m_trace.println("new NodeSfc(" + instance + ")");
        try {
            if(ServerEngine::getFactory("_SFC", factory)) {
                m_elemBlock = factory(this, "", params, elt);
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
                m_elemBlock = factory(this, instance, params, nullptr);
            }
            else {
                m_trace.println("unknown node" + instance );
                SendToConsole("unknown node " + className);
            }
        } catch(out_of_range &) {
            m_trace.println("native node not found");
        }
    }
    if(m_elemBlock == nullptr) {
        m_trace.println("Node module not found " + url);
        throw std::runtime_error("Node module not found " + url);
    }
}

/**
 * TODO this is not the happiest function, it is for SFC only. Should be something more elegant.
 * @param sfc
 */
void
Process::registerChildSfc(const std::string& name, Process* sfc) {
    m_elemBlock->registerChildSfc(name, sfc->m_elemBlock);
}



