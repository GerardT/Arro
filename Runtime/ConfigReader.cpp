
#include <iostream>
#include <vector>
#include <exception>

#include "Trace.h"
#include "ConfigReader.h"
#include "ServerEngine.h"
#include "NodeDb.h"
#include "Pad.h"
#include "Process.h"
#include "SocketClient.h"


using namespace std;
using namespace Arro;

ConfigReader::ConfigReader(const string& filename, NodeDb& db):
     m_trace{"ConfigReader", true},
     m_nodeDb{db}
{
    auto params = new StringMap();

    TiXmlDocument doc(filename);
    if (!doc.LoadFile()) {
        m_trace.println("Cannot read file " + filename);
        throw std::runtime_error("Cannot read file " + filename);
        return;
    }

    TiXmlElement* node = doc.FirstChildElement("modules");

    if(!node) {
        m_trace.println("node 'modules' missing");
        throw std::runtime_error("node 'modules' missing");
    }

    // Collect all module blocks and store them in map 'definitions'.
    TiXmlElement* elt = node->FirstChildElement("module");
    while (elt) {
        storeDefinition(elt);
        elt = elt->NextSiblingElement("module");
    }

    // Recursively process all modules, starting with "Main"
    unsigned int padId = 1;
    makeNodeInstance("Main", "main", "", *params, nullptr, padId);

    // fix pads
    // m_nodeDb.visitNodes([](RealNode& c) { c.fixInputs();});

    m_nodeDb.visitNodes([](RealNode& c) { c.finishConstruction();});

    SocketClient::getInstance()->generateWebUi();
}

ConfigReader::~ConfigReader() {
    // empty
}

void
ConfigReader::storeDefinition(TiXmlElement* node) {
    const char* attr = node->Attribute("type");

    if(attr != nullptr){
        m_definitions[attr] = new Definition(node);
        m_trace.println(string("Added definition ") + attr);
    }
    else {
        m_trace.println("No type found? ");
        throw std::runtime_error("No type definition for " + string(attr));
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
        m_trace.println("found parameter  : key " + *keyAttr + ", subst " + *substAttr + ", value " + *valueAttr);
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
            m_trace.println("faulty parameter block");
            throw std::runtime_error("faulty parameter block");
        }
        m_trace.println("updated parameter: key " + *keyAttr + ", value " + params[*keyAttr]);
        elt = elt->NextSiblingElement("param");
    }
}


