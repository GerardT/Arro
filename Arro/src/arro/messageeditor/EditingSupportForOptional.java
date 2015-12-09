package arro.messageeditor;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;

import arro.messageeditor.EditingSupportForDesc.DeltaInfoOperation;
import arro.messageeditor.MessageEditor.Message;

public class EditingSupportForOptional extends EditingSupport {

	  private final TableViewer viewer;
	  private final CellEditor editor;
	  private final MessageEditor messageEditor;
	  
		class DeltaInfoOperation extends AbstractOperation {
			Message selectedMessage;
			Boolean undoRequired, redoRequired, newRequired;
			public DeltaInfoOperation(Message message, Boolean required) {
				super("Required change");
				this.selectedMessage = message;
				this.newRequired = required;
			}
			public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
				undoRequired = selectedMessage.getRequired();
				selectedMessage.setRequired(newRequired);
				return Status.OK_STATUS;
			}
			public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
				redoRequired = selectedMessage.getRequired();
				selectedMessage.setRequired(undoRequired);
				return Status.OK_STATUS;
			}
			public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
				undoRequired = selectedMessage.getRequired();
				selectedMessage.setRequired(redoRequired);
				return Status.OK_STATUS;
			}
		}

	  public EditingSupportForOptional(TableViewer viewer, MessageEditor messageEditor) {
	    super(viewer);
	    this.viewer = viewer;
	    this.messageEditor = messageEditor;
	    this.editor = new TextCellEditor(viewer.getTable());
	  }

	  protected CellEditor getCellEditor(Object element) {
		return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);
	  }

	  protected boolean canEdit(Object element) {
	    return true;
	  }

	  protected Object getValue(Object element) {
	    return ((Message) element).getRequired();
	  }

	  protected void setValue(Object element, Object userInputValue) {
	    //((Message) element).setRequired((boolean)userInputValue);
	    
	    IUndoableOperation operation = new DeltaInfoOperation(((Message) element), (boolean)userInputValue);
	  	operation.addContext(messageEditor.getUndoContext());
	  	try {
			messageEditor.getOperationHistory().execute(operation, null, null);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    viewer.update(element, null);
	    messageEditor.setDirty();
	  }

}
