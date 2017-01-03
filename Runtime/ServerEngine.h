#ifndef ARRO_SERVER_ENGINE_H
#define ARRO_SERVER_ENGINE_H

#include <string>
#include <map>
#include <functional>

//#include "Process.h"

#define ARRO_PROGRAM_FILE    "arro_pgm"
#define ARRO_API_FILE        "arro_api"
#define ARRO_CONFIG_FILE     "download/arro.xml"
#define ARRO_FOLDER          "download/"


namespace Arro {


class Process;
class IDevice;



    /**
     * \brief Wrapper class that encapsulates ServerEngine functions.
     *
     * Should not be instantiated.
     */
    class ServerEngine {

    public:
        typedef std::function<IDevice* (Process* d, const std::string& instance, std::map<std::string, std::string>& params)> Factory;
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

        /**
         * Send text message to Eclipse client. Client will filter for these
         * message in order to determine success or failure of command sent.
         *
         * \param s String to send to Eclipse console.
         */
        static void console(std::string s);

        static void registerFactory(const std::string& name, Factory factory);

        static bool getFactory(const std::string& name, Factory& factory);

    };

    template<typename T>
    class RegisterMe {
    public:
        RegisterMe(const std::string& name) {
            ServerEngine::registerFactory(name, [](Process* d, const std::string& instance, std::map<std::string, std::string>& params) ->IDevice* {
                return new T(d, instance, params);
            });
        };
    };


}

#endif
