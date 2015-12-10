
#include <ServerEngine.h>
#include <ConfigReader.h>

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h> 
#include <sys/socket.h>
#include <netinet/in.h>
#include <thread>


/*for getting file size using stat()*/
#include <sys/stat.h>

/*for sendfile()*/
#include <sys/sendfile.h>

/*for O_RDONLY*/
#include <fcntl.h>
#include <NodeDb.h>
#include <Nodes/NodeTimer.h>
#include "PythonGlue.h"

static std::thread* thrd;
static int newsockfd = -1;

/**
 * Blocking read from socket until '\n' received. If socket is closed
 * then 'terminate' is returned in string buffer.
 */
static int readln(int sockfd, char* buffer, size_t n/*size*/) {
    ssize_t numRead;                    /* # of bytes fetched by last read() */
    size_t totRead;                     /* Total bytes read so far */
    char *buf;
    char ch;

    if (n <= 0 || buffer == NULL) {
        errno = EINVAL;
        return -1;
    }

	buf = buffer;                       /* No pointer arithmetic on "void *" */

	totRead = 0;
	for (;;) {
		numRead = read(sockfd, &ch, 1);

		if (numRead == -1) {
			if (errno == EINTR)         /* Interrupted --> restart read() */
				continue;
			else {
				// Make loosing socket to terminate process.
				strcpy(buffer, "terminate");
				return strlen("terminate");
			}

		} else if (numRead == 0) {      /* EOF */
			if (totRead == 0) {          /* No bytes read; return 0 */
//				return 0;
				strcpy(buffer, "terminate");
				return strlen("terminate");
			}
			else                        /* Some bytes read; add '\0' */
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
    char command[256], filename[100];
    int size;
    int filehandle;
    char buffer[256];
    struct sockaddr_in serv_addr, cli_addr;
    int i, n;
    NodeDb* nodeDb = NULL;
    PythonGlue* pg = NULL;

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

    NodeTimer::init (); //FIXME: here?

    // Enter the listening loop. 
    while ( true )
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
            newsockfd = -1;
            exit(1);
        }

        ServerEngine::console("========================");

        /* If connection is established then start communicating */
        bzero(buffer,256);
        while ((n = readln( newsockfd,buffer,255 )) != 0)
        {
        	printf("command: %s\n", buffer);

            sscanf(buffer, "%s", command);

            if(!strcmp(command, "echo"))
            {
            	printf("got line %s\n", buffer);
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
                sendfile(newsockfd,filehandle,NULL,size);
            }
            else if(!strcmp(command,"get"))
            {
                sscanf(buffer, "%s%s", filename, filename);
                stat(filename, &obj);
                filehandle = open(filename, O_RDONLY);
                size = obj.st_size;
                if(filehandle == -1)
                    size = 0;
                send(newsockfd, &size, sizeof(int), 0);
                if(size)
                    sendfile(newsockfd, filehandle, NULL, size);

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
                		pg = new PythonGlue("arro");

                		string stringfilename(filename);
                        ConfigReader reader(stringfilename, *nodeDb);
                        ServerEngine::console("loading successful");

                        nodeDb->start();
                        ServerEngine::console("run successful");
                    } catch ( const std::runtime_error& e ) {
                         ServerEngine::console("run failed");
                    	// Nothing to do, just don't run it.
                    }

            	}
            }
            else if(!strcmp(command, "terminate"))
            {

            	/* 1: stop message flow */
            	nodeDb->stop();

            	/* 2: stop python */
            	if(pg) {
            		delete pg;
                	pg = NULL;
            	}

            	/* 3: delete node database */
            	delete nodeDb; // will automatically stop timers etc.
            	nodeDb = NULL;

            	/* 4: close socket - probably already closed by Eclipse client */
                //ServerEngine::console("stopped");
            	close(newsockfd);
            	break;
            }
            else if(!strcmp(command, "pwd"))
            {
                system("pwd>temp.txt");
                i = 0;
                FILE*f = fopen("temp.txt","r");
                while(!feof(f))
                    buffer[i++] = fgetc(f);
                buffer[i-1] = '\0';
                send(newsockfd, buffer, 100, 0);
            }
        }
    }
}

/**
 * Start server thread for Eclipse client.
 */
void ServerEngine::start()
{
    thrd = new std::thread(server);     // spawn new thread that calls server()

    std::cout << "server started...\n";
}

/**
 * Stop server thread for Eclipse client. Will never be called.
 */
void ServerEngine::stop()
{
  // synchronize threads:
    thrd->join();                // pauses until first finishes

    std::cout << "server completed.\n";
}

/**
 * Send text message to Eclipse client. Client will filter for these
 * message in order to determine success or failure of command sent.
 */
void ServerEngine::console(string s)
{
	if(newsockfd >= 0) {
		s += "\n";
	    send(newsockfd, s.c_str(), s.length(), 0);
	}
}



