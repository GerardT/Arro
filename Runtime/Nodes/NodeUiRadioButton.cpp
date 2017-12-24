#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "AbstractNode.h"
#include "json.hpp"

using namespace nlohmann;

namespace Arro {

class NodeRef;
class NodeUiRadioButton: public IDevice {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiRadioButton(AbstractNode* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual ~NodeUiRadioButton();

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

static RegisterMe<NodeUiRadioButton> registerMe("RadioButton");

NodeUiRadioButton::NodeUiRadioButton(AbstractNode* d, const string& /*name*/, StringMap& params, TiXmlElement*):
    m_trace("NodeUiRadioButton", true),
    m_device(d) {

    std::string name;
    auto iter = params.find(std::string("name"));
    if(iter == params.end()) {
        name = "No Name";
    } else {
        name = iter->second;
        params.erase(iter);
    }

    std::string data;
    auto iter2 = params.find(std::string("data"));
    if(iter2 != params.end()) {
        data = iter2->second;
        // ? params.erase(iter);

    }

    //    <arro-radio-button id=".main.aComposite.aRadioButton" name="No Name" data='[
    //         {"name":"small","label":"Small"},{"name":"medium","label":"Medium"}
    //         ]'>
    //    </arro-radio-button>

    std::istringstream l(data);
    std::string s;
    int i = 0;
    std::string inst = std::string("<arro-radio-button id=\"") + d->getName() + "\" name=\"" + name + "\" data=\'[";
    while(getline(l, s, '|')) {
        // add s as radio button
        if(i++ != 0) {
            inst += ",";
        }
        inst += "{\"name\":\"" + s + "\",\"label\":\"" + s + "\"}";

    }
    inst += "]\'>  </arro-radio-button>";
    m_uiClient = SocketClient::getInstance()->subscribe(d->getName(), inst, [=](const std::string& data) {
        m_value = m_device->getOutput("value");

        Selection* sel = new Selection();
        auto info = json::parse(data.c_str());
        sel->set_value(info["value"]);
        m_device->setOutputData(m_value, sel);

                    });

}

NodeUiRadioButton::~NodeUiRadioButton() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiRadioButton::handleMessage(const MessageBuf& /*m*/, const std::string& /*padName*/) {
}

void NodeUiRadioButton::runCycle() {
}

