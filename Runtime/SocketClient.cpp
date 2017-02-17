/*
 * SocketClient.cpp
 *
 *  Created on: Feb 14, 2017
 *      Author: gerard
 */
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <thread>
#include <sys/stat.h>  /*for getting file size using stat()*/
#include <sys/sendfile.h>  /*for sendfile()*/
#include <fcntl.h>  /*for O_RDONLY*/
#include <unistd.h> // read

#include "SocketClient.h"



using namespace std;

/**
    TCP Client class
*/
SocketClient::SocketClient(const std::string& address, int port)
{
    sock = -1;
    conn(address, port);

    m_thrd = new std::thread(SocketClient::server, this);     // spawn new thread that calls server()
}

SocketClient::~SocketClient() {
    // synchronize threads:
    m_thrd->join();                // pauses until finishes

    delete m_thrd;
}

/**
    Connect to a host on a certain port number
*/
bool
SocketClient::conn(const std::string& address, int port) {
    //create socket if it is not already created
    if(sock == -1)
    {
        //Create socket
        sock = socket(AF_INET , SOCK_STREAM , 0);
        if (sock == -1)
        {
            perror("Could not create socket");
        }

        cout<<"Socket created\n";
    }

    //unsigned char buf[sizeof(struct in6_addr)];

    //setup address structure
    if(/*inet_pton(AF_INET, address.c_str(), buf) == 0*/ true /* not a valid network address */) {
        struct hostent *he;
        struct in_addr **addr_list;

        //resolve the hostname, its not an ip address
        if ( (he = gethostbyname( address.c_str() ) ) == NULL)
        {
            //gethostbyname failed
            herror("gethostbyname");
            cout<<"Failed to resolve hostname\n";

            return false;
        }

        //Cast the h_addr_list to in_addr , since h_addr_list also has the ip address in long format only
        addr_list = (struct in_addr **) he->h_addr_list;

        for(int i = 0; addr_list[i] != NULL; i++)
        {
            m_server.sin_addr = *addr_list[i];

            cout<<address<<" resolved to "<<inet_ntoa(*addr_list[i])<<endl;

            break;
        }
    }

    //plain ip address
    else
    {
        m_server.sin_addr.s_addr = inet_addr( address.c_str() );
    }

    m_server.sin_family = AF_INET;
    m_server.sin_port = htons( port );

    //Connect to remote server
    if (connect(sock , (struct sockaddr *)&m_server , sizeof(m_server)) < 0)
    {
        perror("connect failed. Error");
        return 1;
    }

    cout<<"Connected\n";
    return true;
}

/**
    Send data to the connected host
*/
bool
SocketClient::send_data(string data) {
    //Send some data
    if( send(sock , data.c_str() , strlen( data.c_str() ) , 0) < 0)
    {
        perror("Send failed : ");
        return false;
    }
    cout<<"Data send\n";

    return true;
}

void
SocketClient::server(SocketClient* me) {
    //send some data
    char buffer[100];
    bzero(buffer, 100);
    int n;

    while ((n = SocketClient::readln( me->getSock(), buffer, 100 )) != 0)
    {
        printf("buffer %s\n", buffer);
        std::chrono::milliseconds timespan(1000);
        std::this_thread::sleep_for(timespan);
        me->send_data("{\"firstName\":\"Piet\",\"lastName\":\"Paaltjes\"}\n");
        if(!strcmp(buffer, "terminate")) {
            break;
        }

    }

}

/**
 * Blocking read from socket until '\n' received. If socket is closed
 * then 'terminate' is returned in string buffer.
 *
 * \param sockfd File descriptor for IP socket.
 * \param buffer Buffer to return string into. May contain "terminate" if socket closed or EOF received.
 * \param n Max nr of characters to read.
 *
 * \return Actual nr of characters read.
 */
int
SocketClient::readln(int sockfd, char* buffer, size_t n/*size*/) {
    ssize_t numRead;                    /* # of bytes fetched by last read() */
    size_t totRead;                     /* Total bytes read so far */
    char *buf;
    char ch;

    if (n <= 0 || buffer == nullptr) {
        errno = EINVAL;
        return -1;
    }

    buf = buffer;

    totRead = 0;
    for (;;) {
        numRead = read(sockfd, &ch, 1);

        if (numRead == -1) {
            if (errno == EINTR)         /* Interrupted --> restart read() */
                continue;
            else {
                // Make losing socket to terminate process.
                printf("ServerEngine losing socket, terminated!");
                strcpy(buffer, "terminate");
                return strlen("terminate");
            }

        } else if (numRead == 0) {      /* EOF */
            if (totRead == 0) {         /* No bytes read */
                printf("ServerEngine terminated!\n");
                strcpy(buffer, "terminate");
                return strlen("terminate");
            }
            else
                break;

        } else {                        /* 'numRead' must be 1 if we get here */
            if (totRead < n - 1) {      /* Discard > (n - 1) bytes */
                totRead++;
                *buf++ = ch;
            }

            if (ch == '\n')
                break;
        }
    }

    *buf = '\0';
    return totRead;
}



