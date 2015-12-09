#ifndef SERVER_ENGINE_H
#define SERVER_ENGINE_H

#include <string>

class ServerEngine {

public:
	static void start();
	static void stop();
	static void console(std::string s);
};

#endif
