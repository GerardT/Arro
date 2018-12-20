#include "Database.h"
#include "INodeContext.h"
#include "NodeDb.h"
//#include "Database.h"
#include "Trace.h"
#include "algorithm"
#include "string"

namespace Arro
{

    /**
     * \brief Message database.
     *
     * Message database into which to store all messages.
     */
    class DatabaseImpl {

    public:
        class Store; // forward declaration



        class DbUpdate {
        public:
            DbUpdate():
            index{DbRecord::DbEmpty} {
            }
            long index;
        };

        /**
         * \brief Class instance represents one input for a node.
         *
         * InputPad is created when calling registerNodeInput for a node.
         * Connect multiple InputPad objects to one OutputPad.
         */

        class DbRecord {
            friend class DatabaseImpl::Store;
        public:
            /**
             * Constructor for (addressable) message container.
             *
             * \param o OutputPad instance where to send this message to.
             * \param s Message buffer to send.
             */
            DbRecord(long index, unsigned int padId, unsigned int runCycle, unsigned int deletionRc, const MessageBuf& s):
                m_id{index},
                m_updateRef{DbEmpty},
                m_padId{padId},
                m_updateRc{runCycle},
                m_creationRc{runCycle},
                m_deletionRc{deletionRc},
                m_msg{s} {};
            DbRecord():
                m_id{DbEmpty},
                m_updateRef{DbEmpty},
                m_padId{0},
                m_updateRc{0},
                m_creationRc{0},
                m_deletionRc{0},
                m_msg{MessageBuf{nullptr}} {};
            virtual ~DbRecord() {};

            unsigned int getId() {
                return m_id;
            }

            void getMessage(MessageBuf& msg) {
                msg = m_msg;
            }

            unsigned int getPadId() {
                return m_padId;
            }


            // Copy and assignment is not supported.
            //DbRecord(const DbRecord&) = delete;
            //DbRecord& operator=(const DbRecord& other) = delete;


        private:
            // unique id (think ROWID) for this record
            long m_id;
            long m_updateRef;

            unsigned int m_padId;
            unsigned int m_updateRc;
            unsigned int m_creationRc;
            unsigned int m_deletionRc;
            MessageBuf m_msg;

        public:
            static const long DbEmpty = -1;
        };

        /**
         * Database, with index based on updateRc.
         */
        class Store {
            friend class Iterator;
        public:
            Store():
                m_trace{"Store", true},
                m_dbCount{0},
                m_upCount{0}
            {
                m_db = new DbRecord[max_buffer];
                m_up = new DbUpdate[max_buffer];
            };
            ~Store()
            {
                delete[] m_db;
                delete[] m_up;
            }

            void setRecord(DbRecord& rec) {
                long pos = rec.m_id;
                long up = m_db[pos].m_updateRef;
                if(up != DbRecord::DbEmpty) {
                    m_up[up].index = DbRecord::DbEmpty;
                }
                if(m_upCount >= max_buffer - 1) {
                    throw std::runtime_error("Database index is full!");
                }
                rec.m_updateRef = m_upCount;
                m_up[m_upCount++].index = pos;

                m_db[pos] = rec;

                m_trace.println("Record conn " + std::to_string(rec.m_padId) + " stored at " + std::to_string(pos) + " index " + std::to_string(m_upCount - 1));
            }

            unsigned int getFree() {
                if(m_dbCount >= max_buffer - 1) {
                    throw std::runtime_error("Database is full!");
                }
                return m_dbCount++;
            }

            void purge() {
                long ix = 0;
                while(ix != m_dbCount) {
    //                if(m_db.at(it)->m_runCycle != m_runCycle) {
    //                    //it = m_db.erase(it);
    //                }
    //                else {
    //                    ++it;
    //                }
                    ix++;
                }
            }
        private:
            Trace m_trace;
            DbRecord* m_db;
            DbUpdate* m_up;
            long m_dbCount;
            long m_upCount;
            static const int max_buffer = 100;
        };

    public:
        DatabaseImpl():
            m_trace{"Database", false},
            m_runCycle{1}
        {
        };

        ~DatabaseImpl() {
        }

        // Copy and assignment is not supported.
        DatabaseImpl(const DatabaseImpl&) = delete;
        DatabaseImpl& operator=(const DatabaseImpl& other) = delete;


        unsigned int getCurrentRunCycle() {
            return m_runCycle;
        }

        INodeContext::ItRef begin(InputPad* input, unsigned int connection, INodeContext::Mode mode);
        INodeContext::ItRef end(OutputPad* input, unsigned int connection);

