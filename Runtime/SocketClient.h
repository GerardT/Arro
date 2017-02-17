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

class SocketClient {
private:
    int sock;
    struct sockaddr_in m_server;
    std::thread* m_thrd;

public:
    SocketClient(const std::string& address, int port);
    virtual ~SocketClient();
    int getSock() { return sock; };
    bool conn(const std::string& address, int port);
    bool send_data(std::string data);
    static void server(SocketClient* me);
    static int readln(int sockfd, char* buffer, size_t n/*size*/);
};


#endif /* SOCKETCLIENT_H_ */
