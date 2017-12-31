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
INCPATH       = -I"." -I"ElemBlock" -I"/usr/local/include"
LINKER        = g++
# LFLAGS        = -g -Wl,-subsystem		--> -Wl,-subsystem is MinGW stuff, results in no symbols.
LFLAGS        = -g
LIBS          = -L/usr/local/lib -lprotobuf `python2.7-config --ldflags`
DEL_FILE      = rm
OBJDIR       := debug
SRCDIR       := ../Runtime
MKDIR_P      := mkdir -p
_dummy1      := $(shell mkdir -p debug)
_dummy2      := $(shell mkdir -p debug/ElemBlock)

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
		ElemBlock/NodeSfc.cpp \
		ElemBlock/NodePid.cpp \
		ElemBlock/NodePython.cpp \
		ElemBlock/NodeTimer.cpp \
		ElemBlock/NodeServo.cpp \
		ElemBlock/NodeDCMotor.cpp \
		ElemBlock/NodeUiCheckBox.cpp \
		ElemBlock/NodeUiRadioButton.cpp \
		ElemBlock/NodeUiToggleButton.cpp \
		ElemBlock/NodeUiUserInput.cpp \
		ElemBlock/NodeUiUserDisplay.cpp \
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


$(OBJDIR)/arro.pb.o: $(SRCDIR)/arro.proto
	protoc --cpp_out=. arro.proto
	$(CXX) -MMD -c $(CXXFLAGS) $(INCPATH) -o $(OBJDIR)/arro.pb.o arro.pb.cc

clean:
	rm -rf debug
	rm -f lemon/lemon lemon/cfg.c lemon/cfg.h lemon/cfg.out


