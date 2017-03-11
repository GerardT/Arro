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
using namespace Arro;

static void server(SocketClient* me) {
    me->serve();
}

SocketClient* SocketClient::m_inst = nullptr;

class Arro::NodeRef {
public:
    NodeRef(const std::string& n, std::function<void (const std::string& data)> l):
        nodeName{n},
        listen{l}
        {};
    const string& getName() { return nodeName; };
public:
    string nodeName;
    std::function<void (const std::string& data)> listen;
};

/**
    TCP Client class
*/
SocketClient::SocketClient(const std::string& address, int port):
       m_mutex{}
{
    sock = -1;
    conn(address, port);

    m_inst = this;

    m_thrd = new std::thread(server, this);     // spawn new thread that calls server()
}

SocketClient::~SocketClient() {
    // synchronize threads:
    m_thrd->join();                // pauses until finishes

    delete m_thrd;
}


NodeRef*
SocketClient::subscribe(const string& nodeName, std::function<void (const std::string& data)> listen) {
    NodeRef* n = new NodeRef(nodeName, listen);
    m_clients[nodeName] = n;
    return n;
}

void
SocketClient::unsubscribe(NodeRef* clientId) {
    m_clients.erase(clientId->getName());
}

void
SocketClient::generateWebUi() {
    auto fp = fopen("ui.html", "w");
    for(std::pair<std::string, NodeRef*> node: m_clients) {
        fprintf(fp, "address %s", node.second->nodeName.c_str());

    }
    fclose(fp);
}


/**
    Send data to the connected host
    Add address:
    { "address" : "xyz.abc", "data" : {  ... data ...  };

*/
bool
SocketClient::sendMessage(NodeRef* uiClient, const string& data) {
    string newData = "{ \"address\" : \"" + uiClient->getName() + "\", \"data\" : " + data + "}";

    if( send(sock , newData.c_str() , strlen( newData.c_str() ) , 0) < 0)
    {
        perror("Send failed : ");
        return false;
    }
    cout<<"Data send" << newData << "\n";

    return true;
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
#define BUFSIZE 1000
void
SocketClient::serve() {
    char buffer[BUFSIZE];
    bzero(buffer, BUFSIZE);
    int n;

    while ((n = SocketClient::readln( sock, buffer, BUFSIZE )) != 0)
    {
        {
            std::unique_lock<std::mutex> lock(m_mutex);


            /*
            Strip address:
            { "address" : "xyz.abc", "data" : {  ... data ...  };

            */
            string tmp(buffer);
            size_t colon = tmp.find(":");
            size_t start_address = tmp.find("\"", colon) + 1;
            size_t end_address = tmp.find("\"", start_address) - 1;
            size_t start_data = tmp.find("{", end_address);
            size_t end_data = tmp.rfind("}");
            if(start_data < end_data && end_data < BUFSIZE) {
                string address = tmp.substr(start_address, end_address - start_address + 1);

                string data = tmp.substr(start_data, end_data - start_data);

                try {
                    NodeRef* listener = m_clients.at(address);

                    listener->listen(data);

                } catch (std::out_of_range) {

                }

                //string* data = new string(tmp.substr(start_data, end_data - start_data));
                //m_stringQueue.push(std::shared_ptr<string>(data));
                //printf("address %s data %s\n", address.c_str(), data->c_str());

                printf("address %s data %s\n", address.c_str(), data.c_str());
            } else {
                printf("buffer %s\n", buffer);

            }

        }

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



