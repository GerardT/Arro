#include "CodeGenerator.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <vector>
#include "cfg.h"
#include <stdio.h>
#include <string>
#include <iostream>
#include <functional>



/* lemon token struct */
struct Token {
    const char* z;
    int value;
};



/**
 * Class for cutting input text into tokens.
 */
class Tokenizer {
public:
    Tokenizer(std::string i):
    m_input(i),
    m_currentToken(0) {
        
        m_it = m_input.begin();
        m_itEnd = m_input.end();
        
    }
    void printError() {
        printf("Error at %s in %s\n", m_currentName.c_str(), m_input.c_str());
    }
    
    void printFailure() {
        printf("Failure parsing %s\n", m_input.c_str());
    }
    
    // Return one of TOK_ token types and token itself as parameter.
    int getToken(std::string& token) {
        
        int ret = getSubstring(token);
        
        // if name then make more specific
        if(ret == TOK_NAME) {
            if(token == "IN") {
                ret = TOK_IN;
            }
            else if(token == "AND") {
                ret = TOK_AND;
            }
            else if(token == "OR") {
                ret = TOK_OR;
            }
        }
        m_currentToken = ret;
        m_currentName = token;
        
        //printf("Token %s ret %i\n", token.c_str(), ret);
        return ret;
    }
private:
    // (key) words must be separated by whitespace or single-character symbols.
    int getSubstring(std::string& token) {
        int ret = -1;
        token = "";
        
        // skip spaces
        while(m_it != m_itEnd && (*m_it == ' ' || *m_it == '\t')) {
            ++m_it;
        }
        // first get tokens not necessarily separated by whitespace
        if(m_it != m_itEnd && *m_it == '(') {
            ret = TOK_LPAREN;
            token = "(";
            ++m_it;
        }
        else if(m_it != m_itEnd && *m_it == ')') {
            ret = TOK_RPAREN;
            token = ")";
            ++m_it;
        }
        else if(m_it != m_itEnd && *m_it == ',') {
            ret = TOK_COMMA;
            token = ",";
            ++m_it;
        }
        else {
            // otherwise get token separated by whitespace
            while(m_it != m_itEnd && *m_it != ' ' && *m_it != '\t' && *m_it != '(' && *m_it != ')'  && *m_it != ',') {
                ret = TOK_NAME; // name
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
    int m_currentToken;
    std::string m_currentName;

    
};

// Lambda function declaration. Lambda is used as container for 'compiled' code.
typedef std::function<bool (const char*)> tokenInstr;

/**
 * Class implements a method for each of the production rules of the language.
 * Lemon will call each of those methods with the right parameters. Those parameters
 * are stored in the lambdas by capture.
 * When running the code (the 'compiled' code), the lambda functions are called using
 * the stored captures.
 */
class CodeGenerator {
public:
    // Constructor - sets up and runs Lemon in order to generate a list of
    // lambda functions that serve as 'compiled' code.
    CodeGenerator(const std::string& expression, CodeGenInterface* cg):
        m_t{expression},
        m_cg{cg} {};

    bool parse();
    
    int storeInstr(tokenInstr func) {
        m_code.push_back(func);
        int nr = m_code.end() - m_code.begin() - 1;
        return nr;
    }
    
    //    line(L) ::= expr(A).
    int rule1(Token& A) {
        return storeInstr([this,A](const char*){
            return m_code[A.value](NULL);
        });
    }
    //    expr(L) ::= expr(A) AND expr(B).
    int rule2(Token& A, Token& B) {
        return storeInstr([this,A,B](const char*){
            return m_code[A.value](NULL) && m_code[B.value](NULL);
        });
    }
    //    expr(L) ::= expr(A) OR expr(B).
    int rule3(Token& A, Token& B) {
        return storeInstr([this,A,B](const char*){
            return m_code[A.value](NULL) || m_code[B.value](NULL);
        });
    }
    //    expr(L) ::= NAME(A) IN LPAREN states(B) RPAREN.
    int rule5(Token& A, Token& B) {
        if(!m_cg->hasNode(A.z)) {
            printError();
        }
        return storeInstr([this, A, B](const char*){
            // check if this node in m_states;
            return m_code[B.value](A.z);
        });
    }
    //    states(L) ::= states(A) COMMA NAME(B).
    int rule6(Token& A, Token& B) {
        if(!m_cg->hasState(B.z)) {
            printError();
        }
        return storeInstr([this, A, B](const char* node){
            // Build list of state names
            return m_cg->nodeInState(node, B.z) || m_code[A.value](node);
        });
    }
    //    states(L) ::= NAME(A).
    int rule7(Token& A) {
        if(!m_cg->hasState(A.z)) {
            printError();
        }
        return storeInstr([this, A](const char* node){
            return m_cg->nodeInState(node, A.z);
        });
    }

    void printError() {
        m_t.printError();
    }
    
    void printFailure() {
        m_t.printFailure();
    }
    
    bool run() {
        auto it = m_code.end();
        --it;
        bool result = (*it)(NULL);
        
        printf("=== Result %d\n", result);

        return result;
    }
private:
    Tokenizer m_t;
    std::vector<tokenInstr> m_code;
    std::string N_states;
    std::string N_expr;
    std::string N_exprs;
    std::string N_line;
    CodeGenInterface* m_cg;
};

#include "cfg.c"


bool
CodeGenerator::parse() {
    char tmp[100][100];
    int tok;
    int i = 0;
    Token t;
    
    std::string symbol;
    
    void* pParser = ParseAlloc( malloc );
    while( (tok = m_t.getToken(symbol)) != -1 ){
        strncpy(tmp[i], symbol.c_str(), 99);
        t.z = (tmp[i]);
        
        Parse(pParser, tok, t, this);
        i++;
    }
    Parse(pParser, 0, t, this);
    ParseFree(pParser, free );

    return true;
};


CodeGenInterface::CodeGenInterface(const std::string& expression) {
    m_cg = new CodeGenerator(expression, this);
}
CodeGenInterface::~CodeGenInterface() {
    delete m_cg;
}


// ============== test below this point ==============

bool CodeGenInterface::hasNode(const char* /*node*/) {
    return true;
};
bool CodeGenInterface::hasState(const char* /*state*/) {
    return true;
};
bool CodeGenInterface::nodeInState(const char* node, const char* state) {
    if(node[0] == 'd' && state[0] == 'e') return true;
    return false;
};
bool CodeGenInterface::parse() {
    return m_cg->parse();
}

bool CodeGenInterface::runCode() {
    return m_cg->run();
}


/*

int main(int argc, char *argv[]) {
    std::string exp("a IN (b, c) AND d IN (e)");
    CodeGenerator cg(exp);
    
    cg.run();
}
*/