void
ConfigReader::makeNodeInstance(const string& typeName, const string& instanceName, const string& instancePrefix, StringMap& import_params, Process* parentSfc, unsigned int& padId) {
    Definition* def = m_definitions[typeName];
    Process* processNode = nullptr;  // Note: there is always just one elem_block (= Process node) in a module
    Process* sfcNode = nullptr;

    if(def == nullptr)
    {
        SendToConsole("Element not found: " + typeName);
        m_trace.println("Element not found: " + typeName);
        throw std::runtime_error("Element not found: " + typeName);
    }
    string instance = instancePrefix + ARRO_NAME_SEPARATOR + instanceName;

    m_trace.newln();
    m_trace.println("makeNodeInstance for " + typeName + ", " + instance);
    TiXmlElement* elt;


    // Read elemBlocks (the leaves in the tree)
    elt = def->m_node->FirstChildElement("elem_block");
    while(elt) {
        const string* typeURL = elt->Attribute(string("url"));

        if(typeURL != nullptr) {
            // collect all parameters
            StringMap* params = new StringMap();
            getParamsAndSubstitute(elt, import_params, *params);

            // create Process object; inputs & outputs to be added later.
            processNode = new Process(m_nodeDb, *typeURL, instance, *params);

#if PARAM_AS_CONFIG
            // register config not for parameter reception
            m_trace.println("registerInput(" +instance + "#_config)");
            processNode->registerInput("_config", true);
#endif

            delete params;

            string from = "_action";
            string to = "_step";
            m_trace.println("registerInput(" +instance + "#" + from + ")");
            processNode->registerInput(from, true);
            m_trace.println("registerOutput(" +instance + "#" + to + "), Id " + std::to_string(padId));
            processNode->registerOutput(padId++, to);
        }
        elt = elt->NextSiblingElement("elem_block");
    }
    // Read elemBlocks (the leaves in the tree) There should be only one sfc node.
    elt = def->m_node->FirstChildElement("sfc");
    while(elt) {
        StringMap* params = new StringMap();

        // EXTRA Simulate adding a new module
        {
            /*
            <module id="12...34" type="_Sfc">
        elemBlock  <device id="12...34" url="Sfc:"/>
                <pad id="12...34" input="true" name="_action" run="true" type="Action"/>
                <pad id="12...34" input="false" name="_action_nodeX" run="true" type="Action"/>
                <pad id="12...34" input="false" name="_action_nodeY" run="true" type="Action"/>
            </module>

            Pads for nodeX and nodeY are added when parsing those nodes.
            */

            string instanceSfc = instance + ARRO_SFC_INSTANCE;

            sfcNode = new Process(m_nodeDb, "Sfc:", instanceSfc, *params, elt);

            m_trace.println("registerInput(" +instance + "#_action)");
            sfcNode->registerInput("_action", true);

            m_trace.println("registerOutput(" +instance + "#_step), Id " + std::to_string(padId));
            sfcNode->registerOutput(padId++, "_step"); // experimental

            m_trace.println("registerInput(" +instance + "#_steps)");
            sfcNode->registerInput("_steps", true);

            // EXTRA Create an _action and _step pad in every module
            // If processNode != nullptr then we know a 'leaf' node is being added
            if(!processNode) {
                new Pad(m_nodeDb, "Action", instance + ARRO_PAD_SEPARATOR + "_action", padId++);
                new Pad(m_nodeDb, "Mode", instance + ARRO_PAD_SEPARATOR + "_step", padId++);

                // connect Pad to Sfc
                {
                    string from = instance + ARRO_PAD_SEPARATOR + "_action";

                    string to = instanceSfc + ARRO_PAD_SEPARATOR + "_action";

                    m_trace.println("nodeDb.connect(" + from + ", " + to + ")");
                    m_nodeDb.connect(from, to);
                }
                // connect Sfc to Pad
                {
                    string from = instanceSfc + ARRO_PAD_SEPARATOR + "_step";

                    string to = instance + ARRO_PAD_SEPARATOR + "_step";

                    m_trace.println("nodeDb.connect(" + from + ", " + to + ")");
                    m_nodeDb.connect(from, to);
                }
            }
        }

        elt = elt->NextSiblingElement("sfc");
    }
    // Read function blocks (the branches in the tree)
    elt = def->m_node->FirstChildElement("function_block");
    while(elt) {
        const string* typeAttr = elt->Attribute(string("type"));
        const string* idAttr = elt->Attribute(string("name"));
        // trace.println("New node def ");
        if(typeAttr != nullptr && idAttr != nullptr) {
            // collect all parameters
            StringMap* params = new StringMap();
            getParamsAndSubstitute(elt, import_params, *params);

            makeNodeInstance(*typeAttr,  *idAttr,  instance, *params, sfcNode, padId);


            // Every time a function block <f> is added to parent <p>:
            // Register <p>._Sfc#_action_<f>
            // Register <p>._Sfc#_step_<f>
            // Connect <p>._Sfc#_action_<f> to <p>.<f>#_action
            // Connect <p>.<f>#_step to <p>._Sfc#_step_<f>
            {
                string from, to;

                // EXTRA Register _action output and _step input for each node with Sfc Node
                m_trace.println("registerOutput(" +instance + "#_action_" + *idAttr + "), Id " + std::to_string(padId));
                sfcNode->registerOutput(padId++, "_action_" + *idAttr);
                m_trace.println("registerInput(" +instance + "#_step_" + *idAttr + ")");
                sfcNode->registerInput("_step_" + *idAttr, true);

                // Connect from sfc to just created node (in makeNodeInstance).
                from = instance + ARRO_SFC_INSTANCE + ARRO_PAD_SEPARATOR + "_action_" + *idAttr;

                to = instance + ARRO_NAME_SEPARATOR + *idAttr + ARRO_PAD_SEPARATOR + "_action";

                m_trace.println("nodeDb.connect(" + from + ", " + to + ")");
                m_nodeDb.connect(from, to);

                // Connect from just created node (in makeNodeInstance) to sfc.
                to = instance + ARRO_SFC_INSTANCE + ARRO_PAD_SEPARATOR + "_step_" + *idAttr;

                from = instance + ARRO_NAME_SEPARATOR + *idAttr + ARRO_PAD_SEPARATOR + "_step";

                m_trace.println("nodeDb.connect(" + from + ", " + to + ")");
                m_nodeDb.connect(from, to);

                // Connect from just created node (in makeNodeInstance) to sfc.
                to = instance + ARRO_SFC_INSTANCE + ARRO_PAD_SEPARATOR + "_steps";

                from = instance + ARRO_NAME_SEPARATOR + *idAttr + ARRO_PAD_SEPARATOR + "_step";

                m_trace.println("nodeDb.connect(" + from + ", " + to + ")");
                m_nodeDb.connect(from, to);


                // for Main/main, add dummy output to SFC
                // Register main._Sfc#_special_action
                // Connect main._Sfc#_special_action to main._Sfc#_action
                if(typeName == "Main" && instanceName == "main") {
                    sfcNode->registerOutput(padId++, "_special_action");

                    from = instance + ARRO_SFC_INSTANCE + ARRO_PAD_SEPARATOR + "_special_action";

                    to = instance + ARRO_SFC_INSTANCE + ARRO_PAD_SEPARATOR + "_action";

                    m_nodeDb.connect(from, to);
                    m_nodeDb.registerMainSfc(sfcNode);
                }
            }
            delete params;
        }

        elt = elt->NextSiblingElement("function_block");
    }

    // Read pads (other leaves in the tree)
    elt = def->m_node->FirstChildElement("pad");
    while(elt) {
        const string* datatypeAttr = elt->Attribute(string("type"));
        const string* idAttr = elt->Attribute(string("name"));
        const string* inputAttr = elt->Attribute(string("input"));
        const string* runAttr = elt->Attribute(string("run"));

        if(datatypeAttr != nullptr && idAttr != nullptr) {
            string inst = instance + ARRO_PAD_SEPARATOR + *idAttr;

            // If processNode != nullptr then we know a 'leaf' node is being added
            if(processNode) {
                if(*inputAttr == "true") {
                    if(runAttr != nullptr && *runAttr == "true") {
                        m_trace.println("registerInput(" +instance + "#" + *idAttr + ")");
                        processNode->registerInput(*idAttr, true);
                    } else {
                        m_trace.println("registerInput(" +instance + "#" + *idAttr + ")");
                        processNode->registerInput(*idAttr, false);
                    }
                } else {
                    m_trace.println("registerOutput(" +instance + "#" + *idAttr + "), Id " + std::to_string(padId));
                    processNode->registerOutput(padId++, *idAttr);
                }
            } else {
                // create Pad object with input and output.
                m_trace.println("new Pad(" + *datatypeAttr + ", " + inst + ")");
                new Pad(m_nodeDb, *datatypeAttr, inst, padId++);
            }
        }
        elt = elt->NextSiblingElement("pad");
    }


    // Read connections
    elt = def->m_node->FirstChildElement("connection");
    while(elt) {
        const string* fromAttr = elt->Attribute(string("source"));
        const string* toAttr = elt->Attribute(string("target"));
        if(fromAttr != nullptr && toAttr != nullptr) {
            string from = instance;
            if(fromAttr->find(ARRO_PAD_SEPARATOR) == fromAttr->npos) {
                from += ARRO_PAD_SEPARATOR + *fromAttr;
            } else {
                from += ARRO_NAME_SEPARATOR + *fromAttr;
            }
            string to = instance;
            if(toAttr->find(ARRO_PAD_SEPARATOR) == toAttr->npos) {
                to += ARRO_PAD_SEPARATOR + *toAttr;
            } else {
                to += ARRO_NAME_SEPARATOR + *toAttr;
            }

            m_trace.println("nodeDb.connect(" + from + ", " + to + ")");
            m_nodeDb.connect(from, to);
        }
        elt = elt->NextSiblingElement("connection");
    }

    // Provide steps and actions to parentSfc so parent Sfc can check that its conditions and actions are legal.
    // Maybe in future host system can do this check. Or we can use protobuf enum.
    if(parentSfc && sfcNode) parentSfc->registerChildSfc(instanceName, sfcNode);
    //if(sfcNode) sfcNode->test();
}


