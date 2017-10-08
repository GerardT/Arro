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


public class EditingSupportForName extends EditingSupport {

  private final TableViewer viewer;
  private final CellEditor editor;
  private final MessageEditor messageEditor;

	class DeltaInfoOperation extends AbstractOperation {
		Object undoList, redoList;
		Message selectedMessage;
		String undoName, redoName, newName;
		public DeltaInfoOperation(Message message, String name) {
			super("Name change");
			this.selectedMessage = message;
			this.newName = name;
		}
		@Override
        public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
			undoName = selectedMessage.getName();
			selectedMessage.setName(newName);
			return Status.OK_STATUS;
		}
		@Override
        public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
			redoName = selectedMessage.getName();
			selectedMessage.setName(undoName);
			return Status.OK_STATUS;
		}
		@Override
        public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			undoName = selectedMessage.getName();
			selectedMessage.setName(redoName);
			return Status.OK_STATUS;
		}
	}
	
public EditingSupportForName(TableViewer viewer, MessageEditor messageEditor) {
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
    return ((Message) element).getName();
  }

  @Override
  protected void setValue(Object element, Object userInputValue) {
    //((Message) element).setName(String.valueOf(userInputValue));
	  
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