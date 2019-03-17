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

    virtual void finishConstruction();

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

static RegisterMe<NodeUiCheckBox> registerMe("CheckBox");

NodeUiCheckBox::NodeUiCheckBox(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeUiCheckBox", true},
    m_elemBlock{d},
    m_uiClient{nullptr} {

    m_name = d->getParameter("name");
}

void
NodeUiCheckBox::finishConstruction() {
    OutputPad* valuePad = m_elemBlock->getOutputPad("value");
    m_value = m_elemBlock->end(valuePad);

    std::string inst = std::string("<arro-check-box id=\"") + m_elemBlock->getName() + "\" name=\"" + m_name + "\"></arro-check-box>";

    m_uiClient = SocketClient::getInstance()->subscribe(m_elemBlock->getName(), inst, [=](const std::string& data) {

        Selection* sel = new Selection();
        auto info = nlohmann::json::parse(data.c_str());
        sel->set_value(info["value"]);
        m_value->setRecord(*sel);
            });

}


NodeUiCheckBox::~NodeUiCheckBox() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiCheckBox::runCycle() {
}

