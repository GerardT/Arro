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

namespace Arro
{
class NodeRef;

// Singleton class
class SocketClient {
private:
    int sock;
    struct sockaddr_in m_server;
    std::thread* m_thrd;
    std::mutex m_mutex;
    std::queue<std::shared_ptr<std::string>> m_stringQueue;

    std::map<std::string, NodeRef*> m_clients;

public:
    SocketClient(const std::string& address, int port);
    virtual ~SocketClient();
    bool conn(const std::string& address, int port);
    NodeRef* subscribe(const std::string& nodeName, std::function<void ()> listen);
    bool getMessage(NodeRef* uiClient, std::shared_ptr<std::string>& data);
    bool sendMessage(NodeRef* uiClient, const std::string& data);
    void serve();
    static int readln(int sockfd, char* buffer, size_t n/*size*/);
    static SocketClient* getInstance() { return m_inst; };
    static SocketClient* m_inst;
};
}


#endif /* SOCKETCLIENT_H_ */
