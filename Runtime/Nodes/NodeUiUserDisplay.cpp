#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"

namespace Arro {

class NodeRef;
class NodeUiUserDisplay: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiUserDisplay(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);
    virtual void finishConstruction();

    virtual ~NodeUiUserDisplay();

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    Trace m_trace;
    INodeContext* m_elemBlock;
    NodeRef* m_uiClient;
    INodeContext::ItRef m_input;
    INodeContext::ItRef m_value;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiUserDisplay> registerMe("ProgressOutput");

NodeUiUserDisplay::NodeUiUserDisplay(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeUiUserDisplay", true},
    m_elemBlock{d},
    m_uiClient{nullptr} {

    std::string name = d->getParameter("name");

    std::string inst = std::string("<arro-progress id=\"") + d->getName() + "\" name=\"" + name + "\"></arro-progress>";
    m_uiClient = SocketClient::getInstance()->subscribe(d->getName(), inst, [=](const std::string& /*data*/) {
                        // SocketClient::getInstance()->sendMessage(m_uiClient, data);
                    });

}

void
NodeUiUserDisplay::finishConstruction() {
    m_value = m_elemBlock->begin(m_elemBlock->getInputPad("value"), 0, INodeContext::DELTA);
};

NodeUiUserDisplay::~NodeUiUserDisplay() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiUserDisplay::runCycle() {
    MessageBuf buf2;
    if(m_value->getNext(buf2)) {
        Value* msg2 = new Value();
        msg2->ParseFromString((*buf2).c_str());

        // output { "value": "<val>" }
        std::string json_string = "{ \"value\": " + std::to_string(msg2->value()) + " }";

        if(buf2 != nullptr) {
            SocketClient::getInstance()->sendMessage(m_uiClient, json_string);
        }
    }


}

