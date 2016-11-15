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
public:
    Tokenizer(std::string i):
        m_input(i) {
        
        m_it = m_input.begin();
        m_itEnd = m_input.end();

    }
    void reset() {
        m_it = m_input.begin();
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
        char ret = '$';
        token = "";
        
        // skip spaces
        while(m_it != m_itEnd && (*m_it == ' ' || *m_it == '\t')) {
            ++m_it;
        }
        // first get tokens not necessarily separated by whitespace
        if(m_it != m_itEnd && *m_it == '(') {
            ret = '(';
            token = "(";
            ++m_it;
        }
        else if(m_it != m_itEnd && *m_it == ')') {
            ret = ')';
            token = ")";
            ++m_it;
        }
        else {
            // otherwise get token separated by whitespace
            while(m_it != m_itEnd && *m_it != ' ' && *m_it != '\t' && *m_it != '(' && *m_it != ')') {
                ret = 'n'; // name
                token += *m_it;
                ++m_it;
            }
        }
        
        return ret;
    }

private:
    std::string m_input;
    std::string::iterator m_it;
    std::string::iterator m_itEnd;

};

#endif /* Tokenizer_hpp */
