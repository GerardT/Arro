#include "Database.h"
#include "INodeContext.h"
#include "NodeDb.h"
#include "Trace.h"
#include "algorithm"
#include "string"

using namespace std;
using namespace Arro;


INodeContext::ItRef
Database::begin(InputPad* input, unsigned int connection, INodeContext::Mode mode) {
    m_trace.println("New iterator " + input->m_interfaceName);
    if(connection == 0) {
        return INodeContext::ItRef(new Iterator(this, mode, input->getConnections()));
    }
    else {
        const std::list<unsigned int> conn{ connection };
        return INodeContext::ItRef(new Iterator(this, mode, conn));
    }
}

INodeContext::ItRef
Database::end(OutputPad* /*input*/, unsigned int connection) {
    m_trace.println("New iterator ");

    // build a list with only one padId, the padId of the output pad.
    std::list<unsigned int> conn;
    conn.push_back(connection);

    return INodeContext::ItRef(new Iterator(this, INodeContext::LATEST, conn));
}



Iterator::Iterator(Database* db, INodeContext::Mode mode, const std::list<unsigned int>& conns):
    m_trace{"Iterator", false},
    m_ref{db},
    m_db{db->m_db},
    m_mode{mode},
    m_conns{conns},
    m_it{0},
    m_currentEmpty{true}
{
};


Iterator::~Iterator() {};

/**
 * If last time it returned false (no more records) a next call will test if new records
 * have been added in the meantime. If so, the first new record is returned.
 *
 * @param msg
 * @return
 */
bool
Iterator::getNext(MessageBuf& msg) {
    m_trace.println("getNext");

    if(m_currentEmpty == false) {
        m_it++;
    }

    for(auto it = m_conns.begin(); it != m_conns.end(); ++it) {
        m_trace.println("Conn " + std::to_string(*it));
    }

    while(m_it < m_db.m_upCount) {
        if(m_db.m_up[m_it].index == Database::DbRecord::DbEmpty) {
            // skip
        } else {
            unsigned int ix = m_db.m_up[m_it].index;
            Database::DbRecord r = m_db.m_db[ix];
            m_trace.println("Record checking " + std::to_string(ix) + " index " + std::to_string(m_it) + " pad " + std::to_string(r.getPadId()));
            if(std::find(m_conns.begin(), m_conns.end(), r.getPadId()) != m_conns.end()) {
                r.getMessage(msg);
                m_currentEmpty = false;
                m_trace.println("Record found at " + std::to_string(ix) + " index " + std::to_string(m_it));
               return true;
            }
        }
        m_it++;
    }
    if(m_it == m_db.m_upCount) {
        m_trace.println("No more messages found");
        m_currentEmpty = true;
        return false;
    }
    return false;
}


void
Iterator::insertOutput(google::protobuf::MessageLite& msg) {
    m_trace.println("Inserting message msg");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    // get the (only) padId of this output pad
    unsigned int padId = m_conns.front();

    MessageBuf s(new std::string(msg.SerializeAsString()));
    m_it = m_db.getFree();
    m_ref->m_new.push_back(Database::DbRecord(m_it, padId, m_ref->m_runCycle, 0, s));

    m_ref->m_condition.notify_one();
}

void
Iterator::insertOutput(MessageBuf& msg) {
    m_trace.println("Inserting message buf");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    // get the (only) padId of this output pad
    unsigned int padId = m_conns.front();

    m_it = m_db.getFree();
    m_ref->m_new.push_back(Database::DbRecord(m_it, padId, m_ref->m_runCycle, 0, msg));

    m_ref->m_condition.notify_one();
}

void
Iterator::updateOutput(google::protobuf::MessageLite& msg) {
    m_trace.println("Updating message");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    MessageBuf s(new std::string(msg.SerializeAsString()));
    unsigned int padId = m_conns.front();
    m_ref->m_new.push_back(Database::DbRecord(m_it, padId, m_ref->m_runCycle, 0, s));

    m_ref->m_condition.notify_one();
}

void
Iterator::updateOutput(MessageBuf& msg) {
    m_trace.println("Updating message");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    unsigned int padId = m_conns.front();
    m_ref->m_new.push_back(Database::DbRecord(m_it, padId, m_ref->m_runCycle, 0, msg));

    m_ref->m_condition.notify_one();
}

void
Iterator::deleteOutput() {
    m_trace.println("Deleting message");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    MessageBuf s(nullptr);
    unsigned int padId = m_conns.front();
    m_ref->m_new.push_back(Database::DbRecord(m_it, padId, m_ref->m_runCycle, m_ref->m_runCycle, s));

    m_ref->m_condition.notify_one();
}


