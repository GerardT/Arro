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

    AbstractNode* n = nodeDb.registerNode(this, name);

    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class Anon: public NodeDb::NodeSingleInput::IListener {
        Pad* owner;
    public:
        Anon(Pad* n){owner = n;};

        void handleMessage(MessageBuf* msg) {
            // do not put in queue but instead forward directly to target node.
            owner->trace.println("Pad forward ");
            //owner->result->forwardMessage(msg);
            owner->out->forwardMessage(msg);
        }
    };

    in = nodeDb.registerNodeInput(n, "", new Anon(this));
    out = nodeDb.registerNodeOutput(n, "");
}
