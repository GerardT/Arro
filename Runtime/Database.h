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

        /**
         * \brief Class instance represents one input for a node.
         *
         * InputPad is created when calling registerNodeInput for a node.
         * Connect multiple InputPad objects to one OutputPad.
         */

        class DbRecord {
        public:
            /**
             * Constructor for (addressable) message container.
             *
             * \param o OutputPad instance where to send this message to.
             * \param s Message buffer to send.
             */
            DbRecord(unsigned int padId, unsigned int runCycle, const MessageBuf& s):
                m_padId{padId},
                m_runCycle{runCycle},
                m_msg{s} {};
            DbRecord():
                m_padId{0},
                m_runCycle{0},
                m_msg{MessageBuf{nullptr}} {};
            virtual ~DbRecord() {};

            // Copy and assignment is not supported.
            //DbRecord(const DbRecord&) = delete;
            //DbRecord& operator=(const DbRecord& other) = delete;


            // FIXME Should be private
            unsigned int m_padId;
            unsigned int m_runCycle;
            MessageBuf m_msg;
        };
        class Store {
        public:
            Store():
                m_db{nullptr},
                m_dbCount{0}
            {
                m_db = new DbRecord[max_buffer];
            };
            ~Store()
            {
                delete[] m_db;
            }
            unsigned int begin() { return 0; }
            unsigned int end() { return m_dbCount; };

            const DbRecord* at(unsigned int i) { return &(m_db[i]); };

            void push_back(DbRecord buf) {
                if(m_dbCount == max_buffer) {
                    throw std::runtime_error("Database is full!");
                }
                m_db[m_dbCount++] = buf;
            }

            unsigned int erase(unsigned int it)
            {
                return it;
                // TODO
            }

        private:
            DbRecord* m_db;
            unsigned int m_dbCount;
            static const int max_buffer = 10000;
        };

    public:
        Database():
            m_trace{"Database", false},
            m_runCycle{0}
        {
        };

        // Copy and assignment is not supported.
        Database(const Database&) = delete;
        Database& operator=(const Database& other) = delete;


        void store(unsigned int padId, MessageBuf& buf) {
            m_trace.println("Storing message for " + std::to_string(padId));
            m_db.push_back(DbRecord(padId, m_runCycle, buf));
        }

        INodeContext::ItRef getFirst(InputPad* input, unsigned int connection, INodeContext::Mode mode);

        /**
         * Swap (full) input queue and (empty) output queue.
         */
        void incRunCycle() {
            m_runCycle++;

            //purge();
        }

    private:
        void purge() {
            auto it = m_db.begin();
            while(it != m_db.end()) {
                if(m_db.at(it)->m_runCycle != m_runCycle) {
                    it = m_db.erase(it);
                }
                else {
                    ++it;
                }
            }
        }


    private:
        Trace m_trace;
        unsigned int m_runCycle; // current run cycle
        Store m_db;

    };

    class Iterator: public INodeContext::ItRef {
    public:

        Iterator(Database* db, INodeContext::Mode mode, unsigned int rc, const std::list<unsigned int>& conns);
        virtual ~Iterator();
        virtual bool getNext(MessageBuf& msg);

    private:
        Trace m_trace;
        Database* m_ref;

        // Search criteria
        INodeContext::Mode m_mode;
        unsigned int m_runCycle;
        const std::list<unsigned int> m_conns;

        // Current position
        unsigned int m_it;
    };
}


#endif
