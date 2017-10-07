#ifndef codegeninterface_h
#define codegeninterface_h

#include <string>

class CodeGenerator;

class CodeGenInterface
{
public:
    static CodeGenerator* getInstance(const std::string& expression, CodeGenInterface* caller);
    static void delInstance(CodeGenerator* instance);
    static bool run(CodeGenerator *cg);
    virtual bool hasNode(const char* node) = 0;
    virtual bool hasState(const char* node, const char* state) = 0;
    virtual bool nodeInState(const char* node, const char* state) = 0;
};

#endif
