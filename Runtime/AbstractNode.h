#ifndef ARRO_ABSTRACT_NODE_H
#define ARRO_ABSTRACT_NODE_H

#include <memory>
#include <functional>
#include <string>
#include "IDevice.h"
#include <tinyxml.h>
#include <google/protobuf/message.h>


#define ARRO_NAME_SEPARATOR "."

namespace Arro
{

    class NodeSingleInput;
    class NodeMultiOutput;

    typedef std::map<std::string, std::string> StringMap;

    /**
     * Send text message to Eclipse client. Client will filter for these
     * message in order to determine success or failure of command sent.
     *
     * \param s String to send to Eclipse console.
     */
    void SendToConsole(std::string s);


    /**
     * \brief Abstract base class for Pad and Process.
     *
     * The name is the instance name: ".node.subnode.subnode"
     *
     * AbstractNode is the NodeDb-internal representation of a Node.
     *
     * A typical Eclipse diagram consists of nodes and pads. The pads are the
     * connection points to other nodes in parent diagrams.
     * Both pads and nodes are implemented as nodes that receive and send messages
     * to each other. The use NodeSingleInput and NodeMultiOutput objects to connect
     * to each other.
     */
    class AbstractNode {
    public:
        /**
         * Constructor.
         *
         * \param n Name of the node.
         */
        AbstractNode(const std::string& n): m_name(n) {};
        virtual ~AbstractNode() {};

        virtual void sendParameters(StringMap& params) = 0;

// TODO move registerInput and registerOutput to ConfigReader
        /**
         * Called from ConfigReader in order to register an input Pad as input. Basically
         * it installs a listener for this Pad that handles incoming messages.
         *
         * \param interfaceName Name of the interface to register.
         * \param enableRunCycle Set runCycle to true every time a message is received on this interface.
         */
        virtual void registerInput(const std::string& interfaceName, bool enableRunCycle) = 0;

        /**
         * Called from ConfigReader in order to register an output Pad as output.
         *
         * \param interfaceName Name of the interface to register.
         */
        virtual void registerOutput(const std::string& interfaceName) = 0;

        /**
         * Lookup an input by its name, which is internally concatenated: "procesname.name".
         *
         * \param name Name of input.
         */
        virtual MessageBuf getInputData(const std::string& name) const = 0;

        /**
         * Lookup an input by its name, which is internally concatenated: "procesname.name".
         *
         * \param name Name of input.
         */
        virtual NodeSingleInput*  getInput(const std::string& name) const = 0;

        /**
         * Lookup an output by its name, which is concatenated: "procesname.name".
         *
         * \param name Name of output.
         */
        virtual NodeMultiOutput* getOutput(const std::string& name) const = 0;

        virtual void submitMessage(std::string pad, google::protobuf::MessageLite* msg) const = 0;

        /**
         * Get name of the node.
         *
         * \return name Name of the node.
         */
        const std::string& getName() const { return m_name; };

        /**
         * Make the node execute one cycle.
         */
        virtual void runCycle() = 0;

    private:
        std::string m_name;
    };



    typedef std::function<IDevice* (AbstractNode* d, const std::string& instance, std::map<std::string, std::string>& params, TiXmlElement* elt)> Factory;

    void registerFactory(const std::string& name, Factory factory);


    // RegisterMe creates a factory class for T. In each Node source file one instance
    // is created from this class; upon instantiation it registers the factory (itself)
    // using ServerEngine::registerFactory.
    // So instances of T can be created and registered in the NodeDb when needed.
    template<typename T>
    class RegisterMe {
    public:
        RegisterMe(const std::string& name) {
            registerFactory(name, [](AbstractNode* d, const std::string& instance, std::map<std::string, std::string>& params, TiXmlElement* elt) ->IDevice* {
                return new T(d, instance, params, elt);
            });
        };
    };

}


#endif
