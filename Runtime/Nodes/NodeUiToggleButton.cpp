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

    void finishConstruction();

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    Trace m_trace;
    INodeContext* m_elemBlock;
    NodeRef* m_uiClient;
    INodeContext::ItRef m_value;
    std::string m_name;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiToggleButton> registerMe("ToggleButton");

NodeUiToggleButton::NodeUiToggleButton(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeUiToggleButton", true},
    m_elemBlock{d},
    m_uiClient{nullptr} {

    m_name = d->getParameter("name");


}

void
NodeUiToggleButton::finishConstruction() {
    m_trace.println("finishConstruction");

    OutputPad* valuePad = m_elemBlock->getOutputPad("value");
    m_value = m_elemBlock->end(valuePad);

    std::string inst = std::string("<arro-toggle-button id=\"") + m_elemBlock->getName() + "\" name=\"" + m_name + "\"></arro-toggle-button>";

    m_uiClient = SocketClient::getInstance()->subscribe(m_elemBlock->getName(), inst, [=](const std::string& data) {

        Selection* sel = new Selection();
        auto info = nlohmann::json::parse(data.c_str());
        sel->set_value(info["value"]);
        m_value->setRecord(*sel);
                    });

}

NodeUiToggleButton::~NodeUiToggleButton() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiToggleButton::runCycle() {
}

