#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include "json.hpp"

namespace Arro {

class NodeRef;
class NodeUiRadioButton: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiRadioButton(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual ~NodeUiRadioButton();

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
    std::string m_data;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiRadioButton> registerMe("RadioButton");

NodeUiRadioButton::NodeUiRadioButton(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeUiRadioButton", true},
    m_uiClient{nullptr},
    m_elemBlock{d} {

    m_name = d->getParameter("name");

    m_data = d->getParameter("data");

    //    <arro-radio-button id=".main.aComposite.aRadioButton" name="No Name" data='[
    //         {"name":"small","label":"Small"},{"name":"medium","label":"Medium"}
    //         ]'>
    //    </arro-radio-button>

}

void
NodeUiRadioButton::finishConstruction() {
    m_trace.println("finishConstruction");

    OutputPad* valuePad = m_elemBlock->getOutputPad("value");

    std::istringstream l(m_data);
    std::string s;
    int i = 0;
    std::string inst = std::string("<arro-radio-button id=\"") + m_elemBlock->getName() + "\" name=\"" + m_name + "\" data=\'[";
    while(getline(l, s, '|')) {
        // add s as radio button
        if(i++ != 0) {
            inst += ",";
        }
        inst += "{\"name\":\"" + s + "\",\"label\":\"" + s + "\"}";

    }
    inst += "]\'>  </arro-radio-button>";
    m_uiClient = SocketClient::getInstance()->subscribe(m_elemBlock->getName(), inst, [=](const std::string& data) {
        m_value = m_elemBlock->end(valuePad);

        Selection* sel = new Selection();
        auto info = nlohmann::json::parse(data.c_str());
        sel->set_value(info["value"]);
        m_value->setOutput(*sel);

                    });

}

NodeUiRadioButton::~NodeUiRadioButton() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiRadioButton::runCycle() {
}

