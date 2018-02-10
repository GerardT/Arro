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

static RegisterMe<NodeUiUserInput> registerMe("SliderInput");

NodeUiUserInput::NodeUiUserInput(INodeContext* d, const string& /*name*/, StringMap& params, TiXmlElement*):
    m_trace("NodeUiReceiveNumber", true),
    m_elemBlock(d) {

    std::string name;
    auto iter = params.find(std::string("name"));
    if(iter == params.end()) {
        name = "No Name";
    } else {
        name = iter->second;
        params.erase(iter);
    }

    //    <arro-slider id=".main.aComposite.aInput1.aSliderInput" name="speed"></arro-slider>

    std::string inst = std::string("<arro-slider id=\"") + d->getName() + "\" name=\"" + name + "\"></arro-slider>";

    m_uiClient = SocketClient::getInstance()->subscribe(d->getName(), inst, [=](const std::string& data) {
        m_value = m_elemBlock->getOutputPad("value");

        Value* sel = new Value();
        auto info = nlohmann::json::parse(data.c_str());
        int value = info["value"];
        sel->set_value(value);
        m_elemBlock->setOutputData(m_value, sel);
                    });

}

NodeUiUserInput::~NodeUiUserInput() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiUserInput::handleMessage(const MessageBuf& /*m*/, const std::string& /*padName*/) {
}

void NodeUiUserInput::runCycle() {
}

