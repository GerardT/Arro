#ifndef INODE_H
#define INODE_H

#include <string>

/**
 * Base class for Pad and Process.
 * The name is the instance name: ".node.subnode.subnode"
 */
class INode {
private:
	std::string name;
public:
    INode(const std::string& name);
    virtual ~INode() {};
    std::string& getName() { return name; };
    virtual void runCycle() = 0;
};


#endif
