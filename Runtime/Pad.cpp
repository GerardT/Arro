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
    m_trace{"Pad", false},
    m_in{nullptr},
    m_out{nullptr},
    m_name{name},
    m_conn{0} {

    m_trace.println("Pad " + std::to_string(padId));

    nodeDb.registerNode(this, name);

    m_in = nodeDb.registerNodeInput(this, "", [this](const MessageBuf& msg, const std::string&) {
        // do not put in queue but instead forward directly to target node.
        m_trace.println(string("Pad forward ") + this->getName());
        m_out->forwardMessage(msg);

        // and also copy the message from input to output
    });
    m_out = nodeDb.registerNodeOutput(this, padId,"");
}

void
Pad::runCycle() {
    MessageBuf s = m_in->getData(m_conn);
    m_out->m_nm->m_database.store(m_out->getPadId(), s);
};


void
Pad::finishConstruction() {
    std::list<unsigned int> connections = m_in->getConnections();
    m_conn = connections.front();
};
