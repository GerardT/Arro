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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import arro.domain.ArroRequest;
import arro.domain.ArroSequenceChart;

public class ArroPublishedActionsSection  extends ArroGenericSection {

    private TableViewer viewer;
    
    // Do not re-create this array, just clear it and refill.
    ArrayList<ArroRequest> requests = new ArrayList<ArroRequest>();
    private ArroSequenceChart sfc = null;

	
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
	    super.createControls(parent, tabbedPropertySheetPage);
		
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        
        addLayout(parent, viewer.getControl(), 1);

		TableViewerColumn col1 = createTableViewerColumn(viewer, "Request", 200, 0);
		ColumnLabelStrategy cls1 = new ColumnLabelStrategy() {
		    
			@Override
			public String getText(Object element) {
				return ((ArroRequest)element).getValue();
			}
			@Override
			public void setText(String value, Object element) {
                ((ArroRequest)element).setValue(value);
                storeTable();
			}
			@Override
			public String[] getAcceptedValues(Object element) {
				return null;
			}
		};
		col1.setLabelProvider(cls1);
	    col1.setEditingSupport(new EditingSupportForString(viewer, this, cls1));
		
        final Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setEnabled(true);

        // ArrayContentProvider handles objects in arrays. Each element in the
        // table, ColumnsLabelProvider.getText is called to provide content.
        viewer.setContentProvider(new ArrayContentProvider());
        
        // get the content for the viewer, setInput will call getElements in the contentProvider
        viewer.setInput(requests);
        
        // TODO make the selection available to other views - but causes trouble!!
        //getSite().setSelectionProvider(viewer);
        
	}

    /**
     * Refresh the contents of the control - sync properties with domain info.
     * 
     * Called every time a PE is selected.
     */
    @Override
    public void refresh() {
        sfc = getSfc();
        
        if(sfc != null) {
            loadTable(sfc);
            
        } else {
            // when apparently nothing selected.
            requests.clear();
        }
        viewer.refresh();
    }
    

	public ArroSequenceChart getSfc() {
        PictogramElement pe = getSelectedPictogramElement();
        
	    if (pe != null) {
	       	IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();

	        Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

	        if (eObject.length != 0 && eObject[0] instanceof ArroSequenceChart) {
	        	return (ArroSequenceChart)(eObject[0]);
	        }
	    }
	    return null;
	}
	
    
    private void loadTable(ArroSequenceChart n) {
        ArrayList<ArroRequest> tmp = n.getPublishedActions();
        
        requests.clear();
        for(ArroRequest entry : tmp) {
            requests.add(entry);
        }
        requests.add(new ArroRequest());
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
        final ArrayList<ArroRequest> newList = new ArrayList<ArroRequest>();
        for(ArroRequest entry : requests) {
            if(!(entry.getValue().equals(""))) {
                System.out.println("Store requests " + entry.getValue());
                newList.add(entry);
            }
        }

        IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
                
            @Override
            public boolean canExecute(IContext context) {
                return true;
            }
            @Override
            public void execute(IContext context) {
                ArroSequenceChart n = getSfc();

                n.setPublishedActions(newList);
            }
        };
        CustomContext context = new CustomContext();
        execute(feature, context);
    }
    
	
	@Override
	public void update() {
		//refresh();
	}

}
