#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include "iostream"
#include "fstream"

namespace Arro {

class NodeDumper: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeDumper(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual void finishConstruction();

    virtual ~NodeDumper();

    void handleMessage(const MessageBuf& /*msg*/, const std::string& /*padName*/){};

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    Trace m_trace;
    bool m_packetFound;
    INodeContext* m_elemBlock;
    InputPad* m_inputPad;
    OutputPad* m_outputPad;
    char m_packet[188];
    int m_packetCnt;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeDumper> registerMe("Dumper");

NodeDumper::NodeDumper(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeDumper", true},
    m_elemBlock{d},
    m_inputPad{nullptr} {

    m_trace.println("Constructor");
}

void
NodeDumper::finishConstruction() {
    m_inputPad = m_elemBlock->getInputPad("input");
}

NodeDumper::~NodeDumper() {
}

void NodeDumper::runCycle() {
    m_trace.println("Read bytes");

    Blob* blob = new Blob();

    MessageBuf msgBuf = m_elemBlock->getInputData(m_inputPad);
    blob->ParseFromString((*msgBuf));

    std::string bytes = blob->data();

    unsigned int i = 0;

    std::string output;

    // search for first start of TS packet in stream
    while(i < bytes.length()) {
        char tmp[100];
        sprintf(tmp, "0x%02x ", (unsigned char)(bytes[i]));
        output += tmp;
        i++;
    }
    m_trace.println(output);
}

