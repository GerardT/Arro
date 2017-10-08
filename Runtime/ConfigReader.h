#ifndef ARRO_CONFIG_READER_H
#define ARRO_CONFIG_READER_H

#include <tinyxml.h>
#include <iostream>
#include <sstream>
#include <map>

#include "AbstractNode.h"
#include "Trace.h"

#define ARRO_SFC_INSTANCE   "._Sfc"


namespace Arro
{
    class NodeDb;
    class Process;

    /**
    * \brief Read config file arro.xml and build database of nodes.
    *
    * This class will read the config file (arro.xml) and create instances
    * of Nodes and connect them.
    */
    class ConfigReader
    {
    public:
        /**
         * Constructor.
         *
         * \param filename Filename describing all nodes (arro.xml).
         * \param db Node database into which to store all nodes. Should be empty before calling.
         */
        ConfigReader(const std::string& filename, NodeDb& nodeDb);
        virtual ~ConfigReader();

        // Copy and assignment is not supported.
        ConfigReader(const ConfigReader&) = delete;
        ConfigReader& operator=(const ConfigReader& other) = delete;


    private:
        /*
         * \brief Class that contains XML chunk for one node definition.
         *
         * Each Definition contains the XML chunk between <nodedefinition> and </nodedefinition>.
         * This class doesn't do a lot, mainly for future extension and wrapping of TiXml.
         */
        class Definition {
        public:
            TiXmlElement* m_node;
            Definition(TiXmlElement* n):
                m_node(n) {
            }
        };

        typedef std::map<std::string, Definition*> DefinitionMap;

        /**
         * Store a node definition in 'definitions' maps with its type as key.
         * A nodedefinition for example:
         * <nodedefinition id="c02854c7-2cbc-4c30-9cb2-76872b3186b0" type="NewNode">
         *    :
         * </nodedefinition>
         *
         * \param node The node to be stored.
         */
        void storeDefinition(TiXmlElement* node);

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
         * If a parameter in importParams has a key equal to the 'subst' value in the param element,then
         * use the value of importParams to replace value in param element.
         * Return resulting map in 'params'.
         *
         * In the end, the parameters are really only used in primitive nodes.
         *
         * \param node The node for which parameters are collected.
         * \param importParams List of parameters used as reference.
         * \param params List of parameters to update.
         */
        void getParamsAndSubstitute(TiXmlElement* node, StringMap& importParams, StringMap& params);

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
         * \param className Class name to use for creating instance.
         * \param instanceName Instance name to use for creating instance.
         * \param instancePrefix Prefix to use for this instance.
         * \param importParams list of parameters to be pass to instance.
         */
        void makeNodeInstance(const std::string& className, const std::string& instanceName, const std::string& instancePrefix, StringMap& importParams, Process* parentSfc);

    private:
        Trace m_trace;
        NodeDb& m_nodeDb;
        DefinitionMap m_definitions;
    };
}


#endif
