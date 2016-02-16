package arro.messageeditor;

import java.util.Collection;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.e4.core.commands.ExpressionContext;

import util.Logger;



/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class AddRowHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public AddRowHandler() {
		super();
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		//Object object = event.getApplicationContext();
		//if (object instanceof ExpressionContext) {
			//ExpressionContext appContext = (ExpressionContext) object;
			
			//IEditorPart activeEditor = (IEditorPart) appContext.getVariable(ISources.ACTIVE_EDITOR_NAME);
			//ISelection selection = (ISelection) appContext.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			
			Logger.out.trace(Logger.EDITOR, "activeEditor " + activeEditor.toString());
			Logger.out.trace(Logger.EDITOR, "selection " + selection.toString());

			if(activeEditor instanceof MessageEditor) {
				MessageEditor me = (MessageEditor) activeEditor;
		        IUndoableOperation operation = new DeltaInfoOperation(me, selection);
		    	operation.addContext(me.getUndoContext());
	        	me.getOperationHistory().execute(operation, null, null);
			}
		//}
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
	        // Build the string buffer "buf"
//	        MessageDialog.openInformation(shell,
//	                        ContributionMessages.DeltaInfoHandler_shellTitle,
//	                        "Undoing delta calculation");
			me.setMessageList(undoList);

			return Status.OK_STATUS;
		}
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			undoList = me.cloneMessageList();
	        // Build the string buffer "buf"
	        // simply re-calculate the delta
//	        MessageDialog.openInformation(shell,
//	                        ContributionMessages.DeltaInfoHandler_shellTitle, buf
//	                                        .toString());
			me.setMessageList(redoList);

			return Status.OK_STATUS;
		}
	}


	/**
	 * the command has been executed, so extract the needed information
	 * from the application context.
	 */
//	public Object execute(ExecutionEvent event) throws ExecutionException {
//		Object object = event.getApplicationContext();
//		if (object instanceof ExpressionContext) {
//			ExpressionContext appContext = (ExpressionContext) object;
//			
//			IEditorPart activeEditor = (IEditorPart) appContext.getVariable(ISources.ACTIVE_EDITOR_NAME);
//			ISelection selection = (ISelection) appContext.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
//			
//			Logger.out.trace(Logger.EDITOR, "activeEditor " + activeEditor.toString());
//			Logger.out.trace(Logger.EDITOR, "selection " + selection.toString());
//			
//			if(activeEditor instanceof MessageEditor) {
//				MessageEditor me = (MessageEditor) activeEditor;
//				StructuredSelection s = (StructuredSelection) selection;
//				if(s.isEmpty()) {
//					me.addRow();				
//				} else {
//					me.delRow((StructuredSelection) selection);
//				}
//			}
//		}
//		
//		Logger.out.trace(Logger.EDITOR, "command " + object.toString());
//
////		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
////		MessageDialog.openInformation(
////				window.getShell(),
////				"Arro",
////				"Adding a row");
//		return null;
//	}
	
}
