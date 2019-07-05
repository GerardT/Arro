#include <PythonGlue.h>  // include before anything else
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <limits.h>
#include <stdlib.h>
#include <sys/types.h> 
#include <sys/socket.h>
#include <netinet/in.h>
#include <thread>
#include <sys/stat.h>  /*for getting file size using stat()*/
#include <sys/sendfile.h>  /*for sendfile()*/
#include <fcntl.h>  /*for O_RDONLY*/
#include "ServerEngine.h"
#include "SocketClient.h"
#include "Trace.h"


// Debugger wants to have 'real' main.
extern "C" {
   int main();
}


#define ARRO_BUFFER_SIZE 250

using namespace std;
using namespace Arro;

static int newsockfd = -1;

Trace* pTrace;

static pid_t childPid;

void startChild() {

    char cwd[PATH_MAX];
    if (getcwd(cwd, sizeof(cwd)) != NULL) {
        printf("Current working dir: %s\n", cwd);
    } else {
        perror("getcwd() error");
        return;
    }
    std::string pathName(cwd);
    pathName += "/Core";

    childPid = fork(); /* Create a child process */

    switch (childPid) {
        case -1: /* Error */
            std::cerr << "fork() failed.\n";
            exit(1);
        case 0: /* Child process */
            execl(pathName.c_str(), "", 0); /* Execute the program */
            std::cerr << "execl() failed!"; /* execl doesn't return unless there's an error */
            exit(1);
        default: /* Parent process */
            std::cout << "Process created with pid " << childPid << "\n";
//            int status;
//
//            while (!WIFEXITED(status)) {
//                waitpid(pid, status, 0); /* Wait for the process to complete */
//            }
//
//            std::cout << "Process exited with " << WEXITSTATUS(status) << "\n";
    }
}

void killChild()
{
    kill(childPid,SIGKILL);
}




/**
 * Cleanup after exception of terminate command.
 */
static void cleanup()
{
    pTrace->println("Cleanup");

    /* close socket - probably already closed by Eclipse client */
    if(newsockfd != -1) {
        pTrace->println("-- socket");
        close(newsockfd);
        newsockfd = -1;
    }
    pTrace->println("Cleanup done");
}

/**
 * Wrapper to system function to get rid of compiler warning.
 *
 * @param command Command string to execute.
 * @return
 */
int syswrap(const string& command)
{
    int ret = system(command.c_str());
    if(ret != 0) {
        pTrace->fatal("system command failed");
    }
    return ret;
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
        pTrace->fatal("ERROR opening socket");
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
         pTrace->fatal("ERROR on binding");
    }

    //NodeTimer::init (); //FIXME: here?

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
        pTrace->println("new socket " + newsockfd);
        if (newsockfd < 0) 
        {
            newsockfd = -1;
            pTrace->fatal("ERROR on accept");
        }

        SendToConsole("========================");

        /* If connection is established then start communicating */
        bzero(buffer, ARRO_BUFFER_SIZE);
        while ((n = SocketClient::readln( newsockfd, buffer, ARRO_BUFFER_SIZE - 1 )) != 0)
        {
            pTrace->println(string("command: ") + buffer);

            sscanf(buffer, "%s", command);

            syswrap(string("mkdir -p ") + ARRO_FOLDER);
            syswrap(string("touch ") + ARRO_FOLDER + "/arro_api.py");

            if(!strcmp(command, "echo"))
            {
                pTrace->println(string("got line ") + buffer);
                SendToConsole("ServerEngine::console echo\n");
                break;
            }
            else if(!strcmp(command, "ls"))
            {
                syswrap("ls >temps.txt");
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
                string fullname(ARRO_FOLDER);
                fullname += filename;
                filehandle = open(fullname.c_str(), O_CREAT | O_TRUNC | O_WRONLY, 0666);
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
                    SendToConsole("put successful");
                }
                else
                {
                    SendToConsole("put failed");
                }
            }
            else if(!strcmp(command, "protobuf"))
            {
                std::string cmd;

                cmd= std::string("protoc --python_out=")+ARRO_FOLDER+" --proto_path="+ARRO_FOLDER+" "+ARRO_FOLDER+"arro.proto";
                syswrap(cmd);
                SendToConsole("protobuf successful");
                
                cmd = std::string("protoc --cpp_out=")+ARRO_FOLDER+" --proto_path="+ARRO_FOLDER+" "+ARRO_FOLDER+"arro.proto";
                syswrap(cmd);
                SendToConsole("protobuf successful");
            }
            else if(!strcmp(command, "build"))
            {
                std::string cmd;

                cmd = std::string("protoc --cpp_out=")+ARRO_FOLDER+" --proto_path="+ARRO_FOLDER+" "+ARRO_FOLDER+"arro.proto";
                syswrap(cmd);
                SendToConsole("protobuf successful");

                cmd = std::string("cp Nodes/* ")+ARRO_FOLDER;
                syswrap(cmd);
                SendToConsole("cp successful");

                cmd = std::string("make nodes");
                syswrap(cmd);
                SendToConsole("make successful");
            }
            else if(!strcmp(command, "run"))
            {
                // Run Core
                startChild();

                SendToConsole("run successful");
            }
            else if(!strcmp(command, "terminate"))
            {
                // kill Core
                cleanup();

                killChild();
                break;
            }
            else if(!strcmp(command, "pwd"))
            {
                syswrap("pwd>temp.txt");
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

void Arro::SendToConsole(const string& in)
{
    if(newsockfd >= 0) {
        std::string s = in + "\n";
        send(newsockfd, s.c_str(), s.length(), 0);
    }
}

int main() {
    pTrace = new Trace("Server", true);

    //SocketClient c{"localhost" , 9000};

    server();

    return 0;
}
