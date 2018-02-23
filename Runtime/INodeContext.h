#ifndef ARRO_I_NODE_CONTEXT_H
#define ARRO_I_NODE_CONTEXT_H

#include <memory>
#include <functional>
#include <string>
#include <map>
#include <tinyxml.h>
#include <google/protobuf/message.h>
#include "INodeDefinition.h"



#define ARRO_NAME_SEPARATOR "."
#define ARRO_PAD_SEPARATOR "#"

namespace Arro
{

    class InputPad;
    class OutputPad;

    typedef std::map<std::string, std::string> StringMap;

    /**
     * Send text message to Eclipse client. Client will filter for these
     * message in order to determine success or failure of command sent.
     *
     * \param s String to send to Eclipse console.
     */
    void SendToConsole(const std::string& s);


    class INodeContext {
    public:
        /**
         * Constructor.
         */
        INodeContext() {};
        virtual ~INodeContext() {};

        virtual void sendParameters(StringMap& params) = 0;

        /**
         * Lookup an input by its name, which is internally concatenated: "nodename#name".
         *
         * \param name Name of input.
         */
        virtual InputPad*  getInputPad(const std::string& name) const = 0;

        virtual MessageBuf getInputData(InputPad* input) const = 0;

        /**
         * Lookup an output by its name, which is concatenated: "procesname#name".
         *
         * \param name Name of output.
         */
        virtual OutputPad* getOutputPad(const std::string& name) const = 0;

        virtual void setOutputData(OutputPad* output, google::protobuf::MessageLite* msg) const = 0;

        /**
         * Get name of the node.
         *
         * \return name Name of the node.
         */
        virtual const std::string& getName() const = 0;

        virtual std::string getParameter(const std::string& parm) = 0;

        /**
         * TODO add API for debugging, e.g. single step support for Timer node.
         */
    };



    typedef std::function<INodeDefinition* (INodeContext* d, const std::string& instance, std::map<std::string, std::string>& params, TiXmlElement* elt)> Factory;

    void registerFactory(const std::string& name, Factory factory);


    // RegisterMe creates a factory class for T. In each Node source file one instance
    // is created from this class; upon instantiation it registers the factory (itself)
    // using ServerEngine::registerFactory.
    // So instances of T can be created and registered in the NodeDb when needed.
    template<typename T>
    class RegisterMe {
    public:
        RegisterMe(const std::string& name) {
            registerFactory(name, [](INodeContext* d, const std::string& instance, std::map<std::string, std::string>& params, TiXmlElement* elt) ->INodeDefinition* {
                return new T(d, instance, params, elt);
            });
        };
    };

}


#endif
