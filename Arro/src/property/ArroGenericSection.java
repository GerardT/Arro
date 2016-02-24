package property;


import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.graphiti.features.IFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.impl.AbstractFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramEditor;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import util.Misc;
import arro.domain.ArroPad;
import arro.editors.FunctionDiagramEditor;

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
}