#include <google/protobuf/text_format.h>
#include <google/protobuf/util/json_util.h>

#include <NodeUiOut.h>
#include <arro.pb.h>
#include <NodeDb.h>


using namespace std;
//using namespace google;
//using namespace protobuf;
using namespace arro;
using namespace Arro;

using namespace google::protobuf;




NodeUiOut::NodeUiOut(AbstractNode* d, const string& /*name*/, Arro::StringMap& /*params*/, TiXmlElement*):
    m_trace("NodeUiOut", true),
    m_device(d) {

//    try {
//        m_Ch = stod(params.at("Motor"));
//    }
//    catch (std::out_of_range) {
//        m_Ch = 0;
//    }



}


void
NodeUiOut::handleMessage(const MessageBuf& /*m*/, const std::string& padName) {
    m_trace.println("NodeUiOut::handleMessage");
    m_trace.println(padName);
    if(padName == "speed") {
        // do not put in queue but instead forward directly to target node.
        m_trace.println("NodeUiOut dump ");
        /*
        //Token* m = new Token();
        //m->CheckTypeAndMergeFrom(*msg);
        //TextFormat::PrintToString(*(Message*)msg, &(n->xml));
        TextGenerator generator(&(xml));
        Print(*(Message*)msg, generator);
        generator.getString(xml);
        trace.println(
        n->getName());
        */
    } else {
        m_trace.println(string("Message received from ") + padName);
    }
}

void
NodeUiOut::runCycle() {
    m_trace.println("NodeUiOut::runCycle");

    MessageBuf data = m_device->getInputData(m_device->getInput("input"));

    auto m = new Value();
    m->ParseFromString(data->c_str());

    util::JsonOptions options{};
    string out;
    //util::MessageToJsonString(m, &out, options);


}



