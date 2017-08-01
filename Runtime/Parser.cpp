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

typedef int node;  // is a NT

/// how to handle precedence
// Put all in a list,
// Reduce all AND pairs first
// Then calculate final result

typedef enum {
    V_FALSE = 0,
    V_TRUE,
    V_AND,
    V_OR
} Values;


class BtParser  // Recursive descent
{
public:
    class Context {
    public:
        std::string node;
        
        std::list<Values> m_result;
        
        Context* m_parent;
    private:
    public:
        Context(Context* parent):
        m_parent(parent) {
        }
        
        bool calcResult() {
            std::list<Values> newList;
            
            // First pass: remove ANDs
            for(auto res = m_result.begin(); res != m_result.end(); ++res) {
                if(*res == V_AND) {
                    ++res;
                    if(newList.back() == V_TRUE && *res == V_TRUE) {
                        newList.back() = V_TRUE;
                    } else {
                        newList.back() = V_FALSE;
                    }
                } else {
                    newList.push_back(*res);
                }
            }
            for(auto res = newList.begin(); res != newList.end(); ++res) {
                if(*res == V_TRUE) {
                    return true;
                }
            }

            return false;
        }
    };

public:
    BtParser(Tokenizer& t, std::list<Instruction>& i, ParserModel* pm):
        m_pm{pm},
        tokens{t},
        //instructions{i},
        m_context{nullptr}
    {
        m_sym = tokens.getToken();
        p_explist();
        
        bool result = run();
        
        std::cout << "the result is " << result << std::endl;
        
    }
    ~BtParser() {
    }
    
    bool run() {
        m_context = new Context(nullptr);
        for(auto ins : m_code) {
            ins();
        }
        bool result = m_context->calcResult();
        delete m_context;
        m_context = nullptr;
        return result;
    }
    
    
    Context* pushContext() {
        m_context = new Context(m_context);
        return m_context;
    }
    
    Context* popContext() {
        Context* tmp = m_context;
        m_context = m_context->m_parent;
        delete tmp;
        return m_context;
    }

    typedef char Symbol;
    
    void instr(tokenInstr func) {
        m_code.push_back(func);
        
    }

    void expect(Symbol s, const std::string& comment, std::string& tok) {
        if (s == 'n' && m_sym == 'n') {
            if(!m_pm->hasNode(tokens.getTokenValue())) {
                std::cout <<"Parsing condition failed, node not existing " << tokens.getTokenValue() << std::endl;
            }
            std::cout <<"token " << s << " - " << comment << std::endl;
            tok = tokens.getTokenValue();
            m_sym = tokens.getToken();
        }
        else if(s == 's' && m_sym == 'n') {
            if(!m_pm->hasState(m_context->node, tokens.getTokenValue())) {
                std::cout <<"Parsing condition failed, state not existing " << tokens.getTokenValue() << std::endl;
            }
            std::cout <<"token " << s << " - " << comment << std::endl;
            tok = tokens.getTokenValue();
            m_sym = tokens.getToken();
        } else if (m_sym == s) {
            std::cout <<"token " << s << " - " << comment << std::endl;
            tok = tokens.getTokenValue();
            m_sym = tokens.getToken();
        } else {
            std::cout <<"Parsing condition failed, expected " << s << std::endl;
        }
    }
    
    bool accept(Symbol s, const std::string& comment, std::string& tok) {
        if(s == 'n' && m_sym == 'n') {
            if(!m_pm->hasNode(tokens.getTokenValue())) {
                std::cout <<"Parsing condition failed, node not existing " << tokens.getTokenValue() << std::endl;
            }
            std::cout <<"token " << s << " - " << comment << std::endl;
            tok = tokens.getTokenValue();
            m_sym = tokens.getToken();
            return true;
        } else if(s == 's' && m_sym == 'n') {
            if(!m_pm->hasState(m_context->node, tokens.getTokenValue())) {
                std::cout <<"Parsing condition failed, state not existing " << tokens.getTokenValue() << std::endl;
            }
            std::cout <<"token " << s << " - " << comment << std::endl;
            tok = tokens.getTokenValue();
            m_sym = tokens.getToken();
            return true;
        } else if (m_sym == s) {
            std::cout <<"token " << s << " - " << comment << std::endl;
            tok = tokens.getTokenValue();
            m_sym = tokens.getToken();
            return true;
        }
        return false;
    }
    
    // a AND b AND c OR d AND e: store them in array and do 2 passes for ands and for ors.
    // keep 3 values: result, operand1 and operand2
    
    void p_explist() {
        std::string s; // need to have in local scope for lambdas
        // full = condition { ("and" | "or") condition } $
        p_exp();
        while (accept('a', "AND", s) || accept('o', "OR", s)) {
            if(s == "AND") {
                instr([this](){
                    m_context->m_result.push_back(V_AND);
                });
            } else { // "OR"
                instr([this](){
                    m_context->m_result.push_back(V_OR);
                });
            }
            p_exp();
        }

        if(accept('$', "$", s)) {
            std::cout <<"Parsing condition succeeded" << std::endl;
        }
    }

    void p_exp() {
        std::string s; // need to have in local scope for lambdas
        // condition = node "in" "(" state ")"
        //           | "(" full ")"
        if (accept('(', "start group", s))
        {
            instr([this](){
                pushContext();  // save context
            });
            
            p_explist();
            expect(')', "end group", s);
            instr([this](){
                // here we somehow need to manage AND precedence over OR
                bool result = m_context->calcResult();
                popContext();  // restore old context
                
                m_context->m_result.push_back(result == true ? V_TRUE : V_FALSE);
            });
        } else if (accept('n', "node", s)) {
            instr([s,this](){
                pushContext();  // save context
                m_context->node = s;
            });
            expect('i', "IN", s);
            expect('(', "start state list", s);
            expect('s', "first state", s);
            instr([s,this](){
                m_context->m_result.push_back(m_pm->nodeAtStep(m_context->node, s) == true ? V_TRUE : V_FALSE);
            });
            while (accept(',', "", s)) {
                expect('s', "next state", s);
                instr([s,this](){
                    m_context->m_result.push_back(V_OR);
                    m_context->m_result.push_back(m_pm->nodeAtStep(m_context->node, s) == true ? V_TRUE : V_FALSE);
                    });
            }
            expect(')', "end state list", s);
            instr([s,this](){
                // here we somehow need to manage AND precedence over OR
                bool result = m_context->calcResult();
                popContext();  // restore old context
                
                m_context->m_result.push_back(result == true ? V_TRUE : V_FALSE);
                });
        } else {
            std::cout <<"Parsing condition failed, expected " << "node, '(' or','" << std::endl;
        }
    }
private:
    Tokenizer& tokens;
    
    Context* m_context;
    Symbol m_sym;
    std::vector<tokenInstr> m_code;
    ParserModel* m_pm;
    
};



#if 0
Parser::Parser(int startState, int endState):
    m_trace{"Parser", true},
    m_parsedState{startState},
    m_startState{startState} {
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
#endif


