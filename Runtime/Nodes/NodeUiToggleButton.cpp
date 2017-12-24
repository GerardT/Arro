#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "AbstractNode.h"
#include "json.hpp"

using namespace nlohmann;

namespace Arro {

class NodeRef;
class NodeUiToggleButton: public IDevice {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiToggleButton(AbstractNode* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual ~NodeUiToggleButton();

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
    NodeMultiOutput* m_value;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiToggleButton> registerMe("ToggleButton");

NodeUiToggleButton::NodeUiToggleButton(AbstractNode* d, const string& /*name*/, StringMap& params, TiXmlElement*):
    m_trace("NodeUiUserInput.cpp", true),
    m_device(d) {

    std::string name;
    auto iter = params.find(std::string("name"));
    if(iter == params.end()) {
        name = "No Name";
    } else {
        name = iter->second;
        params.erase(iter);
    }
    std::string inst = std::string("<arro-toggle-button id=\"") + d->getName() + "\" name=\"" + name + "\"></arro-toggle-button>";

    m_uiClient = SocketClient::getInstance()->subscribe(d->getName(), inst, [=](const std::string& data) {
        m_value = m_device->getOutput("value");

        Selection* sel = new Selection();
        auto info = json::parse(data.c_str());
        sel->set_value(info["value"]);
        m_device->setOutputData(m_value, sel);
                    });

}

NodeUiToggleButton::~NodeUiToggleButton() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiToggleButton::handleMessage(const MessageBuf& /*m*/, const std::string& /*padName*/) {
}

void NodeUiToggleButton::runCycle() {
}

