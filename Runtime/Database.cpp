#include "Database.h"
#include "INodeContext.h"
#include "NodeDb.h"
//#include "Database.h"
#include "Trace.h"
#include "algorithm"
#include "string"
#include <list>

namespace Arro
{

    /**
     * \brief Class instance represents one input for a node.
     *
     * InputPad is created when calling registerNodeInput for a node.
     * Connect multiple InputPad objects to one OutputPad.
     */

    class DbRecord {
        friend DatabaseImpl;
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

        long getId() {
            return m_id;
        }

        void getMessage(MessageBuf& msg) {
            msg = m_msg;
        }

        unsigned int getPadId() {
            return m_padId;
        }

        long getUpdateRef() {
            return m_updateRef;
        }

        void setUpdateRef(long r) {
            m_updateRef = r;
        }

        // Copy and assignment is not supported.
        //DbRecord(const DbRecord&) = delete;
        //DbRecord& operator=(const DbRecord& other) = delete;


    private:
        // Index where this record is/will be stored
        long m_id;

        // Back reference to index.
        long m_updateRef;

        unsigned int m_padId;
        unsigned int m_updateRc;
        unsigned int m_creationRc;
        unsigned int m_deletionRc;
        MessageBuf m_msg;

    public:
        static const long DbEmpty = -1;
    };


    class DbUpdate {
    public:
        DbUpdate():
        m_index{DbRecord::DbEmpty} {
        }
        // Index where this record is/will be stored in index
        long m_index;
    };



    /**
     * \brief Message database.
     *
     * Message database into which to store all messages.
     */
    class DatabaseImpl {

    public:

        /**
         * The actual storage of the records (DbRecord). There are 2 tables:
         * - Table of records
         * - Table of references (DbUpdate) to records, where any update to a records
         *   results in its reference moved to the end of the table. This
         *   table will be used for reading the latest updates on DB records.
         *
         * The only mechanism to add/update/delete records is by setRecord.
         */
        class Store {
            friend class Iterator;
        public:
            Store(std::function<void (long*)> update):
                m_trace{"Store", true},
                m_dbCount{0},
                m_journalCount{0},
                m_update{update}
            {
                m_db = new DbRecord[max_buffer];
                m_journal = new DbUpdate[max_buffer];

                // Store one empty element in journal, since iterators are assumed to be on
                // last found element
                m_journal[0].m_index = DbRecord::DbEmpty;
                m_journalCount = 1;

            };
            ~Store()
            {
                delete[] m_db;
                delete[] m_journal;
            }



            void cleanJournal() {
                m_trace.println("Clean Journal");

                long* shiftList = new long[m_journalCount];

                long shift = 0;
                long i = 0;
                while (i + shift < m_journalCount) {
                    shiftList[i + shift] = i;
                    if(m_journal[i + shift].m_index == DbRecord::DbEmpty) {
                        shift++;
                    }
                    else {
                        m_journal[i].m_index = m_journal[i + shift].m_index;
                        m_db[m_journal[i].m_index].m_updateRef = i;
                        m_journal[i + shift].m_index = DbRecord::DbEmpty;
                        i++;
                    }
                }
                m_journalCount = i;
                m_update(shiftList);

                free((void*)shiftList);
            }

            // Update index: each and every record update gets stored in update index, in the order
            // that the updates happened.
            void updateJournal(long& up, long pos) {
                // get current position of reference in m_up, this must be cleared
                if(up != DbRecord::DbEmpty) {
                    m_journal[up].m_index = DbRecord::DbEmpty;
                }
                if(m_journalCount >= max_buffer - 1) {
                    cleanJournal();
                    //throw std::runtime_error("Database index is full!");
                }
                up = m_journalCount;
                m_journal[m_journalCount++].m_index = pos;
            }

            // Update a db record.
            // TODO: it should not overwrite since we want an immutable db.
            void setRecord(DbRecord& rec) {
                // get position where this record should be stored
                long pos = rec.getId();

                updateJournal(m_db[pos].m_updateRef, pos);

                m_db[pos] = rec;

                m_trace.println("Record conn " + std::to_string(rec.getPadId()) + " stored at " + std::to_string(pos) + " index " + std::to_string(m_journalCount - 1));
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
            DbUpdate* m_journal;
            long m_dbCount;
            long m_journalCount;
            std::function<void (long* shiftList)> m_update;
            static const int max_buffer = 20;
        };

