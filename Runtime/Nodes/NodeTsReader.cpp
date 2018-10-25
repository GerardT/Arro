
#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include "iostream"
#include "fstream"

using namespace std;
using namespace Arro;
using namespace arro;


// least significant bit (on first byte) is bit 0
unsigned long parseBits(char* buffer, int from, int to) {
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



class Section {
public:
    Section(unsigned int sectionLength, unsigned long pid):
        m_trace{"NodeTsReader-Section", true},
        m_sectionLength{sectionLength},
        m_expectedCont{0},
        m_pid{pid},
        m_packetsReceived{bitmask << ((sectionLength / maxPayloadLen) + 1) /* actually max 15 bits */} { // fixme what if exactly 1021 bytes?
        m_payload = new char[sectionLength + maxPayloadLen /* some extra space */];
        m_payloadLen = 0;
    }


    void addPayload(char* payload, unsigned long cont, INodeContext::ItRef* outputPad) {
        if(m_packetsReceived != bitmask) {

            // TODO I think we need to deal with cont counter differently because when it has
            // rolled over it might be in a different section.
            m_packetsReceived |= 1 << cont;
            //m_trace.println("Buffer " + std::to_string(m_packetsReceived));

            // append TS payload bytes to section buffer
            memcpy(m_payload + (cont * maxPayloadLen), payload, maxPayloadLen);

            if(m_packetsReceived == bitmask) {
                // section is ready
                m_trace.println("Sending section, length " + std::to_string(m_sectionLength));

                Blob* blob = new Blob();
                std::stringstream tmp;
                tmp.write(m_payload, m_sectionLength);

                blob->set_data(tmp.str());

                (*outputPad)->setOutput(*blob);

            }
        }
    }

private:
    static const unsigned int maxPayloadLen = 1021;
    static const unsigned int bitmask = 0xFFFFFFFF;
    Trace m_trace;
    unsigned int m_sectionLength;
    char* m_payload;
    unsigned int m_payloadLen;
    unsigned long m_expectedCont;
    unsigned long m_pid;
    unsigned long m_packetsReceived;

};


class NodeTsReader: public INodeDefinition {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeTsReader(INodeContext* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual void finishConstruction();

    virtual ~NodeTsReader();

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    void processTsPacket(char* packet);

    void processSectionPayload(unsigned long pid, char* payload, unsigned long startIndicator, unsigned long cont);

private:
    Trace m_trace;
    bool m_packetFound;
    INodeContext* m_elemBlock;
    INodeContext::ItRef m_inputPad;
    INodeContext::ItRef m_filterA;
    INodeContext::ItRef m_outputPad;
    INodeContext::ItRef m_statePad;
    char m_packet[188];
    char* m_tsPacket;
    int m_tsPacketLen;
    std::map<unsigned int, Section*> m_payload;

    std::set<int> m_pids;


};

static RegisterMe<NodeTsReader> registerMe("TsReader");

NodeTsReader::NodeTsReader(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeTsReader", true},
    m_packetFound{false},
    m_elemBlock{d},
    m_outputPad{nullptr},
    m_statePad{nullptr},
    m_tsPacket{nullptr} ,
    m_tsPacketLen{0}{

    m_trace.println("Constructor");

    //m_filename = d->getParameter("filename");


}

void
NodeTsReader::finishConstruction() {
    m_trace.println("finishConstruction");

    m_inputPad = m_elemBlock->begin(m_elemBlock->getInputPad("input"), 0, INodeContext::DELTA);
    m_filterA = m_elemBlock->begin(m_elemBlock->getInputPad("filterA"), 0, INodeContext::DELTA);
    m_outputPad = m_elemBlock->end(m_elemBlock->getOutputPad("value"));

    m_statePad = m_elemBlock->end(m_elemBlock->getOutputPad("_step"));

    Step* step = new Step();
    step->set_node(m_elemBlock->getName());
    step->set_name("_ready");
    m_statePad->setOutput(*step);
}

NodeTsReader::~NodeTsReader() {
}

void NodeTsReader::runCycle() {
    m_trace.println("Read bytes");


    MessageBuf msgBuf2;
    while(m_filterA->getNext(msgBuf2)) {
        m_trace.println("Read filter");
        SectionFilter* sf = new SectionFilter();
        sf->ParseFromString((*msgBuf2));

        m_pids.insert(sf->pid());
    }

    for(auto it: m_pids) {
        m_trace.println("Filtering PIDs " + std::to_string(it));
    }

    MessageBuf msgBuf1;
    if(m_inputPad->getNext(msgBuf1)) {
        Blob* blob = new Blob();
        blob->ParseFromString((*msgBuf1));

        std::string bytes = blob->data();

        for(unsigned int i = 0; i < bytes.length(); i++)
        {
            if(!m_packetFound)
            {
                if(bytes[i] == 0x47) {
                    //m_trace.println("First TS packet found");
                    m_packetFound = true;

                    if(m_tsPacket) {
                        free(m_tsPacket);
                    }

                    m_tsPacket = new char[188];
                    m_tsPacketLen = 0;
                }
            }
            if(m_packetFound) {
                // append to packet buffer
                m_tsPacket[m_tsPacketLen++] = bytes[i];
                if(m_tsPacketLen == 188) {
                    //m_trace.println("TS packet found");
                    // send packet and reset counter
    //                Blob* out = new Blob();
    //                std::stringstream tmp;
    //                tmp.write(m_tsPacket, m_tsPacketLen);
    //
    //                blob->set_data(tmp.str());
    //
    //                m_trace.println("Sending TS packet");
    //
    //                m_elemBlock->setOutputData(m_outputPad, out);
    //
                    processTsPacket(m_tsPacket);

                    m_packetFound = 0;
                    m_tsPacketLen = 0;
                }
            }
        }
    }

}


void
NodeTsReader::processTsPacket(char* packet) {
    if((packet[0] & 0x80) == 0x80) {
        m_trace.println("NodeTsReader Transport Error Indicator");
    } else {
        unsigned long PID = parseBits(packet, 11, 24);
        int cont = parseBits(packet, 28, 32);
        // m_trace.println(std::string("NodeTsReader another TS packet found, PID ") + std::to_string(PID));
        if(m_pids.find(PID) != m_pids.end()) {

            m_trace.println("NodeTsReader cont " + cont);
            printf("NodeTsReader cont %d\n", cont);
            unsigned long AdaptationField = parseBits(packet, 26, 28);
            unsigned long PayloadUnitStartIndicator = parseBits(packet, 10, 11);
            printf("NodeTsReader PUSI %ld\n", PayloadUnitStartIndicator);
            printf("NodeTsReader adapt %ld\n", AdaptationField);
            int adaptationFieldLength = 0;
            if(AdaptationField & 0x2) {
                adaptationFieldLength = packet[4]; // TODO take adaptationfield extension into account
            }
            processSectionPayload(PID, packet + 4 + adaptationFieldLength, PayloadUnitStartIndicator, cont);
        }
    }

}

void
NodeTsReader::processSectionPayload(unsigned long pid, char* payload, unsigned long startIndicator, unsigned long cont) {
    // Handle Pointer field: skip nr of bytes in pointer field
    if(startIndicator) {
        payload = payload + 1 + payload[0];
    }

    Section* section = nullptr;

    try {
        section = m_payload.at(pid);
    } catch (std::out_of_range&) {
        section = nullptr;
    }

    // If startIndicator, start a new section
    if(startIndicator) {
        unsigned long SectionLength = parseBits(payload, 14, 24);
        if(section) {
            delete section;
        }

        section = new Section(SectionLength, pid);
        m_payload[pid] = section;
    }

    section->addPayload(payload, cont, &m_outputPad);
}

