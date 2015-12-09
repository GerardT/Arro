#include <NodeDb.h>
#include <NodeLinear.h>


NodeLinear::NodeLinear(Process* d, string name, ConfigReader::StringMap params):
    trace(string("NodeLinear"), true),
	device(d) {

    previous_position = 0;
	setpoint = 100;
    actual_mode = "Idle";

    try {
        Inc = stod(params.at("Inc"));
    }
    catch (std::out_of_range) {
        trace.println("### param not found Inc ");
    }

    //INode* n = NodeManager::getRef()->registerNode(this, name, NULL);

    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class AnonSp: public IPadListener {
        NodeLinear* owner;
    public:
        AnonSp(NodeLinear* n){owner = n; };

        void handleMessage(MessageBuf* m, string padName) {
        	Value* msg = new Value();
        	msg->ParseFromString(m->c_str());

            //Class<?> cl = msg.getClass();
            assert(msg->GetTypeName() == "tutorial.Value");

            owner->setpoint = ((Value*)msg)->value();
        }
    };

    //n->registerNodeInput("Value", "sp", new AnonSp(this));

    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class AnonTick: public IPadListener {
        NodeLinear* owner;
    public:
        AnonTick(NodeLinear* n){owner = n; };

        void handleMessage(MessageBuf* m, string padName) {
        	Tick* msg = new Tick();
        	msg->ParseFromString(m->c_str());

        	//Class<?> cl = msg.getClass();
            assert(msg->GetTypeName() == "tutorial.Tick");

            Tick* tick = (Tick*)msg;

            owner->doIt(tick->ms());
        }
    };

    //n->registerNodeInput("Tick", "timer", new AnonTick(this));

    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class AnonMode: public IPadListener {
        NodeLinear* owner;
    public:
        AnonMode(NodeLinear* n){owner = n; };

        void handleMessage(MessageBuf* m, string padName) {
        	Mode* msg = new Mode();
        	msg->ParseFromString(m->c_str());

        	//Class<?> cl = msg.getClass();
            assert(msg->GetTypeName() == "tutorial.Mode");

            owner->actual_mode = ((Mode*)msg)->mode();

            owner->trace.println("NodeLinear changed mode to " + owner->actual_mode);
        }
    };
    //n->registerNodeInput("Mode", "mode", new AnonMode(this));
    //result = n->registerNodeOutput("Value", "result");
}

void NodeLinear::handleMessage(MessageBuf* m, string padName) {
	Value* msg = new Value();
	msg->ParseFromString(m->c_str());

    //Class<?> cl = msg.getClass();
    assert(msg->GetTypeName() == "tutorial.Value");

    setpoint = ((Value*)msg)->value();

}

void NodeLinear::runCycle() {

}



void NodeLinear::doIt (int ms_elapsed) {
		
    trace.println(string("NodeLinear input = ") + to_string((long double)setpoint));

    if(actual_mode == "Active") {
        ms_elapsed /= 100;

		if(setpoint > previous_position && setpoint - previous_position > Inc) {
			previous_position += Inc;

			trace.println(string("NodeLinear output = ") + to_string((long double)previous_position));

			Value* value = new Value();

			value->set_value(previous_position);

			result->submitMessage(value);
		}
		else if (previous_position - setpoint > Inc){
		    previous_position -= Inc;
			
			trace.println(string("NodeLinear output = ") + to_string((long double)previous_position));

			Value* value = new Value();

			value->set_value(previous_position);

			result->submitMessage(value);
		}
    }

}

