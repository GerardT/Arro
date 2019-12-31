#
# sudo apt-get install python2.7-dev to get python2.7-config
# ./protobuf/python/
# python setup.py build
# python setup.py test
# sudo python setup.py install
#
#
CC            = gcc
CXX           = g++
HOST          = $(shell hostname)
PYTHON_VER    = 2
ifeq ($(HOST), raspberrypi)
DEFINES       = -DUNICODE -DRPI
else
DEFINES       = -DUNICODE
endif

ifeq ($(PYTHON_VER), 3)
DEFINES       +=  -DPYTHON3
CFLAGS        = -pipe -Wall -Wextra $(DEFINES) `python3-config --cflags`
CXXFLAGS      = -fPIC -pipe -std=c++0x -g -frtti -Wall -Wextra -fexceptions $(DEFINES) `python3-config --cflags`
LIBS          = -L/usr/local/lib -lprotobuf `python3-config --ldflags`
else
CFLAGS        = -pipe -Wall -Wextra $(DEFINES) `python2.7-config --cflags`
CXXFLAGS      = -fPIC -pipe -std=c++0x -g -frtti -Wall -Wextra -fexceptions $(DEFINES) `python2.7-config --cflags`
LIBS          = -L/usr/local/lib -lprotobuf `python2.7-config --ldflags`
endif

# INCPATH       = -I"." -I"../../protobuf-2.5.0/src"
INCPATH       = -I"." -I"/usr/local/include" -I"../../download"
LINKER        = g++
# LFLAGS        = -g -Wl,-subsystem		--> -Wl,-subsystem is MinGW stuff, results in no symbols.
LFLAGS        = -g
DEL_FILE      = rm

OBJDIR       := debug
SRCDIR       := ../Runtime
NODESRCDIR   := ../Runtime/Nodes
DWLDIR       := ../../download

MKDIR_P      := mkdir -p
_dummy1      := $(shell mkdir -p debug)
_dummy2      := $(shell mkdir -p debug/Nodes)

DEPS         := $(SRCS:.cpp=.d)
-include $(DEPS)

SRVRSRCS := ServerEngine.cpp \
		Trace.cpp \
		SocketClient.cpp

SRCS  = main.cpp \
		Trace.cpp \
		SocketClient.cpp \
		ConfigReader.cpp \
		NodeDb.cpp \
		Pad.cpp \
		Process.cpp \
		Database.cpp \
		Nodes/NodePython.cpp \
		PythonGlue.cpp \
		tinyxmlparser.cpp \
		tinyxmlerror.cpp \
		tinyxml.cpp \
		tinystr.cpp

NODESRCS := \
		arro.pb.cpp \
		NodeSfc.cpp \
		NodePid.cpp \
		NodeTimer.cpp \
		NodeServo.cpp \
		NodeEsc.cpp \
		NodePulseWidthModulator.cpp \
		NodeDCMotor.cpp \
		NodeStepperMotor.cpp \
		MotorHat.cpp \
		NodeUiCheckBox.cpp \
		NodeUiRadioButton.cpp \
		NodeUiToggleButton.cpp \
		NodeUiUserInput.cpp \
		NodeUiUserDisplay.cpp \
		NodeUiMap.cpp \
		NodeFileReader.cpp \
		NodeTsReader.cpp \
		NodeTsSubtable.cpp \
		NodeDumper.cpp \

OBJS         := $(addprefix $(OBJDIR)/,$(SRCS:.cpp=.o))
SRCS         := $(addprefix $(SRCDIR)/,$(SRCS))

NODEOBJS     := $(addprefix $(OBJDIR)/,$(NODESRCS:.cpp=.o))
NODESRCS     := $(addprefix $(NODESRCDIR)/,$(NODESRCS))

SRVROBJS     := $(addprefix $(OBJDIR)/,$(SRVRSRCS:.cpp=.o))
SRVRSRCS     := $(addprefix $(SRCDIR)/,$(SRVRSRCS))

-include $(OBJS:.o=.d)

.PHONY: all clean core nodes server

all: core nodes server
	
core: $(OBJDIR)/CodeGenerator.o $(OBJS)
	$(CXX) -o Core $(LFLAGS) $(OBJDIR)/CodeGenerator.o $(OBJS) $(LIBS) 

nodes: $(OBJDIR)/arro.pb.o $(NODEOBJS)
	$(CXX) -fPIC -shared $(LFLAGS) -Wl,-soname,libnodes.so -o libnodes.so $(NODEOBJS) $(LIBS)

server: $(SRVROBJS)
	$(CXX) -o Arro $(LFLAGS) $(SRVROBJS) $(LIBS) 

$(OBJDIR)/%.o: $(SRCDIR)/%.cpp
	$(CXX) -MMD -c $(CXXFLAGS) $(INCPATH) -o $@ $<
	
$(OBJDIR)/%.o: $(NODESRCDIR)/%.cpp
	$(CXX) -fPIC -MMD -c $(CXXFLAGS) $(INCPATH) -o $@ $<
	
$(OBJDIR)/lemon: $(SRCDIR)/lemon/lemon.c
	$(CC) -g $(INCPATH) -o $(OBJDIR)/lemon $(SRCDIR)/lemon/lemon.c

$(OBJDIR)/CodeGenerator.o $(SRCDIR)/lemon/cfg.h:	$(OBJDIR)/lemon $(SRCDIR)/lemon/CodeGenerator.cpp $(SRCDIR)/lemon/cfg.y
	cd lemon; \
	../$(OBJDIR)/lemon ../$(SRCDIR)/lemon/cfg.y
	$(CXX) -MMD -c $(CXXFLAGS) $(INCPATH) -o $(OBJDIR)/CodeGenerator.o $(SRCDIR)/lemon/CodeGenerator.cpp


#doc: lemon_notes.txt
#	asciidoc $<


$(OBJDIR)/arro.pb.o: $(DWLDIR)/arro.proto
	protoc --cpp_out=$(DWLDIR)  --proto_path=$(DWLDIR) $(DWLDIR)/arro.proto
	$(CXX) -MMD -c $(CXXFLAGS) $(INCPATH) -o $(OBJDIR)/arro.pb.o $(DWLDIR)/arro.pb.cc

clean:
	rm -rf debug
	rm -f lemon/lemon lemon/cfg.c lemon/cfg.h lemon/cfg.out


