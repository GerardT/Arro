package property;


import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;


/**
 * This class was supposed to take care of all undo/redo implementation. Right now it's a mess.
 *
 */
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
	
	void getEditingDomain() {
	    // how do we do that?
	    TransactionalEditingDomain domain = getDiagramTypeProvider().getDiagramBehavior().getEditingDomain();
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
	
    /**
     * Provide layout and listeners for the controls.
     * 
     * @param parent
     */
    protected void addLayout(Composite parent, Control control, int nrCols) {
        GridLayout layout = new GridLayout(2, false);
        parent.setLayout(layout);

        // define layout for the viewer
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalSpan = nrCols;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        control.setLayoutData(gridData);
    }
    /**
     * Create a column for the table viewer. Column is now associated with
     * this viewer.
     * 
     * @param title
     * @param bound
     * @param colNumber
     * @return
     */
    protected TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, int bound, final int colNumber) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
        final TableColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setWidth(bound);
        column.setResizable(true);
        column.setMoveable(true);
        return viewerColumn;
    }



}