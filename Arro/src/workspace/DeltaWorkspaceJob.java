package workspace;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

class DeltaWorkspaceJob extends WorkspaceJob {
    public DeltaWorkspaceJob() {
        super("Delta Workspace Job");
    }
    public IStatus runInWorkspace(IProgressMonitor monitor) {
        //do the actual work in here
        ResourceCache.getInstance().loadResourcesFromWorkspace();
        ResourceCache.getInstance().updateDependents();
        return Status.OK_STATUS;
    }
}
