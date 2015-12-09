#include <google/protobuf/text_format.h>
//#include <google/protobuf/stubs/strutil.h>

#include <NodeUiOut.h>
#include <arro.pb.h>
#include <NodeDb.h>


using namespace std;
using namespace google;
using namespace protobuf;
using namespace arro;

using namespace google::protobuf;


void NodeUiOut::PrintField(const Message& message,
                                     const Reflection* reflection,
                                     const FieldDescriptor* field,
                                     TextGenerator& generator) const {
  if (field->is_repeated() &&
      field->cpp_type() != FieldDescriptor::CPPTYPE_STRING &&
      field->cpp_type() != FieldDescriptor::CPPTYPE_MESSAGE) {
    PrintShortRepeatedField(message, reflection, field, generator);
    return;
  }

  int count = 0;

  if (field->is_repeated()) {
    count = reflection->FieldSize(message, field);
  } else if (reflection->HasField(message, field)) {
    count = 1;
  }

  for (int j = 0; j < count; ++j) {
    PrintFieldName(message, reflection, field, generator);

    if (field->cpp_type() == FieldDescriptor::CPPTYPE_MESSAGE) {
      //generator.Print(" { ");
    } else {
      //generator.Print(": ");
    }

    // Write the field value.
    int field_index = j;
    if (!field->is_repeated()) {
      field_index = -1;
    }

    PrintFieldValue(message, reflection, field, field_index, generator);

    if (field->cpp_type() == FieldDescriptor::CPPTYPE_MESSAGE) {
      //generator.Print("} ");
    } else {
      //generator.Print(" ");
    }
  }
}

void NodeUiOut::PrintShortRepeatedField(
    const Message& message,
    const Reflection* reflection,
    const FieldDescriptor* field,
    TextGenerator& generator) const {
  // Print primitive repeated field in short form.
  PrintFieldName(message, reflection, field, generator);

  int size = reflection->FieldSize(message, field);
  generator.StartNewNode(": [");
  for (int i = 0; i < size; i++) {
    if (i > 0) generator.NewElt(", ");
    PrintFieldValue(message, reflection, field, i, generator);
  }
  generator.EndNewNode("] ");
}

void NodeUiOut::PrintFieldName(const Message& message,
                                         const Reflection* reflection,
                                         const FieldDescriptor* field,
                                         TextGenerator& generator) const {
  if (field->is_extension()) {
    generator.Print("[");
    // We special-case MessageSet elements for compatibility with proto1.
    if (field->containing_type()->options().message_set_wire_format()
        && field->type() == FieldDescriptor::TYPE_MESSAGE
        && field->is_optional()
        && field->extension_scope() == field->message_type()) {
      generator.Print(field->message_type()->full_name());
    } else {
      generator.Print(field->full_name());
    }
    generator.Print("]");
  } else {
    if (field->type() == FieldDescriptor::TYPE_GROUP) {
      // Groups must be serialized with their original capitalization.
      generator.Print(field->message_type()->name());
    } else {
      generator.PrintFieldName(field->name());
    }
  }
}

void NodeUiOut::PrintFieldValue(
    const Message& message,
    const Reflection* reflection,
    const FieldDescriptor* field,
    int index,
    TextGenerator& generator) const {
  GOOGLE_DCHECK(field->is_repeated() || (index == -1))
      << "Index must be -1 for non-repeated fields";

  switch (field->cpp_type()) {
#define OUTPUT_FIELD(CPPTYPE, METHOD, TO_STRING)                             \
      case FieldDescriptor::CPPTYPE_##CPPTYPE:                               \
        generator.PrintFieldValue(TO_STRING(field->is_repeated() ?                     \
          reflection->GetRepeated##METHOD(message, field, index) :           \
          reflection->Get##METHOD(message, field)));                         \
        break;                                                               \

      OUTPUT_FIELD( INT32,  Int32, SimpleItoa);
      OUTPUT_FIELD( INT64,  Int64, SimpleItoa);
      OUTPUT_FIELD(UINT32, UInt32, SimpleItoa);
      OUTPUT_FIELD(UINT64, UInt64, SimpleItoa);
      OUTPUT_FIELD( FLOAT,  Float, SimpleFtoa);
      OUTPUT_FIELD(DOUBLE, Double, SimpleDtoa);
#undef OUTPUT_FIELD

      case FieldDescriptor::CPPTYPE_STRING: {
        string scratch;
        const string& value = field->is_repeated() ?
            reflection->GetRepeatedStringReference(
              message, field, index, &scratch) :
            reflection->GetStringReference(message, field, &scratch);

        //generator.Print("\"");
        generator.PrintFieldValue(CEscape(value));
        //generator.Print("\"");

        break;
      }

      case FieldDescriptor::CPPTYPE_BOOL:
        if (field->is_repeated()) {
          generator.PrintFieldValue(reflection->GetRepeatedBool(message, field, index)
                          ? "true" : "false");
        } else {
          generator.PrintFieldValue(reflection->GetBool(message, field)
                          ? "true" : "false");
        }
        break;

      case FieldDescriptor::CPPTYPE_ENUM:
        generator.PrintEnum(field->is_repeated() ?
          reflection->GetRepeatedEnum(message, field, index)->name() :
          reflection->GetEnum(message, field)->name());
        break;

      case FieldDescriptor::CPPTYPE_MESSAGE:
        Print(field->is_repeated() ?
                reflection->GetRepeatedMessage(message, field, index) :
                reflection->GetMessage(message, field),
              generator);
        break;
  }
}


void NodeUiOut::Print(const Message& message, TextGenerator& generator) const {
  const Reflection* reflection = message.GetReflection();
  vector<const FieldDescriptor*> fields;
  reflection->ListFields(message, &fields);
  for (int i = 0; i < fields.size(); i++) {
    PrintField(message, reflection, fields[i], generator);
  }
  // FIXME PrintUnknownFields(reflection->GetUnknownFields(message), generator);
}


NodeUiOut::NodeUiOut(string datatype, string name):
    trace(string("NodeUiOut"), true) {


//    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
//    class AnonNode: public NodeListener {
//        NodeUiOut* owner;
//    public:
//        AnonNode(NodeUiOut* n){owner = n; };
//
//        virtual void update(string& set, string& get) {
//            Value* value = new Value();
//
//            int iset;
//            try {
//                iset = std::stoi(set);
//            }
//            catch(std::invalid_argument)
//            {
//                iset = 0;
//            }
//
//            value->set_value(iset);
//
//            owner->result->submitMessage(value);
//
//            get = owner->xml;
//        }
//    };
//
//    n = NodeManager::getRef()->registerNode(name, new AnonNode(this));


    /* Almost anonymous class (if 'Anon' removed), but needed constructor */
    class Anon: public IPadListener {
        NodeUiOut* owner;
    public:
        Anon(NodeUiOut* n){owner = n;};

        void handleMessage(MessageBuf* msg, string padName) {
            // do not put in queue but instead forward directly to target node.
            owner->trace.println("NodeUiOut dump ");
            //Token* m = new Token();
            //m->CheckTypeAndMergeFrom(*msg);
            //TextFormat::PrintToString(*(Message*)msg, &(owner->n->xml));
            TextGenerator generator(&(owner->xml));
            owner->Print(*(Message*)msg, generator);
            generator.getString(owner->xml);
            owner->trace.println(owner->n->getName());
        }
    };

    //n->registerNodeInput(datatype, "", new Anon(this));
    //result = n->registerNodeOutput("Value", "");
}



