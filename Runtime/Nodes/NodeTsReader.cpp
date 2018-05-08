
/*
import com.example.tutorial.AddressBookProtos.Tick;
import com.example.tutorial.AddressBookProtos.TsHeader;
import com.example.tutorial.AddressBookProtos.Value;
import com.example.tutorial.AddressBookProtos.Token;
import com.example.tutorial.AddressBookProtos.Mode;
import com.example.tutorial.AddressBookProtos.TsSection;
import com.example.tutorial.AddressBookProtos.TsHeader;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
*/


#include "arro.pb.h"
#include "Trace.h"
#include "INodeContext.h"
#include "iostream"
#include "fstream"

namespace Arro {

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
        m_expectedCont{0},
        m_pid{pid} {
        m_payload = new char[sectionLength];
        m_payloadLen = 0;
    }

    void processTsPayLoad(char* payload, unsigned long startIndicator, unsigned long cont) {
        // If startIndicator, start a new section
        if(startIndicator) {

        }
        // append TS payload bytes to section buffer, by incrementing cont.

        unsigned long TableId = parseBits(payload, 0, 8);
        printf("NodeTsReader tableid %d\n", TableId);

//        unsigned long SectionsyntaxIndicator = parseBits(payload, 8, 9);
//        unsigned long PrivateBit = parseBits(payload, 9, 10);

    }

private:
    char* m_payload;
    unsigned int m_payloadLen;
    unsigned long m_expectedCont;
    unsigned long m_pid;

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

    void handleMessage(const MessageBuf& /*msg*/, const std::string& /*padName*/){};

    /**
     * Make the node execute a processing cycle.
     */
    void runCycle();

private:
    void processTsPacket(char* packet, unsigned long pid);

    void processSectionPayload(unsigned long pid, char* payload, unsigned long startIndicator, unsigned long cont);

private:
    Trace m_trace;
    bool m_packetFound;
    INodeContext* m_elemBlock;
    InputPad* m_inputPad;
    OutputPad* m_outputPad;
    char m_packet[188];
    char* m_tsPacket;
    int m_tsPacketLen;
    std::map<unsigned int, Section*> m_payload;


};

} /* namespace Arro */


using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeTsReader> registerMe("TsReader");

NodeTsReader::NodeTsReader(INodeContext* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace{"NodeTsReader", true},
    m_packetFound{false},
    m_elemBlock{d},
    m_inputPad{nullptr},
    m_outputPad{nullptr},
    m_tsPacket{nullptr} ,
    m_tsPacketLen{0}{

    m_trace.println("Constructor");

    //m_filename = d->getParameter("filename");


}

void
NodeTsReader::finishConstruction() {
    m_inputPad = m_elemBlock->getInputPad("input");
    m_outputPad = m_elemBlock->getOutputPad("value");

}

NodeTsReader::~NodeTsReader() {
}

void NodeTsReader::runCycle() {
    // m_trace.println("Read bytes");

    Blob* blob = new Blob();
    //blob->set_data(somedata);

    MessageBuf msgBuf = m_elemBlock->getInputData(m_inputPad);
    blob->ParseFromString((*msgBuf));

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
                Blob* out = new Blob();
                std::stringstream tmp;
                tmp.write(m_tsPacket, m_tsPacketLen);

                blob->set_data(tmp.str());

                m_trace.println("Sending TS packet");

                m_elemBlock->setOutputData(m_outputPad, out);

                processTsPacket(m_tsPacket, 0x0);

                m_packetFound = 0;
                m_tsPacketLen = 0;
            }
        }
    }
}


void
NodeTsReader::processTsPacket(char* packet, unsigned long pid) {
    if((packet[0] & 0x80) == 0x80) {
        m_trace.println("NodeTsReader Transport Error Indicator");
    } else {


        /*
        Scrambling control      2   00 = Not scrambled. The following per DVB spec: 01 = Reserved for future use, 10 = Scrambled with even key, 11 = Scrambled with odd key
        Adaptation field exist  2   01 = no adaptation fields, payload only, 10 = adaptation field only, 11 = adaptation field and payload
        Continuity counter      4   Incremented only when a payload is present (i.e., adaptation field exist is 01 or 11)
        */

        // check PID (bit 3 ... 16)
        unsigned long PID = parseBits(packet, 11, 24);
        int cont = parseBits(packet, 28, 32);
        m_trace.println(std::string("NodeTsReader another TS packet found, PID ") + std::to_string(PID));
        if(PID == pid) {

            m_trace.println("NodeTsReader cont " + cont);
            printf("NodeTsReader cont %d\n", cont);
            unsigned long AdaptationField = parseBits(packet, 26, 28);
            unsigned long PayloadUnitStartIndicator = parseBits(packet, 10, 11);
            printf("NodeTsReader PUSI %ld\n", PayloadUnitStartIndicator);
            printf("NodeTsReader adapt %ld\n", AdaptationField);
            int adaptationFieldLength = 0;
            if(AdaptationField) {
                adaptationFieldLength = packet[4]; // TODO take adaptationfield extension into account
            }
            if(PayloadUnitStartIndicator) {
                processSectionPayload(PID, packet + 4 + adaptationFieldLength, PayloadUnitStartIndicator, cont);
            }
        }
    }

}

