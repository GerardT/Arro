#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include "json.hpp"

namespace Arro {

class NodeRef;
class NodeUiToggleButton: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiToggleButton(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual ~NodeUiToggleButton();

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    Trace m_trace;
    INodeContext* m_elemBlock;
    NodeRef* m_uiClient;
    INodeContext::ItRef m_value;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiToggleButton> registerMe("ToggleButton");

NodeUiToggleButton::NodeUiToggleButton(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace("NodeUiUserInput.cpp", true),
    m_elemBlock(d) {

    std::string name = d->getParameter("name");

    std::string inst = std::string("<arro-toggle-button id=\"") + d->getName() + "\" name=\"" + name + "\"></arro-toggle-button>";

    m_uiClient = SocketClient::getInstance()->subscribe(d->getName(), inst, [=](const std::string& data) {
        m_value = m_elemBlock->end(m_elemBlock->getOutputPad("value"));

        Selection* sel = new Selection();
        auto info = nlohmann::json::parse(data.c_str());
        sel->set_value(info["value"]);
        m_value->setOutput(*sel);
                    });

}

NodeUiToggleButton::~NodeUiToggleButton() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiToggleButton::runCycle() {
}

