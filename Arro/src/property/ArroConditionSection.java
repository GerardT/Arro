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

import arro.domain.ArroCondition;
import arro.domain.ArroTransition;

public class ArroConditionSection extends ArroGenericSection {

	private TableViewer viewer;
	private ArrayList<ArroCondition> conditions = new ArrayList<ArroCondition>();
    private boolean listenerFlag = false;

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
		TableViewerColumn col1 = createTableViewerColumn("Node", 100, 0);
		ColumnLabelStrategy cls1 = new ColumnLabelStrategy() {
			@Override
			public String getText(Object element) {
				return ((ArroCondition)element).getName();
			}
			@Override
			public void setText(String value, Object element) {
				((ArroCondition)element).setName(value);
			}
			@Override
			public String[] getAcceptedValues(Object element) {
				return getAcceptedNodeNames((ArroCondition)element);
			}

		};
		col1.setLabelProvider(cls1);
	    col1.setEditingSupport(new EditingSupportForSelection(viewer, this, cls1));
		
		TableViewerColumn col2 = createTableViewerColumn("State", 100, 1);
		ColumnLabelStrategy cls2 = new ColumnLabelStrategy() {
			@Override
			public String getText(Object element) {
				return ((ArroCondition)element).getState();
			}
			@Override
			public void setText(String value, Object element) {
				((ArroCondition)element).setState(value);
			}
			@Override
			public String[] getAcceptedValues(Object element) {
				return getAcceptedStateNames((ArroCondition)element);
			}

		};
		col2.setLabelProvider(cls2);
	    col2.setEditingSupport(new EditingSupportForSelection(viewer, this, cls2));

		
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		// ArrayContentProvider handles objects in arrays. For each element in the
		// table ColumnsLabelProvider.getText is called to provide content.
		viewer.setContentProvider(new ArrayContentProvider());
		
		// get the content for the viewer, setInput will call getElements in the contentProvider
		viewer.setInput(conditions);
		
		// make the selection available to other views - but causes trouble!!
		//getSite().setSelectionProvider(viewer);
    }
    
    private String[] getAcceptedNodeNames(ArroCondition cond) {
    	String[] ret = { "aap", "noot" };
    	
    	return ret;
    }
    
    private String[] getAcceptedStateNames(ArroCondition cond) {
    	String[] ret = { "huis", "tuin" };
    	
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
	 * Refresh the contents of the control. Note this may trigger the update
	 * listener(s) attached.
	 */
    @Override
    public void refresh() {
	    PictogramElement pe = getSelectedPictogramElement();
	    
	    if (pe != null) {
            ArroTransition n = getTransition(pe);
			
			if (n != null) {
                // Temp disable listener; listener only needed if user does types new value.
                listenerFlag = false;

				conditions.clear();
		    	conditions.addAll(n.getConditions());
		    	if(!conditions.contains(new ArroCondition("", ""))) {
					conditions.add(new ArroCondition("", ""));
		    	}
		    	
		    	listenerFlag = true;
			}
	    }
//				HashMap<String, ArrayList<String>> combinations = transition.getParent().getParent().getStateCombinations();
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
    private boolean updateDomainAndPE() {
        final Args x = new Args();
        
        IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
                
            public boolean canExecute(IContext context) {
                return true;
            }
            public void execute(IContext context) {
                final PictogramElement pe = getSelectedPictogramElement();
                ArroTransition n = getTransition(pe);

                if (n != null) {
                    x.success = true;
                    
                    // Update the domain object
                    ArrayList<ArroCondition> current = n.getConditions();
                    current.clear();
                    current.addAll(conditions);
                   
//                    // Then sync PE with domain object
//                    UpdateContext updateContext = new UpdateContext(pe);
//                    IUpdateFeature updateFeature = getFeatureProvider().getUpdateFeature(updateContext);
//                    updateFeature.update(updateContext);
                }

            }
        };
        CustomContext context = new CustomContext();
        execute(feature, context);
        return x.success;
    }
    
    public ArroTransition getTransition(PictogramElement pe) {
        if (pe != null) {
            IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();

            Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

            if (eObject.length != 0 && eObject[0] instanceof ArroTransition) {
                return (ArroTransition)(eObject[0]);
            }
        }
        return null;
    }

	@Override
	public void update() {
		if(listenerFlag) {
			updateDomainAndPE();
		}
		refresh();
	}
}
