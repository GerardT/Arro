package arro.launcher;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

import util.Logger;
import arro.Constants;
import arro.launcher.TCPClient.Result;

public class ArroLauncher implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
		Logger.out.trace(Logger.STD, "Start " + configuration.getAttribute(Constants.ATTR_LAUNCH_IP_ADDRESS, "/localhost"));
		
		 int requestPort = 13000;
		 int eventPort = 13001;
		 //requestPort = findFreePort();
		 //eventPort = findFreePort();
		 

		String projectName, ipAddress;
		try {
			projectName = configuration.getAttribute(Constants.ATTR_LAUNCH_PROJECT, "");
			ipAddress = configuration.getAttribute(Constants.ATTR_LAUNCH_IP_ADDRESS, "localhost");
			
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(projectName);
			IFolder build = project.getFolder("build");
			
			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
				if (requestPort == -1 || eventPort == -1) {
				    Logger.out.trace(Logger.ERROR, "Unable to find free port");
				}

				IDebugTarget target = new ArroDebugTarget(launch, ipAddress, requestPort, eventPort, build);
				launch.addDebugTarget(target);
		    } else if (mode.equals(ILaunchManager.RUN_MODE)) {
		    	// Let's do the same here for now.
				if (requestPort == -1 || eventPort == -1) {
				    Logger.out.trace(Logger.ERROR, "Unable to find free port");
				}

				IDebugTarget target = new ArroDebugTarget(launch, ipAddress, requestPort, eventPort, build);
				launch.addDebugTarget(target);
		    }

		} catch (CoreException e) {
			Logger.out.trace(Logger.ERROR, e.toString());
		}


	}
}
