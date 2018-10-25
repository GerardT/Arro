#ifndef ARRO_DATABASE_DB_H
#define ARRO_DATABASE_DB_H


#include <stdexcept>
#include <thread>

#include <google/protobuf/message.h>
#include <list>
#include <condition_variable>
#include <memory>
#include <functional>

#include "INodeDefinition.h"
#include "INodeContext.h"
#include "Trace.h"


namespace Arro
{
class Database;
class Iterator;
    /**
     * \brief Message database.
     *
     * Message database into which to store all messages.
     */
    class Database {
    friend class Iterator;

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
            friend class Database::Store;
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
        Database():
            m_trace{"Database", false},
            m_runCycle{1}
        {
        };

        ~Database() {
        }

        // Copy and assignment is not supported.
        Database(const Database&) = delete;
        Database& operator=(const Database& other) = delete;


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


    private:
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

    class Iterator: public INodeContext::Iterator {
    public:

        Iterator(Database* db, INodeContext::Mode mode, const std::list<unsigned int>& conns);
        virtual ~Iterator();
        virtual bool getNext(MessageBuf& msg);
        virtual void insertOutput(google::protobuf::MessageLite& msg);
        virtual void insertOutput(MessageBuf& msg);
        virtual void updateOutput(google::protobuf::MessageLite& msg);
        virtual void updateOutput(MessageBuf& msg);
        virtual void setOutput(google::protobuf::MessageLite& msg) {
            if(empty()) {
                insertOutput(msg);
            } else {
                updateOutput(msg);
            }
        };
        virtual void setOutput(MessageBuf& msg) {
            if(empty()) {
                insertOutput(msg);
            } else {
                updateOutput(msg);
            }
        };
        virtual void deleteOutput();
        virtual bool empty() {
            return m_it == 0;
        }

    private:
        Trace m_trace;
        Database* m_ref;
        Database::Store& m_db;

        // Search criteria
        INodeContext::Mode m_mode;

        const std::list<unsigned int> m_conns;

        // Current position
        long int m_it;

        bool m_currentEmpty;
    };
}


#endif
