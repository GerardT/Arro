package arro.node.diagram;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;

import util.Logger;
import arro.domain.POJOIndependenceSolver;

class DeltaHandler implements IResourceDeltaVisitor {
    public boolean visit(IResourceDelta delta) {
        IResource res = delta.getResource();
        switch (delta.getKind()) {
            case IResourceDelta.ADDED:
//	            Logger.out.trace(Logger.EDITOR, "Resource ");
//	            Logger.out.trace(Logger.EDITOR, res.getFullPath());
//	            Logger.out.trace(Logger.EDITOR, " was added.");
	            break;
	        case IResourceDelta.REMOVED:
//	            Logger.out.trace(Logger.EDITOR, "Resource ");
//	            Logger.out.trace(Logger.EDITOR, res.getFullPath());
//	            Logger.out.trace(Logger.EDITOR, " was removed.");
	            
//	            final Runnable r = new Runnable ()  {
//	    	        public void run() {
//	    	            IWorkbenchWindow iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//	    	    	    Shell shell = iw.getShell();
//	    	    	    
//			            // create dialog with ok and cancel button and info icon
//			            MessageBox dialog =
//			              new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
//			            dialog.setText("My info");
//			            dialog.setMessage("Do you really want to do this?");
//		
//			            // open dialog and await user selection
//			            int returnCode = dialog.open(); 
//	    	        }
//	    	    };
//	    	    
//	            
//	    	    Display.getDefault().syncExec(r);
	            POJOIndependenceSolver.getInstance().RemovePOJOObjects(res.getName());

	    		 break;
	        case IResourceDelta.CHANGED:
//	            Logger.out.trace(Logger.EDITOR, "Resource ");
//	            Logger.out.trace(Logger.EDITOR, res.getFullPath());
//	            Logger.out.trace(Logger.EDITOR, " has changed.");
	            break;
         }
         return true; // visit the children
    }
}
