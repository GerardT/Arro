#include "SocketClient.h"

#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"

namespace Arro {

class NodeRef;
class NodeUiMap: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiMap(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);
    virtual void finishConstruction();

    virtual ~NodeUiMap();

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    Trace m_trace;
    INodeContext* m_elemBlock;
    NodeRef* m_uiClient;
    INodeContext::ItRef m_location;
};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiMap> registerMe("UiMap");

NodeUiMap::NodeUiMap(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeUiMap", true},
    m_elemBlock{d},
    m_uiClient{nullptr} {

    std::string name = d->getParameter("name");

    std::string inst = std::string("<arro-map id=\"") + d->getName() + "\" name=\"" + name + "\"></arro-map>";
    m_uiClient = SocketClient::getInstance()->subscribe(d->getName(), inst, [=](const std::string& /*data*/) {
                        // SocketClient::getInstance()->sendMessage(m_uiClient, data);
                    });

}

void
NodeUiMap::finishConstruction() {
    m_location = m_elemBlock->begin(m_elemBlock->getInputPad("location"), 0, INodeContext::DELTA);
};

NodeUiMap::~NodeUiMap() {
    SocketClient::getInstance()->unsubscribe(m_uiClient);
}

void NodeUiMap::runCycle() {
    MessageBuf buf;
    if(m_location->getNext(buf)) {
        LocationAndHeading msg;
        msg.ParseFromString((*buf).c_str());

        std::string json_string =
                "{ \"value\": { \"heading\": " + std::to_string(msg.heading()) +
                "             , \"position\": { \"x\": " + std::to_string(msg.position().x()) +
                "                             , \"y\": " + std::to_string(msg.position().y()) +
                "                             }" +
                "             }" +
                "}";

        if(buf != nullptr) {
            SocketClient::getInstance()->sendMessage(m_uiClient, json_string);
        }
    }
}

