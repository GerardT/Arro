#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include "json.hpp"

namespace Arro {

class NodeRef;
class NodeUiUserInput: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiUserInput(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual ~NodeUiUserInput();

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

static RegisterMe<NodeUiUserInput> registerMe("SliderInput");

NodeUiUserInput::NodeUiUserInput(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeUiUserInput", true},
    m_elemBlock{d},
    m_uiClient{nullptr} {

    m_name = d->getParameter("name");

}

void
NodeUiUserInput::finishConstruction() {
    m_trace.println("finishConstruction");

    OutputPad* valuePad = m_elemBlock->getOutputPad("value");
    m_value = m_elemBlock->end(valuePad);

    std::string inst = std::string("<arro-slider id=\"") + m_elemBlock->getName() + "\" name=\"" + m_name + "\"></arro-slider>";

    m_uiClient = SocketClient::getInstance()->subscribe(m_elemBlock->getName(), inst, [=](const std::string& data) {

        Value* sel = new Value();
        auto info = nlohmann::json::parse(data.c_str());
        int value = info["value"];
        sel->set_value(value);
        m_value->setOutput(*sel);
                    });
}

NodeUiUserInput::~NodeUiUserInput() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiUserInput::runCycle() {
}

