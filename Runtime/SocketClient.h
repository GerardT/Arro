/*
 * SocketClient.h
 *
 *  Created on: Feb 14, 2017
 *      Author: gerard
 */

#ifndef SOCKETCLIENT_H_
#define SOCKETCLIENT_H_

#include <thread>
#include <iostream>    //cout
#include <sys/types.h>
#include <string>  //string
#include <sys/socket.h>    //socket
#include <arpa/inet.h> //inet_addr
#include <netdb.h> //hostent
#include <mutex>
#include <queue>
#include <map>

#include "Trace.h"

namespace Arro
{
class NodeRef;

// Singleton class
class SocketClient {
public:
    SocketClient(const std::string& address, int port);
    virtual ~SocketClient();
    bool conn(const std::string& address, int port);
    NodeRef* subscribe(const std::string& nodeName, const std::string& component, std::function<void (const std::string& data)> listen);
    void unsubscribe(NodeRef* clientId);
    bool sendMessage(NodeRef* uiClient, const std::string& data);
    static int readln(int sockfd, char* buffer, size_t n/*size*/);
    void serve();
    static SocketClient* getInstance() { return m_inst; };
    void generateWebUi();

private:
    Trace m_trace;
    int sock;
    struct sockaddr_in m_server;
    std::thread* m_thrd;
    std::mutex m_mutex;
    std::queue<std::shared_ptr<std::string>> m_stringQueue;

    std::map<std::string, NodeRef*> m_clients;
    static SocketClient* m_inst;
};
}


#endif /* SOCKETCLIENT_H_ */
