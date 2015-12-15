#ifndef ARRO_SERVER_ENGINE_H
#define ARRO_SERVER_ENGINE_H

#include <string>


namespace Arro {

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

        /**
         * Send text message to Eclipse client. Client will filter for these
         * message in order to determine success or failure of command sent.
         *
         * \param s String to send to Eclipse console.
         */
        static void console(std::string s);
    };
}

#endif
