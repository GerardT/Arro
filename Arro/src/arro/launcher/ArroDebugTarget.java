package arro.launcher;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import arro.Constants;
import arro.launcher.TCPClient.Result;
import util.Logger;

public class ArroDebugTarget implements IDebugTarget {
	TCPClient engine;
	ILaunch launch;
	boolean terminated;
	

	public ArroDebugTarget(ILaunch launch, String ipAddress, int requestPort, int eventPort, IFolder build) {
		this.launch = launch;
		terminated = true;
		
		engine = new TCPClient(ipAddress);
		Result result = engine.connect(requestPort);
		if(result.result) {
			try {
				IResource[] members = build.members();
				
				// FTP all the stuff in Build directory
				for(IResource r: members) {
					IFile file;
					
					file = build.getFile(r.getName());
					
					engine.sendFile(file);
					
					engine.filterReply("put");
            	}
    			engine.remoteCmd("protobuf");
			 
                engine.filterReply("protobuf");

    			engine.remoteCmd("run");
    			 
    			engine.filterReply("run");
    			
    			engine.startSocketReader();
    
    			terminated = false;
            } catch (ExecutionException e) {
                Logger.writeToConsole("Lost socket: could not download " + e.getMessage());
            } catch (CoreException e) {
                Logger.writeToConsole("Error: could not download " + e.getMessage());
            }
		}
	}
	
	@Override
	public String getModelIdentifier() {
		return Constants.PLUGIN_ID;	}

	@Override
	public IDebugTarget getDebugTarget() {
		return this;
	}

	@Override
	public ILaunch getLaunch() {
		// TODO Auto-generated method stub
		return launch;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == ILaunch.class) {
			return getLaunch();
		}
		return null;
	}
	
	@Override
	public boolean canTerminate() {
		return true;
	}

	@Override
	public boolean isTerminated() {
		// TODO Auto-generated method stub
		return terminated;
	}

	@Override
	public void terminate() throws DebugException {
		engine.stopSocketReader();
		
		// Just close socket, will cause terminate.
        engine.close();
		
		terminated = true;
	}

	@Override
	public boolean canResume() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canSuspend() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSuspended() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void resume() throws DebugException {
		// TODO Auto-generated method stub

	}

	@Override
	public void suspend() throws DebugException {
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	@Override
	public boolean canDisconnect() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void disconnect() throws DebugException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDisconnected() {
		return false;
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return false;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length)
			throws DebugException {
		return null;
	}

	@Override
	public IProcess getProcess() {
		return null;
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		return new IThread[0];
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#hasThreads()
	 */
	@Override
	public boolean hasThreads() throws DebugException {
		return false;
	}

	@Override
	public String getName() throws DebugException {
		return "mytarget";
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return false;
	}

}
