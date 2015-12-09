

CC            = gcc
CXX           = g++
DEFINES       = -DUNICODE 
CFLAGS        = -pipe -Wall -Wextra $(DEFINES) `python-config --cflags`
CXXFLAGS      = -pipe -std=c++0x -g -frtti -Wall -Wextra -fexceptions $(DEFINES) `python-config --cflags`
# INCPATH       = -I"." -I"../../protobuf-2.5.0/src"
INCPATH       = -I"." -I"Nodes" -I"/usr/local/include"
LINKER        = g++
# LFLAGS        = -g -Wl,-subsystem		--> -Wl,-subsystem is MinGW stuff, results in no symbols.
LFLAGS        = -g
LIBS          = -L/usr/local/lib -lprotobuf `python-config --ldflags`
DEL_FILE      = rm
OBJDIR       := debug
SRCDIR       := ../Runtime


DEPS         := $(SRCS:.cpp=.d)
-include $(DEPS)

SRCS         := main.cpp \
		Trace.cpp \
		ServerEngine.cpp \
		ConfigReader.cpp \
		NodeDb.cpp \
		arro.pb.cpp \
		Pad.cpp \
		Process.cpp \
		PythonGlue.cpp \
		Nodes/NodePid.cpp \
		Nodes/NodePython.cpp \
		Nodes/NodeLinear.cpp \
		Nodes/NodeTimer.cpp \
		Nodes/NodeStartToken.cpp \
		Nodes/NodeTransition.cpp \
		tinyxmlparser.cpp \
		tinyxmlerror.cpp \
		tinyxml.cpp \
		tinystr.cpp \
		socket.cpp

OBJS         := $(addprefix $(OBJDIR)/,$(SRCS:.cpp=.o))
SRCS         := $(addprefix $(SRCDIR)/,$(SRCS))

all: $(OBJS)
	$(CXX) -o Arro $(LFLAGS) $(OBJS) $(LIBS) 

$(OBJDIR)/%.o: $(SRCDIR)/%.cpp
	$(CXX) -MMD -c $(CXXFLAGS) $(INCPATH) -o $@ $<

$(OBJDIR)/arro.pb.o: $(SRCDIR)/arro.proto
	protoc --cpp_out=. arro.proto
	$(CXX) -MMD -c $(CXXFLAGS) $(INCPATH) -o $(OBJDIR)/arro.pb.o arro.pb.cc

clean:
	$(RM) debug/* debug/Nodes/*


