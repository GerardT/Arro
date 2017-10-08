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
    virtual bool hasNode(const std::string& node) = 0;
    virtual bool hasStates(const std::string& node, const std::string& states) = 0;
    virtual bool nodeInState(const std::string& node, const std::string& state) = 0;
};

#endif
