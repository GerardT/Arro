package arro.diagram.types;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;

import util.Logger;

public class MyResourceChangeReporter implements IResourceChangeListener {
    public void resourceChanged(IResourceChangeEvent event) {
      IResource res = event.getResource();
      switch (event.getType()) {
         case IResourceChangeEvent.PRE_CLOSE:
//            Logger.out.trace(Logger.EDITOR, "Project ");
//            Logger.out.trace(Logger.EDITOR, res.getFullPath());
//            Logger.out.trace(Logger.EDITOR, " is about to close.");
            break;
         case IResourceChangeEvent.PRE_DELETE:
            Logger.out.trace(Logger.EDITOR, "Project ");
            Logger.out.trace(Logger.EDITOR, res.getFullPath().toString());
            Logger.out.trace(Logger.EDITOR, " is about to be deleted.");
            break;
         case IResourceChangeEvent.POST_CHANGE:
//            Logger.out.trace(Logger.EDITOR, "Resources have changed.");
			try {
				
				event.getDelta().accept(new DeltaHandler());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            break;
//         case IResourceChangeEvent.PRE_BUILD:
//            Logger.out.trace(Logger.EDITOR, "Build about to run.");
//            event.getDelta().accept(new DeltaPrinter());
//            break;
//         case IResourceChangeEvent.POST_BUILD:
//            Logger.out.trace(Logger.EDITOR, "Build complete.");
//            event.getDelta().accept(new DeltaPrinter());
//            break;
      }
   }
}

