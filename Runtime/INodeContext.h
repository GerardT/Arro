#ifndef ARRO_I_NODE_CONTEXT_H
#define ARRO_I_NODE_CONTEXT_H

#include <memory>
#include <functional>
#include <string>
#include <map>
#include <list>
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
        class Iterator {
        public:
            virtual ~Iterator() {};
            virtual bool getNext(MessageBuf& /*msg*/) = 0;
            virtual void insertOutput(google::protobuf::MessageLite& msg) = 0;
            virtual void insertOutput(MessageBuf& msg) = 0;
            virtual void updateOutput(google::protobuf::MessageLite& msg) = 0;
            virtual void updateOutput(MessageBuf& msg) = 0;
            virtual void deleteOutput() = 0; // it will be empty again
        };

        typedef std::unique_ptr<Iterator> ItRef;
        typedef enum { ALL, DELTA, LATEST } Mode;

        /**
         * Constructor.
         */
        INodeContext() {};
        virtual ~INodeContext() {};

        /**
         * Lookup an input by its name, which is internally concatenated: "nodename#name".
         *
         * \param name Name of input.
         */
        virtual InputPad*  getInputPad(const std::string& name) const = 0;

        virtual MessageBuf getInputData(InputPad* input) const = 0;

        virtual const std::list<unsigned int>& getConnections(InputPad* input) = 0;

        /**
         * Get first record that was changed since previous run-cycle.
         * @param input
         * @param connection: 0 = all connections
         * @param msg
         * @param mode all / delta since previous run-cycle / latest run-cycle only
         * @return
         *
         * Example, table contains records A, B, C. Then in run-cycle 123:
         * record A is updated
         * record D is added
         * record B is deleted
         * record A is updated
         * Contents:
         * +------------+------------+------------+
         * | padId      | record     | run-cycle  |
         * +------------+------------+------------+
         * | 10         | A          | 123        |
         * | 10         | B x        | 123        |
         * | 10         | C          | 122        |
         * | 10         | D          | 123        |
         * +------------+------------+------------+
         * Each node only updates its 'own' records, with its own padId.
         */
        virtual ItRef begin(InputPad* input, unsigned int connection, Mode mode) = 0;
        virtual ItRef end(OutputPad* input) = 0;


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
