package property;


import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;

public abstract class ArroGenericSection extends GFPropertySection implements ITabbedPropertyConstants {
	private UndoActionHandler undoAction;
	private RedoActionHandler redoAction;
	private IUndoContext undoContext;
	
	ArroGenericSection() {
	    undoContext = new ObjectUndoContext(this);
	    IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
	    undoAction = new UndoActionHandler(part.getSite(), undoContext);
	    redoAction = new RedoActionHandler(part.getSite(), undoContext);
	}

	public abstract void update();
	
	public IOperationHistory getOperationHistory() {

	  // The workbench provides its own undo/redo manager
	  //return PlatformUI.getWorkbench()
	  //   .getOperationSupport().getOperationHistory();

	  // which, in this case, is the same as the default undo manager
	  return OperationHistoryFactory.getOperationHistory();
	}

	public IUndoContext getUndoContext() {

	  // For workbench-wide operations, we should return
	  //return PlatformUI.getWorkbench()
	  //   .getOperationSupport().getUndoContext();

	  // but our operations are all local, so return our own content
	  return undoContext;
	}

//			private void setUndoRedoActionHandlers() {
	//
//			    final IActionBars actionBars = getEditorSite().getActionBars();
//			    actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
//			    actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
//			    actionBars.updateActionBars();
//			}


	public IWorkbenchPartSite getSite() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite();
	}
}