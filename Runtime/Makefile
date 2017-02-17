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
INCPATH       = -I"." -I"Nodes" -I"/usr/local/include"
LINKER        = g++
# LFLAGS        = -g -Wl,-subsystem		--> -Wl,-subsystem is MinGW stuff, results in no symbols.
LFLAGS        = -g
LIBS          = -L/usr/local/lib -lprotobuf `python2.7-config --ldflags`
DEL_FILE      = rm
OBJDIR       := debug
SRCDIR       := ../Runtime
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
		PythonGlue.cpp \
		Parser.cpp \
		Tokenizer.cpp \
		Nodes/NodeSfc.cpp \
		Nodes/NodePid.cpp \
		Nodes/NodePython.cpp \
		Nodes/NodeTimer.cpp \
		Nodes/NodeServo.cpp \
		Nodes/NodeDCMotor.cpp \
		Nodes/NodeUiOut.cpp \
		tinyxmlparser.cpp \
		tinyxmlerror.cpp \
		tinyxml.cpp \
		tinystr.cpp

OBJS         := $(addprefix $(OBJDIR)/,$(SRCS:.cpp=.o))
SRCS         := $(addprefix $(SRCDIR)/,$(SRCS))

-include $(OBJS:.o=.d)

.PHONY: all clean

all: $(OBJDIR)/arro.pb.o $(OBJS)
	$(CXX) -o Arro $(LFLAGS) $(OBJS) $(LIBS) 

$(OBJDIR)/%.o: $(SRCDIR)/%.cpp
	$(CXX) -MMD -c $(CXXFLAGS) $(INCPATH) -o $@ $<

$(OBJDIR)/arro.pb.o: $(SRCDIR)/arro.proto
	protoc --cpp_out=. arro.proto
	$(CXX) -MMD -c $(CXXFLAGS) $(INCPATH) -o $(OBJDIR)/arro.pb.o arro.pb.cc

clean:
	rm -rf debug


