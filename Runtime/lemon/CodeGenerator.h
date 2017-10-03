#ifndef codegenerator_h
#define codegenerator_h

#include <string>

class CodeGenerator;

class CodeGenInterface
{
public:
    CodeGenInterface(const std::string& expression);
    virtual bool parse();
    virtual bool runCode();
    virtual ~CodeGenInterface();
    virtual bool hasNode(const char* node);
    virtual bool hasState(const char* state);
    virtual bool nodeInState(const char* node, const char* state);
private:
    CodeGenerator* m_cg;
};

#endif
