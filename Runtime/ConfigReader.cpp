
#include <iostream>
#include <vector>
#include <exception>

#include "Trace.h"
#include "ConfigReader.h"
#include "ServerEngine.h"
#include "NodeDb.h"
#include "Pad.h"
#include "Process.h"


using namespace std;
using namespace Arro;

ConfigReader::ConfigReader(const string& filename, NodeDb& db):

     trace("ConfigReader", true),
     nodeDb(db)
{
    StringMap* params = new StringMap();

    TiXmlDocument doc(filename);
    if (!doc.LoadFile()) {
        trace.fatal("Cannot read file " + filename);
        return;
    }

    TiXmlElement* node = doc.FirstChildElement("diagrams");

    if(!node) {
        trace.fatal("node 'diagrams' missing");
    }

    // Collect all nodedefinition blocks and store them in map 'definitions'.
    TiXmlElement* elt = node->FirstChildElement("nodedefinition");
    while (elt) {
        storeDefinition(elt);
        elt = elt->NextSiblingElement("nodedefinition");
    }
    trace.println("");

    // Recursively process all nodedefinitions, starting with "Main"
    makeNodeInstance("Main", "main", "", *params);
}

ConfigReader::~ConfigReader() {
    // empty
}

void
ConfigReader::storeDefinition(TiXmlElement* node) {
    const char* attr = node->Attribute("type");

    if(attr != nullptr){
        definitions[attr] = new Definition(node);
        trace.println(string("Added definition ") + attr);
    }
    else {
        trace.fatal("No type found? ");
    }
}



void
ConfigReader::getParamsAndSubstitute(TiXmlElement* node, StringMap& import_params, StringMap& params) {
    TiXmlElement* elt;
    elt = node->FirstChildElement("param");
    while(elt) {
        const string* keyAttr = elt->Attribute(string("key"));
        const string* substAttr = elt->Attribute(string("subst"));
        const string* valueAttr = elt->Attribute(string("value"));
        trace.println("found parameter  : key " + *keyAttr + ", subst " + *substAttr + ", value " + *valueAttr);
        if(keyAttr) {
           params[*keyAttr] = *valueAttr;
           if(substAttr && *substAttr != "") {
                // use value from import_params if any
                if(import_params.count(*substAttr) == 1) {
                    params[*keyAttr] = import_params[*substAttr];
                    //trace.println("param " + *keyAttr + " value " + *valueAttr + " = " + import_params[*keyAttr]);
                }
            }
        }
        else {
            trace.fatal("faulty parameter block");
        }
        trace.println("updated parameter: key " + *keyAttr + ", value " + params[*keyAttr]);
        elt = elt->NextSiblingElement("param");
    }
}


void
ConfigReader::makeNodeInstance(const string& typeName, const string& instanceName, const string& instancePrefix, StringMap& import_params) {
    Definition* def = definitions[typeName];
    Process* processNode = nullptr;

    if(def == nullptr)
    {
        trace.fatal("Element not found: " + typeName);
        return;
    }
    string instance = instancePrefix + ARRO_NAME_SEPARATOR + instanceName;

    trace.println("makeNodeInstance for " + typeName + ", " + instance);
    TiXmlElement* elt;


    // Read devices (the leaves in the tree)
    elt = def->node->FirstChildElement("device");
    while(elt) {
        const string* typeURL = elt->Attribute(string("url"));

        if(typeURL != nullptr) {
            // collect all parameters
            StringMap* params = new StringMap();
            getParamsAndSubstitute(elt, import_params, *params);

            // create Process object; inputs & outputs to be added later.
            processNode = new Process(nodeDb, *typeURL, instance, *params);

            delete params;
        }
        elt = elt->NextSiblingElement("device");
    }

    // Read nodes (the branches in the tree)
    elt = def->node->FirstChildElement("node");
    while(elt) {
        const string* typeAttr = elt->Attribute(string("type"));
        const string* idAttr = elt->Attribute(string("name"));
        // trace.println("New node def ");
        if(typeAttr != nullptr && idAttr != nullptr) {
            // collect all parameters
            StringMap* params = new StringMap();
            getParamsAndSubstitute(elt, import_params, *params);

            makeNodeInstance(*typeAttr,  *idAttr,  instance, *params);

            delete params;
        }
        elt = elt->NextSiblingElement("node");
    }

    // Read pads (other leaves in the tree)
    elt = def->node->FirstChildElement("pad");
    while(elt) {
        const string* datatypeAttr = elt->Attribute(string("type"));
        const string* idAttr = elt->Attribute(string("name"));
        const string* inputAttr = elt->Attribute(string("input"));
        const string* runAttr = elt->Attribute(string("run"));

        if(datatypeAttr != nullptr && idAttr != nullptr) {
            string inst = instance + ARRO_NAME_SEPARATOR + *idAttr;
            if(processNode) {
                if(*inputAttr == "true") {
                    if(runAttr != nullptr && *runAttr == "true") {
                        processNode->registerInput(*idAttr, true);
                    } else {
                        processNode->registerInput(*idAttr, false);
                    }
                } else {
                    processNode->registerOutput(*idAttr);
                }
            } else {
                // create Pad object with input and output.
                trace.println("new Pad(" + *datatypeAttr + ", " + inst + ")");
                new Pad(nodeDb, *datatypeAttr, inst);
            }
        }
        elt = elt->NextSiblingElement("pad");
    }

    // Read connections
    elt = def->node->FirstChildElement("connection");
    while(elt) {
        const string* fromAttr = elt->Attribute(string("source"));
        const string* toAttr = elt->Attribute(string("target"));
        if(fromAttr != nullptr && toAttr != nullptr) {
            string from = instance + ARRO_NAME_SEPARATOR + *fromAttr;

            string to = instance + ARRO_NAME_SEPARATOR + *toAttr;

            trace.println("nodeDb.connect(" + from + ", " + to + ")");
            nodeDb.connect(from, to);
        }
        elt = elt->NextSiblingElement("connection");
    }
}


