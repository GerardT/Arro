package property;

import java.util.ArrayList;

import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICustomUndoableFeature;
import org.eclipse.graphiti.features.IFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.features.impl.AbstractFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import arro.Constants;
import arro.domain.ArroDevice;
import arro.domain.ArroModule;
import arro.domain.ArroNode;
import arro.domain.ArroParameter;
import arro.domain.ArroState;
import arro.domain.NonEmfDomainObject;
import util.Logger;

public class ArroParamSection  extends ArroGenericSection {

	private boolean isCodeDiagram;
	

	private TableViewer viewer;
	private ArrayList<ArroParameter> messageList = new ArrayList<ArroParameter>();

	public ArroParamSection(boolean b) {
		isCodeDiagram = b;
	    
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
	
	
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
	    super.createControls(parent, tabbedPropertySheetPage);

		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

	    if(isCodeDiagram) {
			TableViewerColumn col1 = createTableViewerColumn("External Name", 200, 0);
			ColumnLabelStrategy cls1 = new ColumnLabelStrategy() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getFormalKey();
				}
				@Override
				public void setText(String value, Object element) {
					((ArroParameter)element).setSubstitute(value);
					if(isDevice()) {
						((ArroParameter)element).setKey(value);
					}
				}
				@Override
				public String[] getAcceptedValues(Object element) {
					return null;
				}
			};
			col1.setLabelProvider(cls1);
		    col1.setEditingSupport(new EditingSupportForString(viewer, this, cls1));
			
			
			
			TableViewerColumn col2 = createTableViewerColumn("Value", 200, 1);
			ColumnLabelStrategy cls2 = new ColumnLabelStrategy() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getValue();
				}
				@Override
				public void setText(String value, Object element) {
					((ArroParameter)element).setValue(value);;
				}
				@Override
				public String[] getAcceptedValues(Object element) {
					return null;
				}
			};
			col2.setLabelProvider(cls2);
		    col2.setEditingSupport(new EditingSupportForString(viewer, this, cls2));
	    } else {
			TableViewerColumn col1 = createTableViewerColumn("Key", 200, 0);
			ColumnLabelStrategy cls1 = new ColumnLabelStrategy() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getFormalKey();
				}
				@Override
				public void setText(String value, Object element) {
					((ArroParameter)element).setKey(value);
				}
				@Override
				public String[] getAcceptedValues(Object element) {
					return null;
				}
			};
			col1.setLabelProvider(cls1);
		    col1.setEditingSupport(new EditingSupportForString(viewer, this, cls1));
			
			TableViewerColumn col2 = createTableViewerColumn("Value", 200, 1);
			ColumnLabelStrategy cls2 = new ColumnLabelStrategy() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getValue();
				}
				@Override
				public void setText(String value, Object element) {
					((ArroParameter)element).setValue(value);
				}
				@Override
				public String[] getAcceptedValues(Object element) {
					return null;
				}
			};
			col2.setLabelProvider(cls2);
		    col2.setEditingSupport(new EditingSupportForString(viewer, this, cls2));
			
			TableViewerColumn col3 = createTableViewerColumn("External Name", 200, 2);
			ColumnLabelStrategy cls3 = new ColumnLabelStrategy() {
				@Override
				public String getText(Object element) {
					return ((ArroParameter)element).getSubstitute();
				}
				@Override
				public void setText(String value, Object element) {
					((ArroParameter)element).setSubstitute(value);
					if(isDevice()) {
						((ArroParameter)element).setKey(value);
					}
				}
				@Override
				public String[] getAcceptedValues(Object element) {
					return null;
				}
			};
			col3.setLabelProvider(cls3);
		    col3.setEditingSupport(new EditingSupportForString(viewer, this, cls3));
			
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

	public boolean isDevice() {
		return isCodeDiagram;
	}


	/**
	 * Update the domain data according to new input in table.
	 * 
	 * @param parameterList contains the (updated) contents of the table.
	 */
	private void updateDomainAndPE(final ArrayList<ArroParameter> parameterList) {
		// make new clean list before storing.
		final ArrayList<ArroParameter> newList = new ArrayList<ArroParameter>();
		
		for(ArroParameter parm: parameterList) {
			if(!(parm.getFormalKey().equals(""))) {
				// Show default value if none entered
				if(parm.getValue().equals("")) { parm.setValue("0");
				
				}
				// do not include not fully specified entries
				System.out.println("newList.add " + parm);
				newList.add(parm);
			}
		}
		
        CustomContext context = new CustomContext();
        execute(new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
            
        	@Override
            public boolean canExecute(IContext context) {
                return true;
            }
            @Override
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

            }
        }, context);
	}

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
	
	
	@Override
	public void update() {
		updateDomainAndPE(messageList);
		refresh();
	}

}
