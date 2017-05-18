#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "AbstractNode.h"

namespace Arro {

class NodeRef;
class NodeUiUserDisplay: public IDevice {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiUserDisplay(AbstractNode* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual ~NodeUiUserDisplay();

    /**
     * Handle a message that is sent to this node.
     *
     * \param msg Message sent to this node.
     * \param padName name of pad that message was sent to.
     */
    void handleMessage(const MessageBuf& msg, const std::string& padName);

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    Trace m_trace;
    AbstractNode* m_device;
    NodeRef* m_uiClient;
    NodeSingleInput* m_input;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiUserDisplay> registerMe("UiUserDisplay");

NodeUiUserDisplay::NodeUiUserDisplay(AbstractNode* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace("NodeUiReceiveNumber", true),
    m_device(d) {

    m_uiClient = SocketClient::getInstance()->subscribe(d->getName(), [=](const std::string& data) {
                        // SocketClient::getInstance()->sendMessage(m_uiClient, data);
                    });

}

NodeUiUserDisplay::~NodeUiUserDisplay() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiUserDisplay::handleMessage(const MessageBuf& /*m*/, const std::string& /*padName*/) {
}

void NodeUiUserDisplay::runCycle() {
    m_input = m_device->getInput("input");

    MessageBuf buf = m_device->getInputData(m_input);

    Json* msg = new Json();
    msg->ParseFromString((*buf).c_str());


    if(buf != nullptr) {
        SocketClient::getInstance()->sendMessage(m_uiClient, msg->data());
    }
}

