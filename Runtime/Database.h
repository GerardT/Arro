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
#include "Trace.h"


namespace Arro
{

    /**
     * \brief Message database.
     *
     * Message database into which to store all messages.
     */
    class Database {

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
            DbRecord(unsigned int nodeId, unsigned int runCycle, const MessageBuf& s):
                m_nodeId{nodeId},
                m_runCycle{runCycle},
                m_msg{s} {};
            virtual ~DbRecord() {};

            // Copy and assignment is not supported.
            //DbRecord(const DbRecord&) = delete;
            //DbRecord& operator=(const DbRecord& other) = delete;


            // FIXME Should be private
            unsigned int m_nodeId;
            unsigned int m_runCycle;
            MessageBuf m_msg;
        };

    public:
        Database():
            m_trace{"Database", true},
            m_runCycle{0}
        {};

        ~Database() {
        }

        // Copy and assignment is not supported.
        Database(const Database&) = delete;
        Database& operator=(const Database& other) = delete;

        void store(unsigned int nodeId, MessageBuf& buf) {
            m_db.push_back(DbRecord(nodeId, m_runCycle, buf));
        }

        bool getLatest(unsigned int nodeId, MessageBuf& buf) const {
            for(std::list<DbRecord>::const_reverse_iterator it = m_db.rbegin(); it!= m_db.rend(); ++it ) {  // reverse iterator
                if(it->m_nodeId == nodeId) {
                    buf = it->m_msg;
                    return true;
                }
            }
            return false;
        }
        bool getLatest(MessageBuf& buf) const {
            for(std::list<DbRecord>::const_reverse_iterator it = m_db.rbegin(); it!= m_db.rend(); ++it ) {  // reverse iterator
                buf = it->m_msg;
                return true;
            }
            return false;
        }

        /**
         * Swap (full) input queue and (empty) output queue.
         */
        void incRunCycle() {
            m_runCycle++;

            purge();
        }

        void visitMessages(std::function<void(const MessageBuf&)> f) {
            for(auto const& it : m_db) {
                f(it.m_msg);
            }

        }
    private:
        void purge() {
            auto it = m_db.begin();
            while(it != m_db.end()) {
                if(it->m_runCycle != m_runCycle) {
                    it = m_db.erase(it);
                }
                else {
                    ++it;
                }
            }
        }

    private:
        Trace m_trace;
        std::list<DbRecord> m_db;
        unsigned int m_runCycle; // current run cycle

    };
}

#endif
