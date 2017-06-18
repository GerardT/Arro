package arro.messageeditor;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;

import arro.messageeditor.MessageEditor.Message;


public class EditingSupportForDesc extends EditingSupport {

  private final TableViewer viewer;
  private final CellEditor editor;
  private final MessageEditor messageEditor;
  
	class DeltaInfoOperation extends AbstractOperation {
		Object undoList, redoList;
		Message selectedMessage;
		String undoDescription, redoDescription, newDescription;
		public DeltaInfoOperation(Message message, String description) {
			super("Description change");
			this.selectedMessage = message;
			this.newDescription = description;
		}
		@Override
        public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
			undoDescription = selectedMessage.getDescription();
			selectedMessage.setDescription(newDescription);
			return Status.OK_STATUS;
		}
		@Override
        public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
			redoDescription = selectedMessage.getDescription();
			selectedMessage.setDescription(undoDescription);
			return Status.OK_STATUS;
		}
		@Override
        public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			undoDescription = selectedMessage.getDescription();
			selectedMessage.setDescription(redoDescription);
			return Status.OK_STATUS;
		}
	}


  public EditingSupportForDesc(TableViewer viewer, MessageEditor messageEditor) {
    super(viewer);
    this.viewer = viewer;
    this.messageEditor = messageEditor;
    this.editor = new TextCellEditor(viewer.getTable());
  }

  @Override
  protected CellEditor getCellEditor(Object element) {
    return editor;
  }

  @Override
  protected boolean canEdit(Object element) {
    return true;
  }

  @Override
  protected Object getValue(Object element) {
    return ((Message) element).getDescription();
  }

  @Override
  protected void setValue(Object element, Object userInputValue) {
	  
//    ((Message) element).setDescription(String.valueOf(userInputValue));
    IUndoableOperation operation = new DeltaInfoOperation(((Message) element), String.valueOf(userInputValue));
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