void
NodeTsReader::processSectionPayload(unsigned long pid, char* payload, unsigned long startIndicator, unsigned long cont) {
    printf("NodeTsReader payload found 0x%02x\n", payload[0]);

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

        section = new Section(pid, SectionLength);
        m_payload[pid] = section;
    }
    section->processTsPayLoad(payload,startIndicator, cont);
}




#if 0
class NodeTsReader: INodeDefinition {
private:
    Trace trace = new Trace("", false);

    String actual_mode = "Idle";

    FileInputStream in = null;
    HashMap<String, String> params;
    int pid = 0;

    IPadOutput result;

public:
            @Override
            public void handleMessage(MessageBuf msg, string padName) {
                Mode* msg = new Mode();
                msg->ParseFromString(m);

                Class<?> cl = msg.getClass();
                assert(cl.getSimpleName().equals("Mode"));

                actual_mode = ((Mode)msg).getMode();

                trace.println("NodeTsReader changed mode to " + actual_mode);
                if(actual_mode.equals("Active")) {
                    try {
                        // open and read until sync byte 0x47
                        String filename = params.get("file");
                        in = new FileInputStream(filename);
                        int c;

                        while ((c = in.read()) != -1 && c != 0x47) {
                        }
                        trace.println("NodeTsReader file open and sync byte found");

                    } catch (IOException name) {
                        trace.println("NodeTsReader IOException");
                    }
                }
                else {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException name) {
                            trace.println("NodeTsReader IOException");
                        }
                    }
                }
            }
        });

        result = n.registerNodeOutput("TsSection", "ts");
    }
    public void doIt (int ms_elapsed) {

        trace.println("NodeTsReader  ");

        if(actual_mode.equals("Active")) {
            ms_elapsed /= 1000;


            try {
                int c;
                int i = 0;
                byte[] buffer;

                buffer = new byte[188];

                while ((c = in.read()) != -1 && i < (188 - 1)) {
                    buffer[i++] = (byte)c;
                }
                if(c == 0x47) {
                    // check Transport Error Indicator (bit 0)
                    if((buffer[0] & 0x80) == 0x80) {
                        trace.println("NodeTsReader Transport Error Indicator");
                    } else {

                        /*
                        Scrambling control      2   00 = Not scrambled. The following per DVB spec: 01 = Reserved for future use, 10 = Scrambled with even key, 11 = Scrambled with odd key
                        Adaptation field exist  2   01 = no adaptation fields, payload only, 10 = adaptation field only, 11 = adaptation field and payload
                        Continuity counter      4   Incremented only when a payload is present (i.e., adaptation field exist is 01 or 11)
                        */

                        // check PID (bit 3 ... 16)
                        int PID = buffer[1] + ((buffer[0] & 0x1F) * 256);
                        int Cont = buffer[2] & 0x0F;
                        trace.println("NodeTsReader another TS packet found, PID " + PID);
                        if(PID == pid) {

                            trace.println("NodeTsReader cont " + Cont);

                            TsSection.Builder value = TsSection.newBuilder();
                            TsHeader.Builder header = TsHeader.newBuilder();

                            header.setTransportErrorIndicator(buffer[0] & 0x80);
                            header.setPayloadUnitStartIndicator(buffer[0] & 0x40);
                            header.setTransportPriority(buffer[0] & 0x20);
                            header.setPid(PID);
                            header.setScramblingControl(buffer[2] & 0xC0);
                            header.setAdaptationField(buffer[2] & 0x20);
                            header.setPayloadData(buffer[2] & 0x10);
                            header.setContinuityCounter(buffer[2] & 0x0F);

                            value.setHeader(header);
                            value.setPayloadBytes(ByteString.copyFrom(buffer, 0, 188));

                            result->submitMessage(value.build());
                        }
                    }
                }
            } catch (IOException name) {
                trace.println("NodeTsReader IOException");
            }

        }

    }
}
#endif
