#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "AbstractNode.h"

namespace Arro {

class NodeRef;
class NodeUiUserInput: public IDevice {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiUserInput(AbstractNode* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual ~NodeUiUserInput();

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
    NodeMultiOutput* m_output;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiUserInput> registerMe("UiUserInput");

NodeUiUserInput::NodeUiUserInput(AbstractNode* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace("NodeUiReceiveNumber", true),
    m_device(d) {

    // TODO should get i/o here
    //m_output = m_device->getOutput("output");

    m_uiClient = SocketClient::getInstance()->subscribe(d->getName(), [=](const std::string& data) {
        m_output = m_device->getOutput("output");

        Json* value = new Json();

        value->set_data(data);

        m_device->setOutputData(m_output, value);
                    });

}

NodeUiUserInput::~NodeUiUserInput() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiUserInput::handleMessage(const MessageBuf& /*m*/, const std::string& /*padName*/) {
}

void NodeUiUserInput::runCycle() {
}

