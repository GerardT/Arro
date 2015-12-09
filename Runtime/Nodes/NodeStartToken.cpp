#include <NodeDb.h>
#include <NodeStartToken.h>



NodeStartToken::NodeStartToken(Process* d, string name, ConfigReader::StringMap& params):
    trace(string("NodeStartToken"), true),
	device(d) {

//	INode* n = NodeManager::getRef()->registerNode(name, NULL);

//    result = n->registerNodeOutput("Token", "token");
//    port_mode = n->registerNodeOutput("Mode", "mode");

    doIt();
}

void NodeStartToken::handleMessage(MessageBuf* msg, std::string padName) {

}

void NodeStartToken::runCycle() {

}



void NodeStartToken::doIt () {

    trace.println(string("NodeStartToken"));

    Mode* mode = new Mode();

    mode->set_mode("Active");

    device->getOutput("mode")->submitMessage(mode);

    Token* token = new Token();

    token->set_value(0);

    device->getOutput("token")->submitMessage(token);
}
