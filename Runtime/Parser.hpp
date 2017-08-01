//
//  parser.hpp
//  parser
//
//  Created by Gerard Teurlinx on 06/07/16.
//  Copyright © 2016 Gerard Teurlinx. All rights reserved.
//

#ifndef parser_hpp
#define parser_hpp

#include <stdio.h>
#include <map>
#include <set>
#include <list>
#include <string>
#include <functional>
#include "Tokenizer.hpp"
#include "Trace.h"

#if 0
typedef std::function<bool (const std::string& token)> tokenFunction;


/**
 * Instructions will be stored in a (history) list when parsing the
 * expression. Each instruction contains a parsing state and the token
 * that led to the next parsing state (even if keyword or e.g. bracket).
 */
class Instruction {
public:
    Instruction(int state1, std::string argument, int state2) {
        m_state1 = state1;
        m_state2 = state2;
        m_argument = argument;
    }
    std::string& getArgument() {
        return m_argument;
    }
    // Return true if transition from s1 to s2
    bool match(int s1, int s2) {
        return s1 == m_state1 && s2 == m_state2;
    }
    
private:
    int m_state1;
    int m_state2;
    std::string m_argument;
};

class State {
public:
    State(int initialState) {
        m_history.push_back(initialState);
    }
    void add(int newState) {
        m_history.push_back(newState);
    }
    int get() const {
        return m_history.back();
    }
    void collectInstructions(Tokenizer& tokens, std::list<Instruction>& instructions) {
        tokens.reset();
        
        auto c = m_history.begin();
        while(c != m_history.end()) {
            auto c1 = c;
            auto c2 = ++c;
            if(c != m_history.end()) {
                std::string tmp;
                tokens.getToken(tmp);
                instructions.push_back(Instruction(*c1, tmp, *c2));

                std::cout << "Rule " << *c1 << " - " << tmp << " - " << *c2 << "\n";
            }
        }
    }
    const std::list<int>& getHistory() {
        return m_history;
    }
    
    
private:
    std::list<int> m_history;
};

class Parser {

public:
    Parser(int startState, int endState);
    void addRule(int curState, char token, int newState, tokenFunction func);
    bool parse(Tokenizer& tokens, std::list<Instruction>& instructions);
    void runCode(Tokenizer& tokens);

private:
    Arro::Trace m_trace;
    // multimap allows multiple entries with same key value.
    // Below is a multimap of pair<state, token> --> newstate
    std::multimap<std::pair<int, char>, int> m_transitions;
    std::set<int> m_acceptingStates;
    State m_parsedState;
    int m_startState;
    std::map<std::pair<int, int>, tokenFunction> m_tokenFunctions;
};
#endif


#endif /* parser_hpp */