    public:
        DatabaseImpl():
            m_trace{"Database", false},
            m_runCycle{1},
            m_store{ [this](long* shiftList) { for(auto it = m_iterators.begin(); it != m_iterators.end(); ++it) (*it)->update(shiftList); } }
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
                m_store.setRecord(*it);
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
        Store m_store;
        std::list<DbRecord> m_new;

    public:
        // Updating and waiting for condition variable must be done under mutex
        std::mutex m_mutex;

        // Condition variable to awaken run-cycle thread when a database update happened
        std::condition_variable m_condition;

        std::list<Iterator*> m_iterators;

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
Database::getDbLock() {
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
    m_trace.println("New iterator "/* + input->m_interfaceName*/);
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
    m_mode{mode},
    m_conns{conns},
    m_journalIt{0},
    m_writeIt{-1}  // nothing stored yet
{
    m_ref->m_iterators.push_back(this);
};


Iterator::~Iterator() {
    m_ref->m_iterators.remove(this);
};

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

    for(auto it = m_conns.begin(); it != m_conns.end(); ++it) {
        m_trace.println("Conn " + std::to_string(*it));
    }

    // m_journalIt must point to element being processed, so first look ahead..
    while(m_journalIt + 1 < m_ref->m_store.m_journalCount) {
        m_journalIt++;

        if(m_ref->m_store.m_journal[m_journalIt].m_index == DbRecord::DbEmpty) {
            // skip
        } else {
            m_writeIt = m_ref->m_store.m_journal[m_journalIt].m_index;
            DbRecord r = m_ref->m_store.m_db[m_writeIt];
            m_trace.println("Record checking " + std::to_string(m_writeIt) + " index " + std::to_string(m_journalIt) + " pad " + std::to_string(r.getPadId()));
            if(std::find(m_conns.begin(), m_conns.end(), r.getPadId()) != m_conns.end()) {
                r.getMessage(msg);
                m_trace.println("Record found at " + std::to_string(m_writeIt) + " index " + std::to_string(m_journalIt));
               return true;
            }
        }
    }
    if(m_journalIt + 1 == m_ref->m_store.m_journalCount) {
        m_trace.println("No more messages found");
        return false;
    }
    return false;
}


void
Iterator::insertRecord(google::protobuf::MessageLite& msg) {
    m_trace.println("Inserting message msg");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    // get the (only) padId of this output pad
    unsigned int padId = m_conns.front();

    MessageBuf s(new std::string(msg.SerializeAsString()));
    m_writeIt = m_ref->m_store.getFree();
    m_ref->m_new.push_back(DbRecord(m_writeIt, padId, m_ref->m_runCycle, 0, s));

    m_ref->m_condition.notify_one();
}

void
Iterator::insertRecord(MessageBuf& msg) {
    m_trace.println("Inserting message buf");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    // get the (only) padId of this output pad
    unsigned int padId = m_conns.front();

    m_writeIt = m_ref->m_store.getFree();
    m_ref->m_new.push_back(DbRecord(m_writeIt, padId, m_ref->m_runCycle, 0, msg));

    m_ref->m_condition.notify_one();
}

void
Iterator::updateRecord(google::protobuf::MessageLite& msg) {
    m_trace.println("Updating message");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    MessageBuf s(new std::string(msg.SerializeAsString()));
    unsigned int padId = m_conns.front();
    m_ref->m_new.push_back(DbRecord(m_writeIt, padId, m_ref->m_runCycle, 0, s));

    m_ref->m_condition.notify_one();
}

void
Iterator::updateRecord(MessageBuf& msg) {
    m_trace.println("Updating message");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    unsigned int padId = m_conns.front();
    m_ref->m_new.push_back(DbRecord(m_writeIt, padId, m_ref->m_runCycle, 0, msg));

    m_ref->m_condition.notify_one();
}

void
Iterator::deleteOutput() {
    m_trace.println("Deleting message");

    std::lock_guard<std::mutex> lock(m_ref->m_mutex);

    MessageBuf s(nullptr);
    unsigned int padId = m_conns.front();
    m_ref->m_new.push_back(DbRecord(m_writeIt, padId, m_ref->m_runCycle, m_ref->m_runCycle, s));

    m_ref->m_condition.notify_one();
}

void
Iterator::update(long* shiftList) {
    //m_journalIt = m_ref->m_store.m_db[m_writeIt].getUpdateRef();
    m_journalIt = shiftList[m_journalIt];
}


