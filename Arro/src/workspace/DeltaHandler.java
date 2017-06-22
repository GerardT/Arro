package workspace;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;

import arro.domain.POJOIndependenceSolver;
import util.Logger;

class DeltaHandler implements IResourceDeltaVisitor {
    @Override
    public boolean visit(IResourceDelta delta) {
        IResource res = delta.getResource();
        String name = res.getName();
        int ix = name.indexOf(".anod");
        if(ix != -1) {
            name = name.substring(0, ix);
            
            switch (delta.getKind()) {
            case IResourceDelta.ADDED:
                Logger.out.trace(Logger.WS, "Resource " + res.getFullPath().toPortableString() + " was added.");
                WorkspaceAdditionJob job1 = new WorkspaceAdditionJob(name, res);
                job1.schedule();
                break;
            case IResourceDelta.REMOVED:
                Logger.out.trace(Logger.WS, "Resource " + res.getFullPath().toPortableString() + " was removed.");              
                POJOIndependenceSolver.getInstance().RemovePOJOObjects(name);
                WorkspaceRemovalJob job2 = new WorkspaceRemovalJob(name, res);
                job2.schedule();
                break;
            case IResourceDelta.CHANGED:
                // Logger.out.trace(Logger.WS, "Resource " + res.getFullPath().toPortableString() + " has changed.");
                break;
         }
        }
         return true; // visit the children
    }
}
