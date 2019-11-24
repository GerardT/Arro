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
class DatabaseImpl;
class Iterator;


    /**
     * Facade class for database implementation
     */
    class Database {
    friend Iterator;
    public:

        Database();

        ~Database();

        // Copy and assignment is not supported.
        Database(const Database&) = delete;
        Database& operator=(const Database& other) = delete;

        /**
         * Get database lock.
         *
         * @return
         */
        std::mutex& getDbLock();


        std::condition_variable& getConditionVariable();


        unsigned int getCurrentRunCycle();

        /**
         * Return iterator for input pad.
         *
         * @param input
         * @param connection: padId of the connected output pad. So only records
         *                    marked with that padId as origin. If 0 than padId not considered.
         * @param mode: ALL, DELTA, LATEST. Currently only DELTA supported.
         * @return
         */
        INodeContext::ItRef begin(InputPad* input, unsigned int connection, INodeContext::Mode mode);

        /**
         * Return iterator for output pad. Essentially used to (over) write data to output.
         *
         * @param input
         * @param connection
         * @return
         */
        INodeContext::ItRef end(OutputPad* input, unsigned int connection);

        /**
         * Swap (full) input queue and (empty) output queue.
         */
        const std::list<unsigned int> incRunCycle();

        /**
         * Indicates that the previous Run cycle produced no more outputs (that would
         * require yet another Run cycle). So system can go to sleep.
         * @return
         */
        bool noMoreUpdates();

    private:
        DatabaseImpl* m_db = nullptr;

    };




    class Iterator: public INodeContext::Iterator {
    public:

        Iterator(DatabaseImpl* db, INodeContext::Mode mode, const std::list<unsigned int>& conns);
        virtual ~Iterator();
        virtual bool getNext(MessageBuf& msg);
        virtual void insertRecord(google::protobuf::MessageLite& msg);
        virtual void insertRecord(MessageBuf& msg);
        virtual void updateRecord(google::protobuf::MessageLite& msg);
        virtual void updateRecord(MessageBuf& msg);
        virtual void setRecord(google::protobuf::MessageLite& msg) {
            if(empty()) {
                insertRecord(msg);
            } else {
                updateRecord(msg);
            }
        };
        virtual void setRecord(MessageBuf& msg) {
            if(empty()) {
                insertRecord(msg);
            } else {
                updateRecord(msg);
            }
        };
        virtual void deleteRecord();
        virtual bool empty() {
            return m_rowId == -1;
        }
        virtual void update(long* shiftList);

    private:
        Trace m_trace;
        DatabaseImpl* m_ref;

        // Search criteria
        INodeContext::Mode m_mode;

        const std::list<unsigned int> m_conns;

        // Current position
        long int m_journalRowId;
        long int m_rowId;
    };
}


#endif
