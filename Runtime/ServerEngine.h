#ifndef ARRO_SERVER_ENGINE_H
#define ARRO_SERVER_ENGINE_H

#include <string>
#include <map>
#include <functional>

#include "INodeContext.h"

#define ARRO_PROGRAM_FILE    "arro_pgm"
#define ARRO_API_FILE        "arro_api"
#define ARRO_CONFIG_FILE     "../../download/arro.xml"
#define ARRO_TEMPLATE        "../../download/arro.html"
#define ARRO_INDEX           "../../download/index.html"
#define ARRO_FOLDER          "../../download/"

struct TiXmlElement;

namespace Arro {


class Process;
class INodeDefinition;


    /**
     * \brief Wrapper class that encapsulates ServerEngine functions.
     *
     * Should not be instantiated.
     */
    class ServerEngine {

    public:
        /**
         * This class is not for instantiation.
         */
        ServerEngine() = delete;
        ServerEngine(const ServerEngine&) = delete;
        ServerEngine& operator=(const ServerEngine& other) = delete;

        static bool getFactory(const std::string& name, Factory& factory);

    };


}

#endif
