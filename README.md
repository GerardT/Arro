# Arro
Soft PLC implementation with Eclipse plugin as client and (Raspberry) server. For simple robotics.

System consists of
- client sw 'Arro' - this is an Eclipse plugin using Java, XML, and connected to server over TCP/IP
- server sw 'Runtime' - this should run on Raspberry Pi or other such systems, using C++, Protocol Buffers, XML, Python.

Goal is to provide sort of a Soft PLC environment that has low threshold so might be used by anyone. Building blocks in this PLC are:
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


