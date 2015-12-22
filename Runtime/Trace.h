#ifndef ARRO_TRACE_H
#define ARRO_TRACE_H
#include <iostream>
#include <streambuf>
#include <string>
#include <tinyxml.h>

namespace Arro {

    /**
     * \brief Support for debug traces over serial output.
     *
     * One instance to be added to each class.
     * Trace class will add the class name to each line that is printed.
     *
     * It is fairly simple right now but defined to allow for future additions.
     */
    class Trace {
    public:
        /**
         * Constructor
         * \param c Name of the class, this is shown in output.
         * \param on Switch if serial output is enabled for this instance.
         */
        Trace(const std::string& c, bool on) {
            cl = c;
            out = on;
        }

        virtual ~Trace() {};

        // Copy and assignment is not supported.
        Trace(const Trace&) = delete;
        Trace& operator=(const Trace& other) = delete;

        /**
         * Print string to output.
         *
         * \param s String to be printed.
         */
        void println(const std::string& s, int err) const {
            if(out == true) {
                std::cout << cl << " ==> " << s << " ==> " << err  << std::endl;
            }
        }
        void println(const std::string& s) const {
            if(out == true) {
                std::cout << cl << " ==> " << s << std::endl;
            }
        }

        /**
         * Fatal error - string is printed and sw exits.
         *
         * \param s String to be printed.
         */
        void fatal(const std::string& s, int err) const {
            std::cout << cl << " ==> " << s << " ==> " << err << std::endl;
            exit(0);
        }
        void fatal(const std::string& s) const {
            std::cout << cl << " ==> " << s << std::endl;
            exit(0);
        }

        /**
         * Special version: print a XML node.
         *
         * \params s String to be printed
         * \param node XML node to be printed as well.
         */
        void println(const std::string& s, TiXmlElement* node) const {
            if(out == true) {
                std::cout << cl << " ==> " << s << node << std::endl;
            }
        }

    private:
        bool out;
        std::string cl;
    };
}

#endif
