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

#include "ServerEngine.h"
#include "ConfigReader.h"
#include "NodeDb.h"
#include "NodeTimer.h"
#include "PythonGlue.h"

#define ARRO_BUFFER_SIZE 250

using namespace std;
using namespace Arro;

static thread* thrd = nullptr;
static int newsockfd = -1;
static NodeDb* nodeDb = nullptr;
static PythonGlue* pg = nullptr;
static Trace trace("ServerEngine", true);

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
static int readln(int sockfd, char* buffer, size_t n/*size*/) {
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
            	trace.println("ServerEngine losing socket, terminated!");
                strcpy(buffer, "terminate");
                return strlen("terminate");
            }

        } else if (numRead == 0) {      /* EOF */
            if (totRead == 0) {         /* No bytes read */
            	trace.println("ServerEngine terminated!\n");
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

/**
 * Cleanup after exception of terminate command.
 */
static void cleanup()
{
	trace.println("Cleanup");
	if(nodeDb) {
        /* 1: stop message flow */
		trace.println("-- nodeDb");
        nodeDb->stop();

        /* 2: delete node database */
        delete nodeDb; // will automatically stop timers etc.
        nodeDb = nullptr;
	}

    /* 3: stop python */
    if(pg) {
		trace.println("-- PythonGlue");
        delete pg;
        pg = nullptr;
    }

    /* 4: close socket - probably already closed by Eclipse client */
    if(newsockfd != -1) {
		trace.println("-- socket");
        close(newsockfd);
        newsockfd = -1;
    }
}

/**
 * Server thread for Eclipse client.
 * Should keep running forever and serve multiple client 'debugging' sessions.
 * For duration of each 'debugging' session from client the server will keep
 * open an IP socket connection. A 'debugging' session closes by sending 'terminate'
 * command.
 */
static void server()
{
    int sockfd, portno;
    socklen_t clilen;
    struct stat obj;
    char command[ARRO_BUFFER_SIZE], filename[ARRO_BUFFER_SIZE];
    int size;
    int filehandle;
    char buffer[ARRO_BUFFER_SIZE];
    struct sockaddr_in serv_addr, cli_addr;
    int i, n;

    /* First call to socket() function */
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) 
    {
        trace.fatal("ERROR opening socket");
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
         trace.fatal("ERROR on binding");
    }

    NodeTimer::init (); //FIXME: here?

    // Enter the listening loop. 
    while ( true )
    {
        /* Now start listening for the clients, here process will
         * go in sleep mode and will wait for the incoming connection
         */
        listen(sockfd,5);
        clilen = sizeof(cli_addr);

        /* Accept actual connection from the client, we only accept one connection. */
        newsockfd = accept(sockfd, (struct sockaddr *)&cli_addr, &clilen);
        trace.println("new socket " + newsockfd);
        if (newsockfd < 0) 
        {
            newsockfd = -1;
            trace.fatal("ERROR on accept");
        }

        ServerEngine::console("========================");

        /* If connection is established then start communicating */
        bzero(buffer,ARRO_BUFFER_SIZE);
        while ((n = readln( newsockfd,buffer, ARRO_BUFFER_SIZE - 1 )) != 0)
        {
            trace.println(string("command: ") + buffer);

            sscanf(buffer, "%s", command);

            if(!strcmp(command, "echo"))
            {
                trace.println(string("got line ") + buffer);
                ServerEngine::console("ServerEngine::console echo\n");
                break;
            }
            else if(!strcmp(command, "ls"))
            {
                system("ls >temps.txt");
                i = 0;
                stat("temps.txt",&obj);
                size = obj.st_size;
                send(newsockfd, &size, sizeof(int),0);
                filehandle = open("temps.txt", O_RDONLY);
                sendfile(newsockfd,filehandle,nullptr,size);
            }
            else if(!strcmp(command, "put"))
            {
                int c = 0;
                char *f;
                sscanf(buffer+strlen(command), "%s %d", filename, &size);
                filehandle = open(filename, O_CREAT | O_TRUNC | O_WRONLY, 0666);
                f = (char*)malloc(size);
                while(size > 0)
                {
                    int r = recv(newsockfd, f, size, 0);
                    c = write(filehandle, f, r);
                    size -= r;
                }
                close(filehandle);
                free(f);
                if(c != -1)
                {
                    ServerEngine::console("put successful");
                }
                else
                {
                    ServerEngine::console("put failed");
                }
            }
            else if(!strcmp(command, "protobuf"))
            {
                system("protoc --python_out=. arro.proto");
                ServerEngine::console("protobuf successful");
            }
            else if(!strcmp(command, "run"))
            {
                if(nodeDb)
                {
                    ServerEngine::console("run failed, engine running, terminate first");
                }
                else
                {
                    nodeDb = new NodeDb();
                    try {
                        pg = new PythonGlue();

                        ConfigReader reader(ARRO_CONFIG_FILE, *nodeDb);
                        ServerEngine::console("loading successful");

                        nodeDb->start();
                        ServerEngine::console("run successful");
                    } catch ( const std::runtime_error& e ) {
                    	trace.println("Instantiating PythonGlue failed");
                    	cleanup();

                        ServerEngine::console("run failed");
                    }
                }
            }
            else if(!strcmp(command, "terminate"))
            {
            	cleanup();
                break;
            }
            else if(!strcmp(command, "pwd"))
            {
                system("pwd>temp.txt");
                i = 0;
                FILE*f = fopen("temp.txt","r");
                while(!feof(f) && i < ARRO_BUFFER_SIZE)
                    buffer[i++] = fgetc(f);
                buffer[i-1] = '\0';
                send(newsockfd, buffer, 100, 0);
            }
        }
    }
}

void ServerEngine::start()
{
    if(!thrd) {
        thrd = new std::thread(server);     // spawn new thread that calls server()
    }
    else {
        trace.fatal("Thread already present");
    }

    trace.println("server started...");
}

void ServerEngine::stop()
{
    // synchronize threads:
    thrd->join();                // pauses until finishes

    delete thrd;

    trace.println("server completed.");
}

void ServerEngine::console(string s)
{
    if(newsockfd >= 0) {
        s += "\n";
        send(newsockfd, s.c_str(), s.length(), 0);
    }
}



