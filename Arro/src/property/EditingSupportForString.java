package property;

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


public class EditingSupportForString extends EditingSupport {

  private final TableViewer viewer;
  private final CellEditor editor;
  private final ArroGenericSection paramEditor;
  private ColumnLabelStrategy gs;

	class DeltaInfoOperation extends AbstractOperation {
		Object undoList, redoList;
		Object selectedRow;
		String undoName, redoName, newName;
		public DeltaInfoOperation(Object element, String name) {
			super("Name change");
			this.selectedRow = element;
			this.newName = name;
		}
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
			undoName = gs.getText(selectedRow);
			gs.setText(newName, selectedRow);
			paramEditor.update();
			return Status.OK_STATUS;
		}
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
			redoName = gs.getText(selectedRow);
			gs.setText(undoName, selectedRow);
			paramEditor.update();
			return Status.OK_STATUS;
		}
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			undoName = gs.getText(selectedRow);
			gs.setText(redoName, selectedRow);
			paramEditor.update();
			return Status.OK_STATUS;
		}
	}
	
  public EditingSupportForString(TableViewer viewer, ArroGenericSection paramEditor, ColumnLabelStrategy gs) {
    super(viewer);
    this.viewer = viewer;
    this.paramEditor = paramEditor;
    this.editor = new TextCellEditor(viewer.getTable());
    this.gs = gs;
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
    return gs.getText(element);
  }

  @Override
  protected void setValue(Object element, Object userInputValue) {
    IUndoableOperation operation = new DeltaInfoOperation((element), String.valueOf(userInputValue));
  	operation.addContext(paramEditor.getUndoContext());
  	try {
		paramEditor.getOperationHistory().execute(operation, null, null);
	} catch (ExecutionException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
//    gs.setText(String.valueOf(userInputValue), element);
//    paramEditor.update();


    viewer.update(element, null);

  }
} 