#ifndef NODE_UI_OUT_H
#define NODE_UI_OUT_H

#include <Trace.h>
#include <google/protobuf/text_format.h>

#include <google/protobuf/descriptor.h>
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>
#include <google/protobuf/unknown_field_set.h>
#include <google/protobuf/descriptor.pb.h>
#include <google/protobuf/io/tokenizer.h>
//#include <google/protobuf/stubs/strutil.h>
//#include <google/protobuf/stubs/map-util.h>
#include <google/protobuf/stubs/stl_util.h>

#include <google/protobuf/text_format.h>

#include <tinyxml.h>


class TextGenerator: public IDevice {
 public:
  explicit TextGenerator(string* out) {
	  output = out;
	  //TiXmlElement * element = new TiXmlElement( "Node" );
	  element = &doc;

  }

  ~TextGenerator() {
    //delete element;
  }

  // Print text to the output stream.
  void PrintFieldName(const string& str) {
	  attrName = str;
	  output->append(str);
  }

  void PrintFieldValue(const string& str) {
	  TiXmlElement * t = new TiXmlElement( "prop" );
	  t->SetAttribute(attrName, str);
	  element->LinkEndChild( t );
	  output->append(str);
  }

  void Print(const string& str) {
	  output->append(str);
  }
  void NewElt(const string& str) {
	  output->append(str);
  }
  void StartNewNode(const string& str) {
	  output->append(str);
  }
  void EndNewNode(const string& str) {
	  output->append(str);
  }
  void PrintEnum(const string& str) {
	  output->append(str);
  }


 //void Print(const string& str) {
	//  TiXmlElement * t = new TiXmlElement( "dunno" );
	//  t->SetAttribute(attrName, str);
	//  element->LinkEndChild( t );
	//  output->append(str);
 // }

 // // Print text to the output stream.
 // void Print(const char* text) {
	//  TiXmlText * t = new TiXmlText( text );
	//  element->LinkEndChild( t );
	//  output->append(text);
 // }

  // Print text to the output stream.
  //void Print(const char* text, int size) {output->append(text, size); }


  void getString(string& str) {
	  // Declare a printer    
	  TiXmlPrinter printer;

	  // attach it to the document you want to convert in to a std::string 
	  doc.Accept(&printer);

	  // Create a std::string and copy your document data in to the string    
	  str = printer.Str();
  }

  // True if any write to the underlying stream failed.  (We don't just
  // crash in this case because this is an I/O failure, not a programming
  // error.)
  bool failed() const {return false; }

 private:
  GOOGLE_DISALLOW_EVIL_CONSTRUCTORS(TextGenerator);
  string* output;
  TiXmlDocument doc;
  TiXmlNode * element;
  string attrName;
};



class NodeUiOut {
	string xml;

public:
	IPadOutput* result;

    INode* n;
public:
    Trace trace;
	
    NodeUiOut(string datatype, string name);

	void Print(const Message& message, TextGenerator& generator) const;
	void PrintFieldValue(const Message& message, const Reflection* reflection, const FieldDescriptor* field, int index, TextGenerator& generator) const;
	void PrintFieldName(const Message& message, const Reflection* reflection, const FieldDescriptor* field, TextGenerator& generator) const;
	void PrintShortRepeatedField(const Message& message, const Reflection* reflection, const FieldDescriptor* field, TextGenerator& generator) const;
	void PrintField(const Message& message, const Reflection* reflection, const FieldDescriptor* field, TextGenerator& generator) const;

};

#endif
