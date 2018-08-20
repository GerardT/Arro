#include "Database.h"
#include "INodeContext.h"
#include "NodeDb.h"
#include "Trace.h"
#include "algorithm"
#include "string"

using namespace std;
using namespace Arro;



INodeContext::ItRef
Database::getFirst(InputPad* input, unsigned int connection, INodeContext::Mode mode) {
    m_trace.println("New iterator " + input->m_interfaceName);
    if(connection == 0) {
        return INodeContext::ItRef(((INodeContext::Iterator*)new Iterator(this, mode, m_runCycle, input->getConnections())));
    }
    else {
        const std::list<unsigned int> conn{ connection };
        return INodeContext::ItRef(((INodeContext::Iterator*)new Iterator(this, mode, m_runCycle, conn)));
    }
}


Iterator::Iterator(Database* db, INodeContext::Mode mode, unsigned int rc, const std::list<unsigned int>& conns):
    m_trace{"Iterator", false},
    m_ref{db}, m_mode{mode}, m_conns{conns}
{
    m_runCycle = rc;
    m_it = m_ref->m_db.begin();
};


Iterator::~Iterator() {};

bool
Iterator::getNext(MessageBuf& msg) {
    m_trace.println("getNext");
    if(m_mode == INodeContext::ALL) {
        // special case
    }
    else if(m_mode == INodeContext::DELTA || m_mode == INodeContext::LATEST) {

        if(m_mode == INodeContext::LATEST) {
            m_runCycle = m_ref->m_runCycle - 1;
        }
        //m_trace.println("getNext2 " + std::to_string(m_conn));

        while(m_it != m_ref->m_db.end()) {
            //m_trace.println("getNext c " + std::to_string(m_conn) + " rc " + std::to_string(m_ref->m_db.at(m_it)->m_runCycle) + " conn " + std::to_string(m_ref->m_db.at(m_it)->m_padId));
            if(m_ref->m_db.at(m_it)->m_runCycle >= m_ref->m_runCycle) {
                return false;
            }
            else if(m_ref->m_db.at(m_it)->m_runCycle < m_runCycle) {
                // skip
            }
            else  if(std::find(m_conns.begin(), m_conns.end(), m_ref->m_db.at(m_it)->m_padId) != m_conns.end()) // search for all connections
            {
                msg = m_ref->m_db.at(m_it)->m_msg;
                ++m_it;
                return true;
            }
            ++m_it;
        }
    }
    m_trace.println("No message found");
    return false;
}


