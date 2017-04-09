package workspace;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;

import arro.domain.POJOIndependenceSolver;
import util.Logger;

class DeltaHandler implements IResourceDeltaVisitor {
    public boolean visit(IResourceDelta delta) {
        IResource res = delta.getResource();
        switch (delta.getKind()) {
            case IResourceDelta.ADDED:
	            Logger.out.trace(Logger.WS, "Resource " + res.getFullPath().toPortableString() + " was added.");
	            DeltaWorkspaceJob job = new DeltaWorkspaceJob();
	            //job.setRule(myProject);
	            job.schedule();
	                
	            break;
	        case IResourceDelta.REMOVED:
                Logger.out.trace(Logger.WS, "Resource " + res.getFullPath().toPortableString() + " was removed.");
	            
	            POJOIndependenceSolver.getInstance().RemovePOJOObjects(res.getName());

	    	    break;
	        case IResourceDelta.CHANGED:
                // Logger.out.trace(Logger.WS, "Resource " + res.getFullPath().toPortableString() + " has changed.");
	            break;
         }
         return true; // visit the children
    }
}
