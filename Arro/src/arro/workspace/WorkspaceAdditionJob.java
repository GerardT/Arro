package arro.workspace;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import util.Logger;

class WorkspaceAdditionJob extends WorkspaceJob {
    String name;
    IResource res;
    public WorkspaceAdditionJob(String name, IResource res) {
        super("Delta Workspace Job");
        this.name = name;
        this.res = res;
    }
    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) {
        //do the actual work in here
        Logger.out.trace(Logger.WS, "WorkspaceAdditionJob add " + name);
        ResourceCache.getInstance().addToCache(name, res);
        return Status.OK_STATUS;
    }
}
