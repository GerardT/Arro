package property;

import java.util.ArrayList;

import org.eclipse.graphiti.features.IFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.impl.AbstractFeature;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import arro.domain.ArroAction;
import arro.domain.ArroRequest;
import arro.domain.ArroTransition;

public class ArroSetActionsSection extends ArroGenericSection {

	private TableViewer viewer;
	
	// Do not re-create this array, just clear it and refill.
	ArrayList<ArroAction> actions = new ArrayList<ArroAction>();
    ArroTransition transition = null;

    /**
     * Note: createControls is not called between selection PE of same type (e.g. transition).
     */
    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
        super.createControls(parent, tabbedPropertySheetPage);

		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		addLayout(parent, viewer.getControl(), 2);
		
		// first column is for the type
		TableViewerColumn col1 = createTableViewerColumn(viewer, "Node", 200, 0);
		ColumnLabelStrategy cls1 = new ColumnLabelStrategy() {
			@Override
			public String getText(Object element) {
				return ((ArroAction)element).getName();
			}
			@Override
			public void setText(String value, Object element) {
			    ((ArroAction)element).setName(value);
			    storeTable();
			}
			@Override
			public String[] getAcceptedValues(Object element) {
				return getAcceptedNodeNames((ArroAction)element);
			}
            @Override
            public String obsolete(Object element) {
                return ((ArroAction)element).getName();
            }

		};
		col1.setLabelProvider(cls1);
	    col1.setEditingSupport(new EditingSupportForSelection(viewer, this, cls1));
		
		TableViewerColumn col2 = createTableViewerColumn(viewer, "Action", 200, 1);
		ColumnLabelStrategy cls2 = new ColumnLabelStrategy() {
			@Override
			public String getText(Object element) {
				return ((ArroAction)element).getState();
			}
			@Override
			public void setText(String value, Object element) {
	            ((ArroAction)element).setState(value);

                storeTable();
			}
			@Override
			public String[] getAcceptedValues(Object element) {
				return getAcceptedPublishedActions((ArroAction)element);
			}
            @Override
            public String obsolete(Object element) {
                return ((ArroAction)element).getState();
            }


		};
		col2.setLabelProvider(cls2);
	    col2.setEditingSupport(new EditingSupportForSelection(viewer, this, cls2));

		
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setEnabled(true);

        // ArrayContentProvider handles objects in arrays. Each element in the
        // table, ColumnsLabelProvider.getText is called to provide content.
        viewer.setContentProvider(new ArrayContentProvider());
        
        // get the content for the viewer, setInput will call getElements in the contentProvider
        viewer.setInput(actions);
		
		// TODO make the selection available to other views - but causes trouble!!
		//getSite().setSelectionProvider(viewer);
		
    }
    
    
    private String[] getAcceptedNodeNames(ArroAction cond) {
        String[] ret = {""};
        
        if(transition != null) {
            ArrayList<String> list = transition.getParent().getParent().getNodeNames();
            // Add empty option in order to remove the entry
            list.add(0, "");

            ret = list.toArray(new String[list.size()]);
        } else {
            System.out.println("getAcceptedNodeNames: no transition selected");
        }
        
        return ret;
    }
    
    private String[] getAcceptedPublishedActions(ArroAction cond) {
        String[] ret = {""};
        String name = cond.getName(); // node name
        
        if(name.equals("")) {
            return ret;
        }
        
        if(transition != null) {
            try {
                ArrayList<ArroRequest> list = transition.getParent().getParent().getNodeByName(name).getAssociatedModule().getPublishedActions();

                ret = list.toArray(new String[list.size()]);
            } catch (RuntimeException e) {
                return ret;
            }
        } else {
            System.out.println("getAcceptedPublishedActions: no transition selected");
        }
        
        return ret;
    }
    
    @SuppressWarnings("unused")
    private String[] getAcceptedStateNames(ArroAction cond) {
       	String[] ret = {""};
    	String name = cond.getName(); // node name
    	
    	if(name.equals("")) {
    		return ret;
    	}
    	
        if(transition != null) {
            try {
                ArrayList<String> list = transition.getParent().getParent().getNodeByName(name).getAssociatedModule().getStateNames();

                ret = list.toArray(new String[list.size()]);
            } catch (RuntimeException e) {
                return ret;
            }
        } else {
            System.out.println("getAcceptedStateNames: no transition selected");
        }
    	
    	return ret;
    }
    
	/**
	 * Refresh the contents of the control - sync properties with domain info.
	 * 
	 * Called every time a PE is selected.
	 */
    @Override
    public void refresh() {
        transition = getTransition();
        
        if(transition != null) {
            loadTable(transition);
            
		} else {
		    // when apparently nothing selected.
		    actions.clear();
		}
        viewer.refresh();
    }
    

    
    private void loadTable(ArroTransition n) {
        ArrayList<ArroAction> tmp = n.getEntryActions();
        
        actions.clear();
        for(ArroAction entry : tmp) {
            actions.add(new ArroAction(entry.getName(), entry.getState()));
        }
        actions.add(new ArroAction("", ""));
    }

    /**
     * Update the Domain object that is referenced by the Pictogram Element.
     * 
     * Then update the Pictogram Element so it reflects the value that has been updated
     * in the domain by the user dialog.
     * 
     * @return true if domain object (referenced by PE) was updated successfully.
     */
    private void storeTable() {
        
        // make new clean list before storing.
        final ArrayList<ArroAction> newList = new ArrayList<ArroAction>();
        for(ArroAction entry : actions) {
            if(!(entry.getName().equals("") /*|| entry.getState().equals("")*/)) {
                System.out.println("Store actions " + entry.getName() + " " + entry.getState());
                newList.add(new ArroAction(entry.getName(), entry.getState()));
            }
        }

        IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
                
            @Override
            public boolean canExecute(IContext context) {
                return true;
            }
            @Override
            public void execute(IContext context) {
                ArroTransition n = getTransition();

                n.setEntryActions(newList);
            }
        };
        CustomContext context = new CustomContext();
        execute(feature, context);
    }
    
    public ArroTransition getTransition() {
        PictogramElement pe = getSelectedPictogramElement();
        if(pe != null && pe instanceof FreeFormConnection) {
	    
            IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();
            
            Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

            if (eObject.length != 0 && eObject[0] instanceof ArroTransition) {
                return (ArroTransition)(eObject[0]);
            }
        }
        System.out.println("No ArroTransition found!");
        return null;
    }

	@Override
	public void update() {
		//refresh();
	}
}
