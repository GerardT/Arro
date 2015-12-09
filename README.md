# Arro
Soft PLC implementation with Eclipse plugin as client and (Raspberry) server. For simple robotics.

System consists of
- client sw 'Arro' - this is an Eclipse plugin using Java, Graphiti (https://eclipse.org/graphiti), XML, and connected to server over TCP/IP
- server sw 'Runtime' - this should run on Raspberry Pi or other such systems, using C++, Protocol Buffers, XML, Python.

Goal is to provide sort of a Soft PLC environment that has easy learning curve so might be used by anyone. Building blocks in this PLC are:
- function blocks - those consists of other blocks (function / code blocks)
- code blocks for native code - written in C / C++.
- code blocks for Python code.
- state/transition diagrams. This is not implemented yet, thinking is to add this to function blocks.
- user interface blocks - plan is to provide HTML building blocks based on web components (Polymer).

User should be able to program the PLC and run it from within Eclipse framework.


System is running with server sw on host (over localhost). Still many details to fix:
- icons
- graphics details.
- language support
- fix the Runtime Makefile (it is bad..)
- etc.


Special thanks to tinyXml:
www.sourceforge.net/projects/tinyxml

=============================================================================
To run

Start Eclipse workbench
Import the Arro/Arro folder containing the Eclipse plugin code:
File->Import->Existing Projects into Workspace->Browse (select Arro/Arro)->Finish
Run it by right-click on Arro project->Run As->Eclipse Application.
When running select the right perspective.
Window->Open Perpective->Other-> select Arro.


=============================================================================
To use
Create Arro project:
File->New->Other->select Arro/Arro Project->Next->Provide name->Finish.
Now open newly created project.

Create your own new message type:
File->New->Other->select Arro/Message Definition->Next->give name->Finish
Open messages folder in Project Explorer and drag existing message types onto right pane.
Provide name etc if needed.

Create e.g. new Python code block:
File->New->Other->select Arro/Python Code Block->Next->give name->Finish
Now drag existing message type onto right pane, inside the rectangle. Those messages will become the input/output pads of this code block. In the Properties tab below the right pane you can set name, input/output, etc. Trigger cycle means that a change on this input will trigger execution of the code block.

Create e.g. a new Function block:
File->New->Other->select Arro/Function Block->Next->give name->Finish
Now drag existing message type onto right pane, inside the rectangle. Those messages will become the input/output pads of this code block. In the Properties tab below the right pane you can set name, input/output, etc. Trigger cycle means that a change on this input will trigger execution of the code block.
Open diagrams in Project Explorer
You can also drag other diagrams from diagrams folder onto right pane. Those diagrams become (sub) components inside the diagram you dragged it into. If the message types on the blocks match, you can make connections between them using 'Create Connection' on right side of workbench.

In order to be able to run you will have to create one top-level Function Block diagram named 'Main'.

=============================================================================
To run Arro project
To Be Done.





