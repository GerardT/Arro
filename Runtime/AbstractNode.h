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


    class AbstractNode {
    public:
        /**
         * Constructor.
         */
        AbstractNode() {};
        virtual ~AbstractNode() {};

        virtual void UiSend(const std::string& json) = 0;

        virtual bool UiReceive(std::string& json) = 0;

        virtual void sendParameters(StringMap& params) = 0;

        /**
         * Lookup an input by its name, which is internally concatenated: "nodename.name".
         *
         * \param name Name of input.
         */
        virtual NodeSingleInput*  getInput(const std::string& name) const = 0;

        virtual MessageBuf getInputData(NodeSingleInput* input) const = 0;

        /**
         * Lookup an output by its name, which is concatenated: "procesname.name".
         *
         * \param name Name of output.
         */
        virtual NodeMultiOutput* getOutput(const std::string& name) const = 0;

        virtual void setOutputData(NodeMultiOutput* output, google::protobuf::MessageLite* msg) const = 0;

        /**
         * Get name of the node.
         *
         * \return name Name of the node.
         */
        virtual const std::string& getName() const = 0;

        /**
         * TODO add API for debugging, e.g. single step support for Timer node.
         */
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
