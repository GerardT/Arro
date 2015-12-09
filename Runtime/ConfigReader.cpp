
#include <iostream>
#include <vector>
#include <exception>

#include <Trace.h>
#include "ConfigReader.h"
#include <NodeDb.h>
#include <Pad.h>
#include <Process.h>


using namespace std;

/**
 * Constructor, which is passed the filename of the file to parse. It
 * will populate the passed (empty) NodeDb instance.
 */
ConfigReader::ConfigReader(std::string filename, NodeDb& db):

     trace(string("ConfigReader"), true),
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
        getDefinition(elt);
        elt = elt->NextSiblingElement("nodedefinition");
    }
    trace.println("");

    // Recursively process all nodedefinitions, starting with "Main"
    makeNodeInstance(string("Main"), string("main"), string(""), *params);
}

/**
 * Find a "nodedefinition" and store in map.
 * A nodedefinition for example:
 * <nodedefinition id="c02854c7-2cbc-4c30-9cb2-76872b3186b0" type="NewNode">
 *    :
 * </nodedefinition>
 *
 * @param node
 */
void
ConfigReader::getDefinition(TiXmlElement* node) {
    const char* attr = node->Attribute("type");

    if(attr != NULL){
        definitions[attr] = new Definition(node);
        trace.println(string("Added definition ") + attr);
    }
    else {
        trace.fatal("No type found? ");
    }
}



/**
 * Process parameters for a node in a diagram.
 * For example:
 * <nodedefinition id="c02854c7-2cbc-4c30-9cb2-76872b3186b0" type="NewNode">
 *     <node id="609bb62b-8f4c-4708-b168-7de11c6826e3" name="aNewDevice" type="NewDevice"/>
 *     <pad id="02b7ccf9-fe1f-43fe-8441-0da0b8a04b8d" input="true" name="afixed32" type="fixed32"/>
 *     <pad id="955c676e-d808-4aad-836b-2d376afa352c" input="true" name="abool" type="bool"/>
 *     <param key="P1" subst="Motor1" value="1"/>
 * </nodedefinition>
 *
 * If a parameter in import_params has a key equal to the 'subst' value in the param element,then
 * use the value of import_param to replace value in param element.
 * Return resulting map in 'params'.
 *
 * In the end, the parameters are really only used in primitive nodes.
 */
void
ConfigReader::getParamsAndSubstitute(TiXmlElement* node, StringMap& import_params, StringMap& params) {
    //trace.println("getParamsAndSubstitute");
    // non-primitive node
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


/**
 * In a recursive way, process nodedefinition XML blocks (node).
 * For example:
 * <nodedefinition id="c02854c7-2cbc-4c30-9cb2-76872b3186b0" type="NewNode">
 *     <node id="609bb62b-8f4c-4708-b168-7de11c6826e3" name="aNewDevice" type="NewDevice"/>
 *     <pad id="02b7ccf9-fe1f-43fe-8441-0da0b8a04b8d" input="true" name="afixed32" type="fixed32"/>
 *     <pad id="955c676e-d808-4aad-836b-2d376afa352c" input="true" name="abool" type="bool"/>
 *     <param key="P1" subst="Motor1" value="1"/>
 * </nodedefinition>
 *
 * If (definition) node has subnodes, instantiate them as well.
 * If (definition) node has no subnodes, then assumed to be primitive node.
 *
    new NodePid("main.pid1");
    new NodePid("main.pid2");
    NodeConfig c = new NodeConfig("main.config");
    NodeTimer t = new NodeTimer("main.timer");

    nodeDb().connect("main.timer.result", "main.pid1.timer");
    nodeDb().connect("main.pid1.result", "main.pid1.sub1");
    nodeDb().connect("main.config.result", "main.pid1.sub1");
 *
 */
void
ConfigReader::makeNodeInstance(string typeName, string instanceName, string instancePrefix, StringMap& import_params) {
    Definition* def = definitions[typeName];
    Process* device = NULL;

    if(def == NULL)
    {
    	trace.fatal("Element not found: " + typeName);
    	return;
    }
    string instance = "";

    instance = instancePrefix + NAME_SEPARATOR + instanceName;

    trace.println("makeNodeInstance for " + typeName + ", " + instance);
    TiXmlElement* elt;


    // Read devices
    elt = def->node->FirstChildElement("device");
    while(elt) {
        const string* typeURL = elt->Attribute(string("url"));

        if(typeURL != NULL) {
            // collect all parameters
            StringMap* params = new StringMap();
            getParamsAndSubstitute(elt, import_params, *params);

        	// create Process object; inputs & outputs to be added later.
            device = new Process(nodeDb, typeURL, &instance, *params);

            delete params;
        }
        elt = elt->NextSiblingElement("device");
    }

    // Read nodes
    elt = def->node->FirstChildElement("node");
    while(elt) {
        const string* typeAttr = elt->Attribute(string("type"));
        const string* idAttr = elt->Attribute(string("name"));
        // trace.println("New node def ");
        if(typeAttr != NULL && idAttr != NULL) {
            //StringMap* params = new StringMap();
            //getParamsAndSubstitute(elt, import_params, *params);
            // collect all parameters
            StringMap* params = new StringMap();
            getParamsAndSubstitute(elt, import_params, *params);

            makeNodeInstance(*typeAttr,  *idAttr,  instance, *params);

            delete params;
        }
        elt = elt->NextSiblingElement("node");
    }

    // Read pads
    elt = def->node->FirstChildElement("pad");
    while(elt) {
        const string* datatypeAttr = elt->Attribute(string("type"));
        const string* idAttr = elt->Attribute(string("name"));
        const string* inputAttr = elt->Attribute(string("input"));
        const string* runAttr = elt->Attribute(string("run"));

        if(datatypeAttr != NULL && idAttr != NULL) {
            string inst = instance + NAME_SEPARATOR + *idAttr;
            if(device) {
            	if(*inputAttr == "true") {
            		if(runAttr != NULL && *runAttr == "true") {
                    	device->registerInput(*idAttr, true);
            		} else {
                    	device->registerInput(*idAttr, false);
            		}
            	} else {
            		device->registerOutput(*idAttr);
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
        if(fromAttr != NULL && toAttr != NULL) {
            string from = instance + NAME_SEPARATOR + *fromAttr;

            string to = instance + NAME_SEPARATOR + *toAttr;

            trace.println("nodeDb.connect(" + from + ", " + to + ")");
            nodeDb.connect(from, to);
        }
        elt = elt->NextSiblingElement("connection");
    }
}


