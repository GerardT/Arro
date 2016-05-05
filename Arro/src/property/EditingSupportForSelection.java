package property;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;

public class EditingSupportForSelection extends EditingSupport {

	private final TableViewer viewer;
	private final CellEditor editor;
	private final ArroGenericSection paramEditor;
	private ColumnLabelStrategy gs;
	private String[] acceptedValues;

	class DeltaInfoOperation extends AbstractOperation {
		Object undoList, redoList;
		Object selectedRow;
		String undoName, redoName, newName;

		public DeltaInfoOperation(Object element, String name) {
			super("Change");
			this.selectedRow = element;
			this.newName = name;
			paramEditor.update();
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

	public EditingSupportForSelection(TableViewer viewer,
			ArroGenericSection paramEditor, ColumnLabelStrategy gs) {
		super(viewer);
		String[] empty = { " " };
		this.viewer = viewer;
		this.paramEditor = paramEditor;
		this.editor = new ComboBoxCellEditor(viewer.getTable(), empty, SWT.READ_ONLY);
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
		acceptedValues = gs.getAcceptedValues(element);
		if (acceptedValues != null) {
			((ComboBoxCellEditor) this.editor).setItems(acceptedValues);
		}
		String elt = gs.getText(element);
		
		// determine index of elt in acceptedValues
		int index;
		for(index = 0; index < acceptedValues.length; index++) {
			if(acceptedValues[index].equals(elt)) {
				break;
			}
		}
		if(index == acceptedValues.length) {
			index = 0; // what else can we do?
		}
		return new Integer(index);
	}

	@Override
	protected void setValue(Object element, Object userInputValue) {
		if(!(userInputValue instanceof Integer)) return;
				
		Integer i = (Integer)userInputValue;
		
		if(i < 0) return;
		
		IUndoableOperation operation = new DeltaInfoOperation((element), acceptedValues[i]);
		operation.addContext(paramEditor.getUndoContext());
		try {
		    String tmp = gs.getText(element);
		    if(!tmp.equals(acceptedValues[i])) {
		        paramEditor.getOperationHistory().execute(operation, null, null);
		    }
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		viewer.update(element, null);

	}
}