#ifndef ARRO_SERVER_ENGINE_H
#define ARRO_SERVER_ENGINE_H

#include <string>
#include <map>
#include <functional>

#include "INodeContext.h"

#define ARRO_PROGRAM_FILE    "arro_pgm"
#define ARRO_API_FILE        "arro_api"
#define ARRO_CONFIG_FILE     "download/arro.xml"
#define ARRO_FOLDER          "download/"

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

        /**
         * Start server thread for Eclipse client.
         */
        static void start();

        /**
         * Stop server thread for Eclipse client. Will never be called.
         */
        static void stop();


        static bool getFactory(const std::string& name, Factory& factory);

    };


}

#endif
