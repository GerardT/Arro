package property;

import java.util.ArrayList;

import org.eclipse.graphiti.features.IFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.impl.AbstractFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import arro.domain.ArroAction;
import arro.domain.ArroStep;

public class ArroStateEntrySection extends ArroGenericSection {

	private TableViewer viewer;
    @SuppressWarnings("unused")
	private boolean listenerFlag = false;  // unused since we don't have listeners on property.

    /**
     * Note: createControls is not called between selection PE of same type (e.g. transition).
     */
    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
        super.createControls(parent, tabbedPropertySheetPage);

		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		addLayout(parent, viewer.getControl());
		
		// first column is for the type
		TableViewerColumn col1 = createTableViewerColumn("Node", 200, 0);
		ColumnLabelStrategy cls1 = new ColumnLabelStrategy() {
			@Override
			public String getText(Object element) {
				return ((ArroAction)element).getName();
			}
			@Override
			public void setText(String value, Object element) {
			    ArroAction act = new ArroAction(value, "");
			    updateDomainAndPE((ArroAction) element, act);
			}
			@Override
			public String[] getAcceptedValues(Object element) {
				return getAcceptedNodeNames((ArroAction)element);
			}

		};
		col1.setLabelProvider(cls1);
	    col1.setEditingSupport(new EditingSupportForSelection(viewer, this, cls1));
		
		TableViewerColumn col2 = createTableViewerColumn("Action", 200, 1);
		ColumnLabelStrategy cls2 = new ColumnLabelStrategy() {
			@Override
			public String getText(Object element) {
				return ((ArroAction)element).getState();
			}
			@Override
			public void setText(String value, Object element) {
                ArroAction act = new ArroAction(((ArroAction)element).getName(), value);
                updateDomainAndPE((ArroAction) element, act);
			}
			@Override
			public String[] getAcceptedValues(Object element) {
				return getAcceptedStateNames((ArroAction)element);
			}

		};
		col2.setLabelProvider(cls2);
	    col2.setEditingSupport(new EditingSupportForSelection(viewer, this, cls2));

		
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setEnabled(true);

		// ArrayContentProvider handles objects in arrays. For each element in the
		// table ColumnsLabelProvider.getText is called to provide content.
		viewer.setContentProvider(new ArrayContentProvider());
		
		// make the selection available to other views - but causes trouble!!
		//getSite().setSelectionProvider(viewer);
    }
    
    private String[] getAcceptedNodeNames(ArroAction cond) {
       	String[] ret;
       	
        ArroStep n = getStep();
        ArrayList<String> list = n.getParent().getParent().getNodeNames();
        // Add empty option in order to remove the entry
        list.add(0, "");

		ret = list.toArray(new String[list.size()]);
    	
    	return ret;
    }
    
    private String[] getAcceptedStateNames(ArroAction cond) {
       	String[] ret = { "" };
    	String name = cond.getName(); // node name
    	
    	if(name.equals("")) {
    		return ret;
    	}
    	
        ArroStep n = getStep();
        try {
			ArrayList<String> list = n.getParent().getParent().getNodeByName(name).getAssociatedModule().getStateNames();

			ret = list.toArray(new String[list.size()]);
		} catch (RuntimeException e) {
			return ret;
		}
    	
    	return ret;
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
	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	/**
	 * Refresh the contents of the control - sync properties with domain info.
	 * Since this may trigger the listener(s) attached, we prevent that using
	 * listenerFlag.
	 */
    @Override
    public void refresh() {
        ArroStep n = getStep();
		
		if (n != null) {
            // Temp disable listener; listener only needed if user does types new value.
            listenerFlag = false;
            
            // get the content for the viewer, setInput will call getElements in the contentProvider
            viewer.setInput(n.getEntryActions());
            
	    	listenerFlag = true;
		}
	    viewer.refresh();

    }
    /**
     * Provide layout and listeners for the controls.
     * 
     * @param parent
     */
    private void addLayout(Composite parent, Control control) {
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);

		// define layout for the viewer
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		control.setLayoutData(gridData);
		
    }


    
    // Use a class so we can declare it as final and use as closure.
    class Args {
        public boolean success = false;
    }

    /**
     * Update the Domain object that is referenced by the Pictogram Element.
     * 
     * Then update the Pictogram Element so it reflects the value that has been updated
     * in the domain by the user dialog.
     * 
     * @return true if domain object (referenced by PE) was updated successfully.
     */
    private boolean updateDomainAndPE(ArroAction action, ArroAction newValue) {
        final Args x = new Args();
        
        IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
                
            @Override
            public boolean canExecute(IContext context) {
                return true;
            }
            @Override
            public void execute(IContext context) {
                ArroStep n = getStep();

                if (n != null) {
                    n.updateEntry((ArroAction)(context.getProperty("p1")), (ArroAction)(context.getProperty("p2")));
                    x.success = true;
                }
            }
        };
        CustomContext context = new CustomContext();
        context.putProperty("p1", action);
        context.putProperty("p2", newValue);
        execute(feature, context);
        return x.success;
    }
    
    public ArroStep getStep() {
	    PictogramElement pe = getSelectedPictogramElement();
	    
        if (pe != null) {
            IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();

            Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

            if (eObject.length != 0 && eObject[0] instanceof ArroStep) {
                return (ArroStep)(eObject[0]);
            }
        }
        return null;
    }

	@Override
	public void update() {
//		updateDomainAndPE();
		refresh();
	}
}
