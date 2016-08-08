//
//  Tokenizer.hpp
//  parser
//
//

#ifndef Tokenizer_hpp
#define Tokenizer_hpp

#include <stdio.h>
#include <string>
#include <iostream>
#include <functional>

class Tokenizer {
private:
    std::string input;
    std::string::iterator it;
    std::string::iterator itEnd;
    
public:
    Tokenizer(std::string i):
        input(i) {
        
        it = input.begin();
        itEnd = input.end();

    }
    void reset() {
        it = input.begin();
    }
    
    char getToken(std::string& token) {

        char ret = getSubstring(token);

        // if name then make more specific
        if(ret == 'n') {
            if(token == "IN") {
                ret = 'i';
            }
            else if(token == "AND") {
                ret = 'a';
            }
            else if(token == "OR") {
                ret = 'o';
            }
        }
        
        // std::cout <<  "Token " + token + " ret " + ret + "\n";
        return ret;
    }
    
    // (key) words must be separated by whitespace.
    char getSubstring(std::string& token) {
        char ret = '\0';
        token = "";
        
        // skip spaces
        while(it != itEnd && (*it == ' ' || *it == '\t')) {
            ++it;
        }
        // first get tokens not necessarily separated by whitespace
        if(it != itEnd && *it == '(') {
            ret = '(';
            token = "(";
            ++it;
        }
        else if(it != itEnd && *it == ')') {
            ret = ')';
            token = ")";
            ++it;
        }
        else {
            // otherwise get token separated by whitespace
            while(it != itEnd && *it != ' ' && *it != '\t' && *it != '(' && *it != ')') {
                ret = 'n'; // name
                token += *it;
                ++it;
            }
        }
        
        return ret;
    }

};

#endif /* Tokenizer_hpp */
