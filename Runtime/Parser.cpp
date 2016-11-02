//
//  parser.cpp
//  parser
//
//  Created by Gerard Teurlinx on 06/07/16.
//  Copyright © 2016 Gerard Teurlinx. All rights reserved.
//

#include "Parser.hpp"

#include "Tokenizer.hpp"
#include <map>
#include <set>
#include <string>

using namespace std;

Parser::Parser(int startState, int endState):
    m_trace("Parser", true),
    m_parsedState(startState) {
    m_startState = startState;
    m_acceptingStates.insert(endState);
}
void Parser::addRule(int curState, char token, int newState, tokenFunction func) {
    m_transitions.insert(std::make_pair(make_pair(curState, token), newState));
    m_tokenFunctions.insert(std::make_pair(make_pair(curState, newState), func));
}

bool Parser::parse(Tokenizer& tokens, list<Instruction>& instructions) {
    /* Track our set of states.  We begin in the start state. */
    list<State> currStates;
    currStates.push_back(State(m_startState));
    string tokenString;
    char token;
    
    /*while((token = tokens.getToken(tokenString)) != '$')*/ do {
        token = tokens.getToken(tokenString);

        list<State> nextStates;
        
        for(auto state = currStates.begin(); state != currStates.end(); ++state) {
            
            int lastState = state->get();
            
            /* Get all states that we transition to from this current state. */
            pair<multimap<pair<int, char>, int>::iterator,
                 multimap<pair<int, char>, int>::iterator>
            transitionsIt = m_transitions.equal_range(make_pair(lastState, token));
            
            /* Add these new states to the nextStates set. */
            for(; transitionsIt.first != transitionsIt.second; ++transitionsIt.first)
            {
                /* transitions.first is the current iterator, and its second
                 * field is the value (new state) in the STL multimap.
                 */
                
                State current = *state;
            
                current.add(transitionsIt.first->second);

                nextStates.push_back(current);
            
                // if no insertion for this state then remove it.
            }
        }
        
        currStates = nextStates;
        //for(list<State>::iterator itr = currStates.begin(); itr != currStates.end(); ++itr) {
        //    std::cout << "Current state " << itr->get() << "\n";
        //}
    } while(token != '$');
    

    // Find State in currStates that is in m_acceptingStates
    for(list<State>::iterator itr = currStates.begin(); itr != currStates.end(); ++itr) {
        if(m_acceptingStates.count(itr->get())) {
            m_parsedState = *itr;
            itr->collectInstructions(tokens, instructions);
            
            return true;
        }
    }
    
    return false;
}

void Parser::runCode(Tokenizer& tokens) {
    tokens.reset();
    bool cont = true;

    m_trace.println("Checking transitions ");
    auto history = m_parsedState.getHistory();

    auto c = history.begin();
    while(c != history.end() && cont) {
        auto c1 = c;
        auto c2 = ++c;
        if(c != history.end()) {
            std::string tmp;
            tokens.getToken(tmp);

            try {
                tokenFunction func = m_tokenFunctions.at(make_pair(*c1, *c2));

                std::cout << "Rule " << *c1 << " - " << tmp << " - " << *c2 << "\n";

                cont = func(tmp);
            }
            catch (std::out_of_range) {
                throw std::runtime_error("Running instruction fails " + tmp);
            }
        }
    }

}



