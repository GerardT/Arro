
#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include "iostream"
#include "fstream"

namespace Arro {

class NodeFileReader: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeFileReader(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual void finishConstruction();

    virtual ~NodeFileReader();

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    Trace m_trace;
    INodeContext* m_elemBlock;
    std::string m_filename;
    unsigned int m_bufsize;
    std::ifstream m_file;
    OutputPad* m_outputPad;
    char* m_buf;

};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeFileReader> registerMe("FileReader");

NodeFileReader::NodeFileReader(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeFileReader", true},
    m_elemBlock{d},
    m_outputPad{nullptr} {

    m_trace.println("Constructor");

    m_filename = d->getParameter("filename");
    m_bufsize = stod(d->getParameter("bufsize"));

    m_buf = (char*)malloc(m_bufsize);


}

void
NodeFileReader::finishConstruction() {
    m_file = std::ifstream(m_filename, std::ios_base::in | std::ios_base::binary);

    m_outputPad = m_elemBlock->getOutputPad("value");

}

NodeFileReader::~NodeFileReader() {
    m_file.close();
    free(m_buf);
}

void NodeFileReader::runCycle() {
    //m_trace.println("runCycle, read data from " + m_filename);

    m_file.read(m_buf, m_bufsize);

    Blob* blob = new Blob();
    std::stringstream tmp;
    tmp.write(m_buf, m_bufsize);

    blob->set_data(tmp.str());

    m_elemBlock->setOutputData(m_outputPad, blob);

}

