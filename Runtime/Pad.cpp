#include "NodeDb.h"
#include "Pad.h"


using namespace std;
using namespace Arro;


/**
 * Pad, like Process, implements interface AbstractNode.
 * Pad is for non-functional connection pads.
 * Basically it just installs a listener that forwards incoming messages.
 * TODO: maybe we could remove Pads and connect Process outputs directly to Process inputs.
 */
Pad::Pad(NodeDb& nodeDb, const string& /*datatype*/, const string& name):
    AbstractNode{name},
    trace{"Pad", false},
    result{nullptr},
    in{nullptr},
    out{nullptr} {

    nodeDb.registerNode(this, name);

    in = nodeDb.registerNodeInput(this, "", [this](MessageBuf* msg) {
        // do not put in queue but instead forward directly to target node.
        trace.println("Pad forward ");
        out->forwardMessage(msg);
    });
    out = nodeDb.registerNodeOutput(this, "");
}
