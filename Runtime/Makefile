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
ifeq ($(HOST), raspberrypi)
DEFINES       = -DUNICODE -DRPI
else
DEFINES       = -DUNICODE
endif

CFLAGS        = -pipe -Wall -Wextra $(DEFINES) `python2.7-config --cflags`
CXXFLAGS      = -pipe -std=c++0x -g -frtti -Wall -Wextra -fexceptions $(DEFINES) `python2.7-config --cflags`
# INCPATH       = -I"." -I"../../protobuf-2.5.0/src"
INCPATH       = -I"." -I"/usr/local/include" -I"../../download"
LINKER        = g++
# LFLAGS        = -g -Wl,-subsystem		--> -Wl,-subsystem is MinGW stuff, results in no symbols.
LFLAGS        = -g
LIBS          = -L/usr/local/lib -lprotobuf `python2.7-config --ldflags`
DEL_FILE      = rm
OBJDIR       := debug
SRCDIR       := ../Runtime
DWLDIR       := ../../download
MKDIR_P      := mkdir -p
_dummy1      := $(shell mkdir -p debug)
_dummy2      := $(shell mkdir -p debug/Nodes)

DEPS         := $(SRCS:.cpp=.d)
-include $(DEPS)

SRCS         := main.cpp \
		Trace.cpp \
		ServerEngine.cpp \
		SocketClient.cpp \
		ConfigReader.cpp \
		NodeDb.cpp \
		arro.pb.cpp \
		Pad.cpp \
		Process.cpp \
		Database.cpp \
		PythonGlue.cpp \
		Nodes/NodeSfc.cpp \
		Nodes/NodePid.cpp \
		Nodes/NodePython.cpp \
		Nodes/NodeTimer.cpp \
		Nodes/NodeServo.cpp \
		Nodes/NodeDCMotor.cpp \
		Nodes/NodeStepperMotor.cpp \
		Nodes/MotorHat.cpp \
		Nodes/NodeUiCheckBox.cpp \
		Nodes/NodeUiRadioButton.cpp \
		Nodes/NodeUiToggleButton.cpp \
		Nodes/NodeUiUserInput.cpp \
		Nodes/NodeUiUserDisplay.cpp \
		Nodes/NodeFileReader.cpp \
		Nodes/NodeTsReader.cpp \
		Nodes/NodeTsSubtable.cpp \
		Nodes/NodeDumper.cpp \
		tinyxmlparser.cpp \
		tinyxmlerror.cpp \
		tinyxml.cpp \
		tinystr.cpp

OBJS         := $(addprefix $(OBJDIR)/,$(SRCS:.cpp=.o))
SRCS         := $(addprefix $(SRCDIR)/,$(SRCS))

-include $(OBJS:.o=.d)

.PHONY: all clean

all: $(OBJDIR)/arro.pb.o $(OBJDIR)/CodeGenerator.o $(OBJS)
	$(CXX) -o Arro $(LFLAGS) $(OBJDIR)/CodeGenerator.o $(OBJS) $(LIBS) 

$(OBJDIR)/%.o: $(SRCDIR)/%.cpp
	$(CXX) -MMD -c $(CXXFLAGS) $(INCPATH) -o $@ $<
	
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


