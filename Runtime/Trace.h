#ifndef TRACE_H
#define TRACE_H

#include <iostream>
#include <streambuf>
#include <string>
#include <tinyxml.h>


/**
 * Support for debug traces. One instance to be added to each class.
 * Trace class will add the class name to each line that is printed.
 */
class Trace {
    bool out;
    std::string cl;
public:
    Trace(std::string c, bool o) {
		cl = c;
		out = o;
	}
    void println(std::string s) {
        if(out == true) {
            std::cout << cl << "==>" << s << std::endl;
        }
    }
    void fatal(std::string s) {
        std::cout << cl << "==>" << s << std::endl;
        exit(0);
    }
    void println(std::string s, TiXmlElement* node) {
        if(out == true) {
            std::cout << cl << "==>" << s << node << std::endl;
        }
    }

};
/*
std::stringstream ss;

  ss << 100 << ' ' << 200;

  int foo,bar;
  ss >> foo >> bar;

  std::cout << "foo: " << foo << '\n';
  std::cout << "bar: " << bar << '\n';
 */
#endif
