
#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include "iostream"
#include "fstream"

using namespace std;
using namespace Arro;
using namespace arro;

// least significant bit (on first byte) is bit 0
unsigned long parseBits(const char* buffer, int from, int to) {
    if(to - from > (32)) {
        return 0;
    }
    int offset = from / 8;
    int shift = from % 8;
    unsigned char* b = (unsigned char*)buffer + offset;

    unsigned long result = b[3] + (b[2] << 8) + (b[1] << 16) + (b[0] << 24);
    result = result << shift;
    result = result >> (32 - (to - from));
    //result = result + (b[4] << (32 - shift));

    // mask unneeded bits
    unsigned long mask = ~ ((0xffffffff) << (to - from) );

    result = result & mask;

    return result;
}



class NodeTsSubtable: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeTsSubtable(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual void finishConstruction();

    virtual ~NodeTsSubtable();

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    const char* handleDesc(const char* data);
    void handlePAT(const char* buf, unsigned long SectionLength);
    void handleCAT(const char* buf, unsigned long SectionLength);
    void handlePMT(const char* buf, unsigned long SectionLength);

private:
    static const int sizeTableHeader = 3;
    static const int sizeSubtableHeader = 5;
    static const int sizeHeader = sizeTableHeader + sizeSubtableHeader;
    static const int sizeCrc = 4;
    Trace m_trace;
    INodeContext* m_elemBlock;
    INodeContext::ItRef m_inputPad;
    INodeContext::ItRef m_outputPad;
    INodeContext::ItRef m_pmtFilter;
    INodeContext::ItRef m_inputIt;
    INodeContext::ItRef m_patIt;

};



static RegisterMe<NodeTsSubtable> registerMe("TsSubtable");

NodeTsSubtable::NodeTsSubtable(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeTsSubtable", true},
    m_elemBlock{d},
    m_inputPad{nullptr},
    m_outputPad{nullptr} {

    m_trace.println("Constructor");

    //m_filename = d->getParameter("filename");


}

void
NodeTsSubtable::finishConstruction() {
    m_inputPad = m_elemBlock->begin(m_elemBlock->getInputPad("input"), 0, INodeContext::DELTA);
    m_outputPad = m_elemBlock->end(m_elemBlock->getOutputPad("value"));
    m_pmtFilter = m_elemBlock->end(m_elemBlock->getOutputPad("pmtFilter"));

    m_patIt = m_elemBlock->end(m_elemBlock->getOutputPad("patFilter"));

    SectionFilter* sf = new SectionFilter();
    sf->set_pid(0 /* PAT pid */);
//    m_elemBlock->setOutputData(m_elemBlock->getOutputPad("patFilter"), sf);
    m_patIt->setRecord(*sf);

}

NodeTsSubtable::~NodeTsSubtable() {
}

void NodeTsSubtable::runCycle() {
    // m_trace.println("Read bytes");

    Blob* blob = new Blob();
    //blob->set_data(somedata);

    MessageBuf msgBuf;
    if(m_inputPad->getNext(msgBuf)) {
        blob->ParseFromString((*msgBuf));

        std::string bytes = blob->data();

        const char* buf = bytes.c_str();

        unsigned long TableId = parseBits(buf, 0, 8);
    //    unsigned long SectionSyntaxIndicator = parseBits(buf, 8, 9);
        unsigned long SectionLength = parseBits(buf, 14, 24);
    //    unsigned long TableIdExtension = 0;
    //    if(SectionSyntaxIndicator) {
    //        TableIdExtension = parseBits(buf, 24, 30);
    //    }

        switch(TableId) {
        case 0:
            handlePAT(buf + sizeHeader, SectionLength - sizeSubtableHeader - sizeCrc);
            break;
        case 1:
            handleCAT(buf + sizeHeader, SectionLength - sizeSubtableHeader - sizeCrc);
            break;
        case 2:
            handlePMT(buf + sizeHeader, SectionLength - sizeSubtableHeader - sizeCrc);
            break;
        }
    }
}


const char* NodeTsSubtable::handleDesc(const char* data) {
    return data + data[1];
}

void NodeTsSubtable::handlePAT(const char* data, unsigned long dataSize) {
    m_trace.println("Handling PAT size: " + std::to_string(dataSize));

    int nrLoops = dataSize / 4;

    for(int i = 0; i < nrLoops; i++) {
        unsigned long ProgramNum = parseBits(data + (i * 4), 0, 16);
        unsigned long ProgramMapPid = parseBits(data + (i * 4), 19, 32);
        m_trace.println("PAT - found a program, Num: " + std::to_string(ProgramNum) + " Pid: " + std::to_string(ProgramMapPid));

        SectionFilter* sf = new SectionFilter();
        sf->set_pid(ProgramMapPid);
        m_pmtFilter->setRecord(*sf);
    }



}

void NodeTsSubtable::handleCAT(const char* /*data*/, unsigned long /*dataSize*/) {
    m_trace.println("Handling CAT");
}

void NodeTsSubtable::handlePMT(const char* data, unsigned long dataSize) {
    m_trace.println("Handling PMT");

    const char* end = data + dataSize;

    unsigned long pcrPID = parseBits(data, 3, 13);
    unsigned long programInfoLength = parseBits(data, 22, 32);
    m_trace.println("PMT - PCR PID: " + std::to_string(pcrPID) + " programInfoLength " + std::to_string(programInfoLength));

    data += 4;
    data += programInfoLength;

    while(data < end) {

        unsigned long streamType = parseBits(data, 0, 8);
        unsigned long elementaryPid = parseBits(data, 11, 24);

        m_trace.println("PMT - found elem stream type: " + std::to_string(streamType) + " PID: " + std::to_string(elementaryPid));

        data += 5;

        data = handleDesc(data);

    }
}

