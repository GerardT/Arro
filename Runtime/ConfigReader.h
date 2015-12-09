#ifndef XML_READER_H
#define XML_READER_H

#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include "Trace.h"
class NodeDb;

using namespace std;

#define NAME_SEPARATOR "."

class ConfigReader {
public:
    Trace trace;
    class Definition {
    public:
        TiXmlElement* node;
        Definition(TiXmlElement* n) {
            node = n;
		}
		
    };
    typedef std::map<std::string,std::string> StringMap;
    typedef std::map<string, Definition*> DefinitionMap;

private:
    NodeDb& nodeDb;

    /*
     * Each definition contains the XML chunk between <nodedefinition> and </nodedefinition>
     */
    DefinitionMap definitions;
public:

    ConfigReader(string filename, NodeDb& nodeDb);
    void getDefinition(TiXmlElement* node);

    void getPrimitive(string* url, string* instance, StringMap& params);
    void getManifest(TiXmlElement* manifest, string& instance);
    void getParamsAndSubstitute(TiXmlElement* node, StringMap& import_params, StringMap& params);

public:
    void makeNodeInstance(string className, string instanceName, string instancePrefix, StringMap& import_params);
};

#endif
