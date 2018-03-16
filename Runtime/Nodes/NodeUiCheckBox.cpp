#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include "json.hpp"

namespace Arro {

class NodeRef;
class NodeUiCheckBox: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiCheckBox(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual ~NodeUiCheckBox();

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
    INodeContext* m_elemBlock;
    NodeRef* m_uiClient;
    OutputPad* m_value;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiCheckBox> registerMe("CheckBox");

NodeUiCheckBox::NodeUiCheckBox(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace("NodeUiReceiveNumber", true),
    m_elemBlock(d) {

    std::string name = d->getParameter("name");

    std::string inst = std::string("<arro-check-box id=\"") + d->getName() + "\" name=\"" + name + "\"></arro-check-box>";

    m_uiClient = SocketClient::getInstance()->subscribe(d->getName(), inst, [=](const std::string& data) {
        m_value = m_elemBlock->getOutputPad("value");

        Selection* sel = new Selection();
        auto info = nlohmann::json::parse(data.c_str());
        sel->set_value(info["value"]);
        m_elemBlock->setOutputData(m_value, sel);
                    });

}

NodeUiCheckBox::~NodeUiCheckBox() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiCheckBox::handleMessage(const MessageBuf& /*m*/, const std::string& /*padName*/) {
}

void NodeUiCheckBox::runCycle() {
}