        /**
         * Swap (full) input queue and (empty) output queue.
         */
        const std::list<unsigned int> incRunCycle() {
            m_runCycle++;

            std::list<unsigned int> pads;
            for(auto it = m_new.begin(); it != m_new.end(); ++it) {
                pads.push_back(it->getPadId());
                m_db.setRecord(*it);
            }
            m_new.clear();

            //purge();
            return pads;
        }

        bool noMoreUpdates() {
            return m_new.empty();
        }

        std::mutex& getMutex() {
            return m_mutex;
        }
        std::condition_variable& getConditionVariable() {
            return m_condition;
        }



    public:
        Trace m_trace;
        unsigned int m_runCycle; // current run cycle
        Store m_db;
        std::list<DbRecord> m_new;

    public:
        // Updating and waiting for condition variable must be done under mutex
        std::mutex m_mutex;

        // Condition variable to awaken run-cycle thread when a database update happened
        std::condition_variable m_condition;

    };
}


using namespace std;
using namespace Arro;


Database::Database() {
    if(!m_db) {
        m_db = new DatabaseImpl();
    };
};

Database::~Database() {
    delete m_db;
    m_db = nullptr;
}

unsigned int
Database::getCurrentRunCycle() {
    return m_db->getCurrentRunCycle();
}

INodeContext::ItRef
Database::begin(InputPad* input, unsigned int connection, INodeContext::Mode mode) {
    return m_db->begin(input, connection, mode);
}

INodeContext::ItRef
Database::end(OutputPad* input, unsigned int connection) {
    return m_db->end(input, connection);
}

std::mutex&
Database::getMutex() {
    return m_db->getMutex();
}
std::condition_variable&
Database::getConditionVariable() {
    return m_db->getConditionVariable();
}

bool
Database::noMoreUpdates() {
    return m_db->noMoreUpdates();
}

/**
 * Swap (full) input queue and (empty) output queue.
 */
const std::list<unsigned int>
Database::incRunCycle() {
    return m_db->incRunCycle();
}




INodeContext::ItRef
DatabaseImpl::begin(InputPad* input, unsigned int connection, INodeContext::Mode mode) {
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
DatabaseImpl::end(OutputPad* /*input*/, unsigned int connection) {
    m_trace.println("New iterator ");

    // build a list with only one padId, the padId of the output pad.
    std::list<unsigned int> conn;
    conn.push_back(connection);

    return INodeContext::ItRef(new Iterator(this, INodeContext::LATEST, conn));
}



Iterator::Iterator(DatabaseImpl* db, INodeContext::Mode mode, const std::list<unsigned int>& conns):
    m_trace{"Iterator", false},
    m_ref{db},
    //m_db{db},
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

    while(m_it < m_ref->m_db.m_upCount) {
        if(m_ref->m_db.m_up[m_it].index == DatabaseImpl::DbRecord::DbEmpty) {
            // skip
        } else {
            unsigned int ix = m_ref->m_db.m_up[m_it].index;
            DatabaseImpl::DbRecord r = m_ref->m_db.m_db[ix];
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
    if(m_it == m_ref->m_db.m_upCount) {
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
    m_it = m_ref->m_db.getFree();
    m_ref->m_new.push_back(DatabaseImpl::DbRecord(m_it, padId, m_ref->m_runCycle, 0, s));

    m_ref->m_condition.notify_one();
}

void
Iterator::insertOutput(MessageBuf& msg) {
    m_trace.println("Inserting message buf");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    // get the (only) padId of this output pad
    unsigned int padId = m_conns.front();

    m_it = m_ref->m_db.getFree();
    m_ref->m_new.push_back(DatabaseImpl::DbRecord(m_it, padId, m_ref->m_runCycle, 0, msg));

    m_ref->m_condition.notify_one();
}

void
Iterator::updateOutput(google::protobuf::MessageLite& msg) {
    m_trace.println("Updating message");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    MessageBuf s(new std::string(msg.SerializeAsString()));
    unsigned int padId = m_conns.front();
    m_ref->m_new.push_back(DatabaseImpl::DbRecord(m_it, padId, m_ref->m_runCycle, 0, s));

    m_ref->m_condition.notify_one();
}

void
Iterator::updateOutput(MessageBuf& msg) {
    m_trace.println("Updating message");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    unsigned int padId = m_conns.front();
    m_ref->m_new.push_back(DatabaseImpl::DbRecord(m_it, padId, m_ref->m_runCycle, 0, msg));

    m_ref->m_condition.notify_one();
}

void
Iterator::deleteOutput() {
    m_trace.println("Deleting message");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    MessageBuf s(nullptr);
    unsigned int padId = m_conns.front();
    m_ref->m_new.push_back(DatabaseImpl::DbRecord(m_it, padId, m_ref->m_runCycle, m_ref->m_runCycle, s));

    m_ref->m_condition.notify_one();
}


