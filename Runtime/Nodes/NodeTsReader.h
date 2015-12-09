#ifndef NODE_TS_READER_H
#define NODE_TS_READER_H

/*
import com.example.tutorial.AddressBookProtos.Tick;
import com.example.tutorial.AddressBookProtos.TsHeader;
import com.example.tutorial.AddressBookProtos.Value;
import com.example.tutorial.AddressBookProtos.Token;
import com.example.tutorial.AddressBookProtos.Mode;
import com.example.tutorial.AddressBookProtos.TsSection;
import com.example.tutorial.AddressBookProtos.TsHeader;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
*/

class NodeTsReader: public IDevice {
private:
    Trace trace = new Trace("", false);
	
	String actual_mode = "Idle";

    FileInputStream in = null;
	HashMap<String, String> params;
	int pid = 0;
	
	IPadOutput result;
	
public:

    NodeTsReader(String name, HashMap<String, String> p_params) {
		params = p_params;
		
		pid = Integer.valueOf(params.get("pid"));
		
		INode n;// = NodeDb.getRef().registerNode(name);
		
		n.registerNodeInput("Tick", "timer", new NodeDb.NodeInputListener() {
			public void handleMessage(MessageBuf msg, string padName) {
	        	Tick* msg = new Tick();
	        	msg->ParseFromString(m);

			    Class<?> cl = msg.getClass();
				assert(cl.getSimpleName().equals("Tick"));
				
				Tick tick = (Tick)msg;
				
				doIt(tick.getMsElapsed());
			}
		});
		
		n.registerNodeInput("Mode", "mode", new NodeDb.NodeInputListener() {
			public void handleMessage(MessageBuf msg, string padName) {
	        	Mode* msg = new Mode();
	        	msg->ParseFromString(m);

			    Class<?> cl = msg.getClass();
				assert(cl.getSimpleName().equals("Mode"));
				
				actual_mode = ((Mode)msg).getMode();
				
				trace.println("NodeTsReader changed mode to " + actual_mode);
				if(actual_mode.equals("Active")) {
			        try {
			        	// open and read until sync byte 0x47
			        	String filename = params.get("file");
			            in = new FileInputStream(filename);
			            int c;

			            while ((c = in.read()) != -1 && c != 0x47) {
			            }
						trace.println("NodeTsReader file open and sync byte found");

			        } catch (IOException name) {
						trace.println("NodeTsReader IOException");
			        }
				}
				else {
		            if (in != null) {
		            	try {
		            		in.close();
		            	} catch (IOException name) {
							trace.println("NodeTsReader IOException");
		            	}
			        }
				}
			}
		});
		
		result = n.registerNodeOutput("TsSection", "ts");
	}
	public void doIt (int ms_elapsed) {
		
		trace.println("NodeTsReader  ");
		
		if(actual_mode.equals("Active")) {
			ms_elapsed /= 1000;


	        try {
	            int c;
	            int i = 0;
	            byte[] buffer;

	            buffer = new byte[188];
	            
	            while ((c = in.read()) != -1 && i < (188 - 1)) {
	            	buffer[i++] = (byte)c;
	            }
	            if(c == 0x47) {
					// check Transport Error Indicator (bit 0)
					if((buffer[0] & 0x80) == 0x80) {
						trace.println("NodeTsReader Transport Error Indicator");
					} else {
						
						/*
						Scrambling control	    2	00 = Not scrambled. The following per DVB spec: 01 = Reserved for future use, 10 = Scrambled with even key, 11 = Scrambled with odd key
						Adaptation field exist	2	01 = no adaptation fields, payload only, 10 = adaptation field only, 11 = adaptation field and payload
						Continuity counter	    4	Incremented only when a payload is present (i.e., adaptation field exist is 01 or 11)
						*/
						
						// check PID (bit 3 ... 16)
						int PID = buffer[1] + ((buffer[0] & 0x1F) * 256);
						int Cont = buffer[2] & 0x0F;
						trace.println("NodeTsReader another TS packet found, PID " + PID);
						if(PID == pid) {
							
							trace.println("NodeTsReader cont " + Cont);
							
						    TsSection.Builder value = TsSection.newBuilder();
							TsHeader.Builder header = TsHeader.newBuilder();
							
						    header.setTransportErrorIndicator(buffer[0] & 0x80);
						    header.setPayloadUnitStartIndicator(buffer[0] & 0x40);
						    header.setTransportPriority(buffer[0] & 0x20);
						    header.setPid(PID);
						    header.setScramblingControl(buffer[2] & 0xC0);
						    header.setAdaptationField(buffer[2] & 0x20);
						    header.setPayloadData(buffer[2] & 0x10);
						    header.setContinuityCounter(buffer[2] & 0x0F);

						    value.setHeader(header);
						    value.setPayloadBytes(ByteString.copyFrom(buffer, 0, 188));

							result->submitMessage(value.build());
						}
					}
	            }
	        } catch (IOException name) {
				trace.println("NodeTsReader IOException");
	        }
			
		}
		
	}
}
