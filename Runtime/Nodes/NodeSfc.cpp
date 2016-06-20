#include <iostream>
#include <vector>
#include <exception>

#include "Trace.h"
#include "ConfigReader.h"
#include "ServerEngine.h"

#include "arro.pb.h"
#include "NodeDb.h"
#include "NodeSfc.h"

using namespace std;
using namespace Arro;
using namespace arro;


class SfcStep {
public:
    SfcStep(const std::string& name, const NodeSfc& parent):
        m_parent(parent),
        m_name(name)
    {}

    void AddEntryAction(const std::string& actionName, const std::string& actionString) {
        entryActions.at(actionName) = actionString;
    }

    const std::string& getName() const {
        return m_name;
    }


private:
    const NodeSfc& m_parent;
    std::string m_name;
    std::map<std::string, std::string> entryActions;
};


NodeSfc::NodeSfc(Process* d, TiXmlElement* elt):
    trace("NodeSfc", true),
    device(d) {

    TiXmlElement* eltStep = elt->FirstChildElement("step");
    while(eltStep) {
        const string* nameAttr = eltStep->Attribute(string("name"));
        SfcStep* step = new SfcStep(*nameAttr, *this);


        TiXmlElement* eltAction = eltStep->FirstChildElement("entry-action");
        while(eltAction) {
            const string* actionNameAttr = eltStep->Attribute(string("name"));
            const string* actionActionAttr = eltStep->Attribute(string("state"));
            if(actionNameAttr && actionActionAttr) {
                step->AddEntryAction(*actionNameAttr, *actionActionAttr);
            }
            eltAction = eltAction->NextSiblingElement("entry-action");
        }
        eltStep = eltStep->NextSiblingElement("step");
    }

}

void
NodeSfc::handleMessage(MessageBuf* m, const std::string& padName) {
/*
    if(padName == "actualValue") {
        Value* msg = new Value();
        msg->ParseFromString(m->c_str());

        assert(msg->GetTypeName() == "arro.Value");
        actual_position = ((Value*)msg)->value();
    } else if(padName == "targetValue") {
        Value* msg = new Value();
        msg->ParseFromString(m->c_str());

        assert(msg->GetTypeName() == "arro.Value");
        setpoint = ((Value*)msg)->value();
    } else if(padName == "aTick") {
        Tick* msg = new Tick();
        msg->ParseFromString(m->c_str());

        trace.println(string(msg->GetTypeName()));
        assert(msg->GetTypeName() == "arro.Tick");
        Tick* tick = (Tick*)msg;
        ms_elapsed = tick->ms();

    } else if (padName == "mode") {
        Mode* msg = new Mode();
        msg->ParseFromString(m->c_str());

        assert(msg->GetTypeName() == "arro.Mode");
        actual_mode = ((Mode*)msg)->mode();
    } else {
        trace.println(string("Message received from ") + padName);
    }
    */
}

void
NodeSfc::runCycle() {

    //trace.println(string("NodeSfc input = ") + to_string((long double)actual_position));

    if(true /*actual_mode == "Active"*/) {
        //trace.println(string("NodeSfc output = ") + to_string((long double)output));

        Value* value = new Value();

       // value->set_value(output);

        //device->getOutput("output")->submitMessage(value);
    }
}
