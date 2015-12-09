package property;

import java.util.ArrayList;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.graphiti.features.IFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.impl.AbstractFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramEditor;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import util.Logger;
import arro.domain.ArroDevice;
import arro.domain.ArroNode;
import arro.domain.ArroParameter;
import arro.domain.NonEmfDomainObject;
import arro.editors.SubGraphitiEditor;

public class ArroParamSection extends GFPropertySection/*AbstractPropertySection*/ {

	private UndoActionHandler undoAction;
	private RedoActionHandler redoAction;
	private IUndoContext undoContext;
	private boolean isDeviceDiagram;
	

	private TableViewer viewer;
	private ArrayList<ArroParameter> messageList = new ArrayList<ArroParameter>();

	public ArroParamSection(boolean b) {
		
		isDeviceDiagram = b;
	    undoContext = new ObjectUndoContext(this);
	    IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
	    undoAction = new UndoActionHandler(part.getSite(), undoContext);
	    redoAction = new RedoActionHandler(part.getSite(), undoContext);
	    
	}
	

	/**
	 * Read the contents from domain data and show in table.
	 */
	@Override
	public void refresh() {
	    PictogramElement pe = getSelectedPictogramElement();
	    NonEmfDomainObject n = null;
	    
	    if (pe != null) {
			n = getNode(pe);
			
			if (n != null) {

				
				if(n instanceof ArroNode) {
					Logger.out.trace(Logger.STD, "Node " + ((ArroNode)n).getName() + " get parameters ");
					messageList.clear();
			    	messageList.addAll(((ArroNode)n).getParameterList());
					messageList.add(new ArroParameter("", "", "", ""));
				} else if(n instanceof ArroDevice) {
					Logger.out.trace(Logger.STD, "Device get parameters ");
					messageList.clear();
			    	messageList.addAll(((ArroDevice)n).getParameterList());
					messageList.add(new ArroParameter("", "", "", ""));
				}
				
				Logger.out.trace(Logger.STD, "Value " + messageList.get(0).getValue());
				Logger.out.trace(Logger.STD, "Subst " + messageList.get(0).getSubstitute());
			}
	    }
	    viewer.refresh();

	}
	
	@SuppressWarnings("deprecation")
	public void update() {
		updateModelData(messageList);

	    IDiagramEditor editor = getDiagramEditor();
	    if(editor instanceof SubGraphitiEditor) {
	    	((SubGraphitiEditor)editor).updateDirtyState();
	    }
	}

	
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
	    super.createControls(parent, tabbedPropertySheetPage);

		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		
		String[] titlesStandard = { "Key", "Value", "External Name" };
		String[] titlesDevice   = { "External Name", "Value",  };
		String[] titles;
		if(isDeviceDiagram) {
			titles = titlesDevice;
		}else {
			titles = titlesStandard;
		}
		int[] bounds = { 100, 100, 100, 100 };

	    if(isDeviceDiagram) {
			// first column is for the type
			TableViewerColumn col1 = createTableViewerColumn(titles[0], bounds[0], 0);
			col1.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getFormalKey();
				}
			});
		    col1.setEditingSupport(new EditingSupportForSubst(viewer, this));
			
			TableViewerColumn col2 = createTableViewerColumn(titles[1], bounds[1], 1);
			col2.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getValue();
				}
			});
		    col2.setEditingSupport(new EditingSupportForValue(viewer, this));
	    } else {
			// first column is for the type
			TableViewerColumn col1 = createTableViewerColumn(titles[0], bounds[0], 0);
			col1.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getFormalKey();
				}
			});
			
			TableViewerColumn col2 = createTableViewerColumn(titles[1], bounds[1], 1);
			col2.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getValue();
				}
			});
		    col2.setEditingSupport(new EditingSupportForValue(viewer, this));

			TableViewerColumn col3 = createTableViewerColumn(titles[2], bounds[2], 2);
			col3.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getSubstitute();
				}
			});
		    col3.setEditingSupport(new EditingSupportForSubst(viewer, this));
	    }

		
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		// ArrayContentProvider handles objects in arrays. Each element in the
		// table, ColumnsLabelProvider.getText is called to provide content.
		viewer.setContentProvider(new ArrayContentProvider());
		
		// get the content for the viewer, setInput will call getElements in the contentProvider
		viewer.setInput(messageList);
		
		// make the selection available to other views
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite().setSelectionProvider(viewer);
		// set the sorter for the table

		// define layout for the viewer
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "Arro.viewer");
////		makeActions();
//		hookContextMenu();
////		hookDoubleClickAction();
//		contributeToActionBars();
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
				SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

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

//		private void setUndoRedoActionHandlers() {
//
//		    final IActionBars actionBars = getEditorSite().getActionBars();
//		    actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
//		    actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
//		    actionBars.updateActionBars();
//		}

	public NonEmfDomainObject getNode(PictogramElement pe) {
	    if (pe != null) {
	       	IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();

	        Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

	        if (eObject.length != 0 && eObject[0] instanceof NonEmfDomainObject) {
	        	return (NonEmfDomainObject)(eObject[0]);
	        }
	    }
	    return null;
	}
	public boolean isDevice() {
		return isDeviceDiagram;
	}

	/**
	 * Update the domain data according to new input in table.
	 * 
	 * @param parameterList contains the (updated) contents of the table.
	 */
	private void updateModelData(final ArrayList<ArroParameter> parameterList) {
		// make new clean list before storing.
		final ArrayList<ArroParameter> newList = new ArrayList<ArroParameter>();
		
		for(ArroParameter parm: parameterList) {
			if(!(parm.getFormalKey().equals("") || parm.getValue().equals(""))) {
				// do not include not fully specified entries
				System.out.println("newList.add " + parm);
				newList.add(parm);
			}
		}
		IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
				
			public boolean canExecute(IContext context) {
				return true;
			}
			public void execute(IContext context) {
				final PictogramElement pe = getSelectedPictogramElement();
				NonEmfDomainObject n = getNode(pe);

				if (n != null) {
					if(n instanceof ArroNode) {
						for(ArroParameter p: newList){
							System.out.println("Node " + ((ArroNode)n).getName() + " new parameters " + p.getSubstitute());
						}
						((ArroNode)n).setParameterList(newList);
					} else if(n instanceof ArroDevice) {
						for(ArroParameter p: newList){
							System.out.println("Device new parameters " + p.getSubstitute());
						}
						((ArroDevice)n).setParameterList(newList);
					}
				}
				
//				TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(getDiagram());
//				domain.getCommandStack().execute(new RecordingCommand(domain) {
//				    public void doExecute() {
//
//		    			// update UI with new model data
//				    	UpdateContext updateContext = new UpdateContext(pe);
//				    	IUpdateFeature updateFeature = getFeatureProvider().getUpdateFeature(updateContext);
//				    	updateFeature.update(updateContext);
//				    }
//				});
			}
		};
		CustomContext context = new CustomContext();
		execute(feature, context);
	}

}
