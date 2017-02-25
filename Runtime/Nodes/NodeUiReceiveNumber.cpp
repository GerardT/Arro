/*
 * NodeUiReceiveNumber.cpp
 *
 *  Created on: Feb 20, 2017
 *      Author: gerard
 */

#include "NodeUiReceiveNumber.h"

using namespace std;
using namespace Arro;
using namespace arro;

static RegisterMe<NodeUiReceiveNumber> registerMe("UiReceiveNumber");

NodeUiReceiveNumber::NodeUiReceiveNumber(AbstractNode* d, const string& /*name*/, StringMap& /*params*/, TiXmlElement*):
    m_trace("NodeUiReceiveNumber", true),
    m_device(d) {

}

NodeUiReceiveNumber::~NodeUiReceiveNumber() {
    // TODO Auto-generated destructor stub
}

void NodeUiReceiveNumber::handleMessage(const MessageBuf& /*m*/, const std::string& /*padName*/) {
}

void NodeUiReceiveNumber::runCycle() {
    // echo...
    string buf;
    if(m_device->UiReceive(buf)) {
        m_device->UiSend(buf);
    }
}

