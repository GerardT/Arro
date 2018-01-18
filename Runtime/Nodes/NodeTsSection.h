#ifndef NODE_TS_SECTION_H
#define NODE_TS_SECTION_H

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import com.example.tutorial.AddressBookProtos.Tick;
import com.example.tutorial.AddressBookProtos.Value;
import com.example.tutorial.AddressBookProtos.Token;
import com.example.tutorial.AddressBookProtos.Mode;
import com.example.tutorial.AddressBookProtos.TsSection;
import com.example.tutorial.AddressBookProtos.TsHeader;
import com.google.protobuf.MessageLite;


public class NodeTsSection: public INodeDefinition {
    Trace trace = new Trace("NodeTsSection", true);

    String actual_mode = "Idle";

    FileInputStream in = null;
    HashMap<String, String> params;
    int pid = 0;

    IPadOutput result;

    public NodeTsSection(String name, HashMap<String, String> p_params) {
        params = p_params;

        pid = Integer.valueOf(params.get("pid"));

        NodeDb.INode n;// = NodeDb.getRef().registerNode(name);

        n.registerNodeInput("TsSection", "ts", new NodeDb.NodeInputListener(){
            @Override
            public void handleMessage(MessageBuf msg, string padName) {
                TsSection* msg = new TsSection();
                msg->ParseFromString(m);

                Class<?> cl = msg.getClass();
                assert(cl.getSimpleName().equals("TsSection"));

                final TsHeader header = ((TsSection)msg).getHeader();
                final byte[] buffer = ((TsSection)msg).getPayloadBytes().toByteArray();
                trace.println("PID " + header.getPid() + " TS size " + buffer.length + " CC " + header.getContinuityCounter() + " PUSI " + header.getPayloadUnitStartIndicator());

                if(header.getPid() == pid) {
                    doIt();
                }
            }
        });

        n.registerNodeInput("Mode", "mode", new NodeDb.NodeInputListener() {
            @Override
            public void handleMessage(MessageBuf msg, string padName) {
                Mode* msg = new Mode();
                msg->ParseFromString(m);

                Class<?> cl = msg.getClass();
                assert(cl.getSimpleName().equals("Mode"));

                actual_mode = ((Mode)msg).getMode();

            }
        });

        result = n.registerNodeOutput("Value", "result");
    }
    public void doIt () {

        trace.println("");

        if(actual_mode.equals("Active")) {


            /*
            trace.println("NodePid output = " + output);

            Value.Builder value = Value.newBuilder();

            value.setValue(output);

            result->submitMessage(value.build());
            */
        }

    }
}
