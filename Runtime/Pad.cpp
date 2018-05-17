#include "NodeDb.h"
#include "Pad.h"


using namespace std;
using namespace Arro;


/**
 * Pad, like Process, implements interface INodeContext.
 * Pad is for non-functional connection pads.
 * Basically it just installs a listener that forwards incoming messages.
 * TODO: maybe we could remove Pads and connect Process outputs directly to Process inputs.
 */
Pad::Pad(NodeDb& nodeDb, const string& /*datatype*/, const string& name, unsigned int padId):
    RealNode{},
    trace{"Pad", false},
    m_in{nullptr},
    m_out{nullptr},
    m_name{name} {

    nodeDb.registerNode(this, name);

    m_in = nodeDb.registerNodeInput(this, "", [this](const MessageBuf& msg, const std::string&) {
        // do not put in queue but instead forward directly to target node.
        trace.println(string("Pad forward ") + this->getName());
        m_out->forwardMessage(msg);
    });
    m_out = nodeDb.registerNodeOutput(this, padId,"");
}
