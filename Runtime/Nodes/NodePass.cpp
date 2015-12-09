#include <NodePass.h>

#include "../NodeDb.h"


NodePass::NodePass(string datatype, string name):
    trace(string("NodePass"), false) {
}

void NodePass::handleMessage(MessageBuf* m, string padName) {
	Value* msg = new Value();
	msg->ParseFromString(m);

    // do not put in queue but instead forward directly to target node.
    trace.println("NodePass forward ");
    result->forwardMessage(msg);
}

void NodePass::runCycle() {

}

