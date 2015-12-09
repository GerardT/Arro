#include <arro.pb.h>
#include <NodeDb.h>
#include <NodePid.h>


NodePid::NodePid(Process* d, string /*name*/, ConfigReader::StringMap params):
    trace(string("NodePid"), true),
	device(d) {

    previous_error = 0;
    integral = 0;
    derivative = 0;
    setpoint = 100;
    actual_position = 0;
    actual_mode = "Idle";
    ms_elapsed = 0;

    try {
        Kp = stod(params.at("Kp"));
        Ki = stod(params.at("Ki"));
        Kd = stod(params.at("Kd"));
    }
    catch (std::out_of_range) {
        trace.println("### param not found Kp, Ki, Kd ");
    }
}

void NodePid::handleMessage(MessageBuf* m, std::string padName) {
    //Class<?> cl = msg.getClass();
    //assert(msg->GetTypeName() == "tutorial.Value");

	if(padName == "actualValue") {
    	Value* msg = new Value();
    	msg->ParseFromString(m->c_str());

		assert(msg->GetTypeName() == "arro.Value");
        actual_position = ((Value*)msg)->value();
    } else if(padName == "targetValue") {
    	Value* msg = new Value();
    	msg->ParseFromString(m->c_str());

    	assert(msg->GetTypeName() == "arro.Value");
		setpoint = ((Value*)msg)->value();
    } else if(padName == "aTick") {
    	Tick* msg = new Tick();
    	msg->ParseFromString(m->c_str());

    	trace.println(string(msg->GetTypeName()));
		assert(msg->GetTypeName() == "arro.Tick");
		Tick* tick = (Tick*)msg;
		ms_elapsed = tick->ms();

    } else if (padName == "mode") {
    	Mode* msg = new Mode();
    	msg->ParseFromString(m->c_str());

    	assert(msg->GetTypeName() == "arro.Mode");
		actual_mode = ((Mode*)msg)->mode();
    } else {
        trace.println(string("Message received from ") + padName);
    }
}

void NodePid::runCycle() {

    trace.println(string("NodePid input = ") + to_string((long double)actual_position));

    if(true /*actual_mode == "Active"*/) {
        ms_elapsed /= 100;


        // calculate the difference between
        // the desired value and the actual value
        double error = setpoint - actual_position;
        // track error over time, scaled to the timer interval
        integral = integral + (error * ms_elapsed);
        // determine the amount of change from the last time checked
        derivative = (error - previous_error) / ms_elapsed;
        // calculate how much to drive the output in order to get to the
        // desired setpoint.
        double output = (Kp * error) + (Ki * integral) + (Kd * derivative);
        // remember the error for the next time around.
        previous_error = error;

        trace.println(string("NodePid output = ") + to_string((long double)output));

        Value* value = new Value();

        value->set_value(output);

        device->getOutput("output")->submitMessage(value);
    }
}
