package arro.launcher;

import java.io.*;
import java.net.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.internal.gtk.OS;

import util.Logger;


class TCPClient
{
	public class Result {
		boolean result;
		Result(boolean result) {
			this.result = result;
		}
	}
	
	private String address;
	private Socket clientSocket;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	InputStreamReader isr;
	SocketReader socketReader;

	
	TCPClient(String address) {
		this.address = address;
	}
	
	Result connect(int requestPort) {
		  try {
			clientSocket = new Socket(address, requestPort);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			return new Result(true);
		} catch (IOException e) {
			Logger.out.trace(Logger.ERROR, "Failed to connect");
			return new Result(false);
		}
	}
	
	/**
	 * Close the socket, will generate 'terminate' inside server process.
	 */
	public void close() {
		try {
			clientSocket.close();
		} catch (IOException e) {
		    Logger.out.trace(Logger.ERROR, "Could not close socket");
		}
	}
	
	
	public void sendFile(IFile file) {

		 IPath p = file.getLocation();
		 File f = p.toFile();
		 long length = f.length();
				  
		 remoteCmd("put " + file.getName() + " " + length);

		 try {
			  InputStream content = file.getContents(true);
			
			  int count;
			  byte[] buffer = new byte[1024];

			  BufferedInputStream in = new BufferedInputStream(content);
			  while ((count = in.read(buffer)) > 0) {
				  outToServer.write(buffer, 0, count);
				  outToServer.flush();
			  }
		 } catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
	}
	
	public void remoteCmd(String msg) {
		  try {
			outToServer.writeBytes(msg + "\n");
			outToServer.flush();
		} catch (IOException e) {
		    Logger.out.trace(Logger.ERROR, "Could not write to socket");
		}
	}
	
	/**
	 * Blocked read text from socket until '\n' is received.
	 * @return
	 * @throws IOException 
	 */
	private String readln() throws IOException {
		return inFromServer.readLine();
	}
	
	class SocketReader implements Runnable {
	    private Thread t;
		   
		@Override
		public void run() {
			while(true) {
			    try {
					String s = readln();
					Logger.writeToConsole(s);
				} catch (IOException e) {
				    Logger.out.trace(Logger.ERROR, "Could not read from socket");
				    break;
				}
			}
		}
		   
		public void start ()
		{
		    System.out.println("Starting");
		    if (t == null)
		    {
		        t = new Thread (this);
		        t.start ();
		    }
		}

	}

	public void startSocketReader() {
		socketReader = new SocketReader();
		socketReader.start();
	}
	
	public void stopSocketReader() {
		socketReader = null;  // FIXME radical
	}

		
	public void filterReply(String filterString) {
		while(true) {
			String s;
			try {
				s = readln();
				Logger.writeToConsole(s);
				if(s.contains(filterString)) {
					return;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

