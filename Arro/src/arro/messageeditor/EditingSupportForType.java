//package arro.messageeditor;
//
//import org.eclipse.core.commands.ExecutionException;
//import org.eclipse.core.commands.operations.AbstractOperation;
//import org.eclipse.core.commands.operations.IUndoableOperation;
//import org.eclipse.core.runtime.IAdaptable;
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.IStatus;
//import org.eclipse.core.runtime.Status;
//import org.eclipse.jface.viewers.CellEditor;
//import org.eclipse.jface.viewers.ComboBoxCellEditor;
//import org.eclipse.jface.viewers.EditingSupport;
//import org.eclipse.jface.viewers.TableViewer;
//
//import arro.messageeditor.EditingSupportForDesc.DeltaInfoOperation;
//import arro.messageeditor.MessageEditor.Message;
//
//public class EditingSupportForType extends EditingSupport {
//
//  private final TableViewer viewer;
//  private final MessageEditor messageEditor;
//  
//	class DeltaInfoOperation extends AbstractOperation {
//		Message selectedMessage;
//		String undoType, redoType, newType;
//		public DeltaInfoOperation(Message message, String type) {
//			super("Type change");
//			this.selectedMessage = message;
//			this.newType = type;
//		}
//		public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
//			undoType = selectedMessage.getType();
//			selectedMessage.setType(newType);
//			return Status.OK_STATUS;
//		}
//		public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
//			redoType = selectedMessage.getType();
//			selectedMessage.setType(undoType);
//			return Status.OK_STATUS;
//		}
//		public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
//			undoType = selectedMessage.getType();
//			selectedMessage.setType(redoType);
//			return Status.OK_STATUS;
//		}
//	}
//
//  public EditingSupportForType(TableViewer viewer, MessageEditor messageEditor) {
//    super(viewer);
//    this.viewer = viewer;
//    this.messageEditor = messageEditor;
//  }
//
//  @Override
//  protected CellEditor getCellEditor(Object element) {
//    String[] gender = new String[2];
//    gender[0] = "Integer";
//    gender[1] = "Boolean";
//
//    return new ComboBoxCellEditor(viewer.getTable(), gender);
//  }
//
//  @Override
//  protected boolean canEdit(Object element) {
//    return true;
//  }
//
//  @Override
//  protected Object getValue(Object element) {
//	Message m = (Message) element;
//    if (m.getType().equals("Integer")) {
//      return 0;
//    }
//    return 1;
//
//  }
//
//  @Override
//  protected void setValue(Object element, Object value) {
//	Message m = (Message) element;
//	String type;
//    if (((Integer) value) == 0) {
//      // m.setType("Integer");
//      type = "Integer";
//    } else {
//      //m.setType("Boolean");
//      type = "Boolean";
//    }
//    
//    IUndoableOperation operation = new DeltaInfoOperation(((Message) element), type);
//  	operation.addContext(messageEditor.getUndoContext());
//  	try {
//		messageEditor.getOperationHistory().execute(operation, null, null);
//	} catch (ExecutionException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}
//
//    viewer.update(element, null);
//    messageEditor.setDirty();
//  }
//} 