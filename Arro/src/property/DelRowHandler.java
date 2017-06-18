package property;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import arro.editors.MultiPageEditor;
import arro.messageeditor.MessageEditor;
import util.Logger;



/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class DelRowHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Logger.out.trace(Logger.EDITOR, "activeEditor " + activeEditor.toString());
		Logger.out.trace(Logger.EDITOR, "selection " + selection.toString());

		if(activeEditor instanceof MultiPageEditor) {
			MessageEditor me = (MessageEditor) activeEditor;
	        IUndoableOperation operation = new DeltaInfoOperation(me, selection);
	    	operation.addContext(me.getUndoContext());
        	me.getOperationHistory().execute(operation, null, null);
		}
        return null;
	}

	class DeltaInfoOperation extends AbstractOperation {
		MessageEditor me;
		Object undoList, redoList;
		ISelection selection;
		public DeltaInfoOperation(MessageEditor me, ISelection selection) {
			super("Delta Operation");
			this.me = me;
			this.selection = selection;
		}
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
			StructuredSelection s = (StructuredSelection) selection;
			undoList = me.cloneMessageList();
			if(s.isEmpty()) {
				//me.addRow();				
			} else {
				me.delRow((StructuredSelection) selection);
			}
			return Status.OK_STATUS;
		}
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
			redoList = me.cloneMessageList();
			me.setMessageList(undoList);

			return Status.OK_STATUS;
		}
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			undoList = me.cloneMessageList();
			me.setMessageList(redoList);

			return Status.OK_STATUS;
		}
	}

}
