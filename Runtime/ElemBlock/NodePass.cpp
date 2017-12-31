#include "../ElemBlock/NodePass.h"

static RegisterMe<NodePass> registerMe("Pass");


NodePass::NodePass(string& datatype, string& name):
    trace("NodePass", false) {
}

void NodePass::handleMessage(MessageBuf* m, const string& padName) {
    Value* msg = new Value();
    msg->ParseFromString(m);

    // do not put in queue but instead forward directly to target node.
    trace.println("NodePass forward ");
    result->forwardMessage(msg);
}

void NodePass::runCycle() {
    // empty
}

