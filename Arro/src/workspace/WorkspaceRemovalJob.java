package workspace;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

class WorkspaceRemovalJob extends WorkspaceJob {
    String name;
    IResource res;
    public WorkspaceRemovalJob(String name, IResource res) {
        super("Delta Workspace Job");
        this.name = name;
        this.res = res;
    }
    public IStatus runInWorkspace(IProgressMonitor monitor) {
        //do the actual work in here
        ResourceCache.getInstance().removeFromCache(name, res);
        return Status.OK_STATUS;
    }
}
