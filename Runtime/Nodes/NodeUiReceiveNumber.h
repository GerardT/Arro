/*
 * NodeUiReceiveNumber.h
 *
 *  Created on: Feb 20, 2017
 *      Author: gerard
 */

#ifndef NODES_NODEUIRECEIVENUMBER_H_
#define NODES_NODEUIRECEIVENUMBER_H_

#include "arro.pb.h"
#include "Trace.h"
#include "AbstractNode.h"

namespace Arro {

class NodeUiReceiveNumber: public IDevice {
public:
    /**
     * Constructor
     *
     * \param d The Process node instance.
     * \param name Name of this node.
     * \param params List of parameters passed to this node.
     */
    NodeUiReceiveNumber(AbstractNode* d, const std::string& name, StringMap& params, TiXmlElement*);

    virtual ~NodeUiReceiveNumber();

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
    AbstractNode* m_device;

};

} /* namespace Arro */

#endif /* NODES_NODEUIRECEIVENUMBER_H_ */
