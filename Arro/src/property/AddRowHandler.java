package property;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import arro.editors.MultiPageEditor;
import util.Logger;



/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class AddRowHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		System.out.println(event.getCommand().getId().toString());
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Logger.out.trace(Logger.EDITOR, "activeEditor " + activeEditor.toString());
		Logger.out.trace(Logger.EDITOR, "selection " + selection.toString());

		if(activeEditor instanceof MultiPageEditor) {
			//MultiPageEditor me = (MultiPageEditor) activeEditor;
	        //IUndoableOperation operation = new DeltaInfoOperation(me, selection);
	    	//operation.addContext(me.getUndoContext());
        	//me.getOperationHistory().execute(operation, null, null);
		}
        return null;
	}

	@Override
	public boolean isEnabled() {

		return true;
	}

	class DeltaInfoOperation extends AbstractOperation {
		MultiPageEditor me;
		Object undoList, redoList;
		ISelection selection;
		public DeltaInfoOperation(MultiPageEditor me, ISelection selection) {
			super("Delta Operation");
			this.me = me;
			this.selection = selection;
		}
		@Override
        public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
			StructuredSelection s = (StructuredSelection) selection;
			//undoList = me.cloneMessageList();
			if(s.isEmpty()) {
				//me.addRow();				
			} else {
				//me.delRow((StructuredSelection) selection);
			}
			return Status.OK_STATUS;
		}
		@Override
        public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
			//redoList = me.cloneMessageList();
			//me.setMessageList(undoList);

			return Status.OK_STATUS;
		}
		@Override
        public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			//undoList = me.cloneMessageList();
			//me.setMessageList(redoList);

			return Status.OK_STATUS;
		}
	}

}
