#include <stdio.h>
#include <sys/types.h> 
#include <sys/socket.h>
#include <netinet/in.h>
#include <NodeDb.h>

static bool running = false;


//
//static bool getXml(string& nodename, string& nodevalue, string& xml)
//{
//    std::cout << nodename << "==>" << nodevalue << std::endl;
//
//    // for this node, node and input have same name...
//    //NodeIf* n = nodeDb->getNode("/main/sample/m3/ui_test");
//    INode* n = nodeDb->getNode(nodename);
//    if(n) {
//        n->update(nodevalue, xml);
//        return true;
//    }
//    else {
//        return false;
//    }
//}
//
static int readln(int sockfd, char* buffer, int /*size*/) {
	int index = 0;
	int n;

    while ((n = read(sockfd, buffer + index, 255 - index )) != 0) {
    	for(int x = 0; x < index + n; x++) {
    		printf("char %c\n", buffer[x]);
    		if(buffer[x] == '\n') {
    			return x;
    		}
    	}
    	index += n;
    }
    return -1;

}
static void server()
{
    int sockfd, newsockfd, portno;
    socklen_t clilen;
    char buffer[256];
    struct sockaddr_in serv_addr, cli_addr;
    int  n;

    /* First call to socket() function */
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) 
    {
        perror("ERROR opening socket");
        exit(1);
    }
    /* Initialize socket structure */
    bzero((char *) &serv_addr, sizeof(serv_addr));
    portno = 13000;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(portno);
 
    /* Now bind the host address using bind() call.*/
    if (::bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0)
    {
         perror("ERROR on binding");
         exit(1);
    }
    // Enter the listening loop. 
    while ( running )
    {
        /* Now start listening for the clients, here process will
        * go in sleep mode and will wait for the incoming connection
        */
        listen(sockfd,5);
        clilen = sizeof(cli_addr);

        /* Accept actual connection from the client */
        newsockfd = accept(sockfd, (struct sockaddr *)&cli_addr, &clilen);
		printf("new socket %d\n", newsockfd);
        if (newsockfd < 0) 
        {
            perror("ERROR on accept");
            exit(1);
        }
        /* If connection is established then start communicating */
        bzero(buffer,256);
        while ((n = readln( newsockfd,buffer,255 )) != 0)
        {
        	string* line = new string(buffer);

        	/*int x = */line->find("echo");

    		printf("got line %s\n", buffer);
            n = write(newsockfd, buffer, 6);
    		break;
            bzero(buffer,256);
        }
    }
}


//void test()
//{
//
//    // create the socket
//    Socket^ listenSocket = gcnew Socket( AddressFamily::InterNetwork,
//        SocketType::Stream,
//        ProtocolType::Tcp );
//
//    // bind the listening socket to the port
//    IPAddress^ hostIP = ( Dns::Resolve( IPAddress::Any->ToString() ) )->AddressList[ 0 ];
//    IPEndPoint^ ep = gcnew IPEndPoint( hostIP,port );
//    listenSocket->Bind( ep );
//
//    // start listening
//    listenSocket->Listen( backlog );
//}


#include <thread>
static std::thread* first;

void start_server()
{
	running = true;
    first = new std::thread(server);     // spawn new thread that calls server()

    std::cout << "server started...\n";
}

void stop_server()
{
    // synchronize threads:
	running = false;
    first->join();                // pauses until first finishes

    std::cout << "server completed.\n";
}

