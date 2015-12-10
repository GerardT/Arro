#ifndef SERVER_ENGINE_H
#define SERVER_ENGINE_H

#include <string>

class ServerEngine {

public:
	static void start();
	static void stop();
	static void console(std::string s);
};

/**
 * Little utility class.
 */
class StringRef {
private:
	std::string s;
public:
	StringRef(const char* str):
		s(str){};

	operator std::string&() {
		return s;
	}
};

#endif
