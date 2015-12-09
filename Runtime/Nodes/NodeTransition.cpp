#include <NodeDb.h>
#include <NodeTransition.h>


NodeTransition::NodeTransition(Process* d, string name, ConfigReader::StringMap params):
    trace(string("NodeTransition"), true),
	device(d) {


    hasToken = false;

   // INode* n = NodeManager::getRef()->registerNode(this, name, NULL);


    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class AnonValue: public IPadListener {
        NodeTransition* owner;
    public:
        AnonValue(NodeTransition* n){owner = n; };

        void handleMessage(MessageBuf* m, string padName) {
        	Value* msg = new Value();
        	msg->ParseFromString(m->c_str());

        	//Class<?> cl = msg.getClass();
            assert(msg->GetTypeName() == "tutorial.Value");

            owner->value = ((Value*)msg)->value();

            if(owner->value > 99.90 && owner->value < 100.10 && owner->hasToken) {
                owner->doIt();
            }
        }
    };
    //n->registerNodeInput("Value", "value", new AnonValue(this));

    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class AnonToken: public IPadListener {
        NodeTransition* owner;
    public:
        AnonToken(NodeTransition* n){owner = n; };

        void handleMessage(MessageBuf* m, string padName) {
        	Token* msg = new Token();
        	msg->ParseFromString(m->c_str());

            //Class<?> cl = msg.getClass();
            assert(msg->GetTypeName() == "tutorial.Token");

            owner->hasToken = true;

            if(owner->value > 99.90 && owner->value < 100.10) {
                owner->doIt();
            }
        }
    };
    //n->registerNodeInput("Token", "token", new AnonToken(this));

    //result = n->registerNodeOutput("Value", "result");
    //p_mode = n->registerNodeOutput("Mode", "mode");
}

void NodeTransition::handleMessage(MessageBuf* msg, std::string padName) {

}

void NodeTransition::runCycle() {

}


void NodeTransition::doIt () {

    trace.println(string("NodeTransition"));

    Mode* mode = new Mode();

    mode->set_mode("Idle");

    p_mode->submitMessage(mode);

    Token* token = new Token();

    token->set_value(0);

    result->submitMessage(token);

}

