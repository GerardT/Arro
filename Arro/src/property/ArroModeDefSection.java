package property;

import java.util.ArrayList;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import util.Logger;
import arro.domain.ArroDevice;
import arro.domain.ArroNode;
import arro.domain.ArroParameter;
import arro.domain.ArroState;
import arro.domain.NonEmfDomainObject;

public class ArroModeDefSection extends GFPropertySection implements ITabbedPropertyConstants {

	private UndoActionHandler undoAction;
	private RedoActionHandler redoAction;
	private IUndoContext undoContext;
	

	private TableViewer viewer;
	private ArrayList<ArroParameter> messageList = new ArrayList<ArroParameter>();

	public ArroModeDefSection() {
		
	    undoContext = new ObjectUndoContext(this);
	    IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
	    undoAction = new UndoActionHandler(part.getSite(), undoContext);
	    redoAction = new RedoActionHandler(part.getSite(), undoContext);
	    
	}
	
    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
        super.createControls(parent, tabbedPropertySheetPage);

		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		addLayout(parent, viewer.getControl());
		
		// first column is for the type
		TableViewerColumn col1 = createTableViewerColumn("Mode", 100, 0);
		col1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((ArroParameter)element).getFormalKey();
			}
		});
	    //col1.setEditingSupport(new EditingSupportForSubst(viewer, this));
		
		TableViewerColumn col2 = createTableViewerColumn("Description", 100, 1);
		col2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((ArroParameter)element).getValue();
			}
		});
	    //col2.setEditingSupport(new EditingSupportForValue(viewer, this));

		
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		// ArrayContentProvider handles objects in arrays. For each element in the
		// table ColumnsLabelProvider.getText is called to provide content.
		viewer.setContentProvider(new ArrayContentProvider());
		
		// get the content for the viewer, setInput will call getElements in the contentProvider
		viewer.setInput(messageList);
		
		// make the selection available to other views
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite().setSelectionProvider(viewer);
		// set the sorter for the table
		

    }
    
	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

    @Override
    public void refresh() {
//        PictogramElement pe = getSelectedPictogramElement();
//        if (pe != null) {
//            ArroState n = getState(pe);
//
//            if (n != null) {
//                // Make sure to first read these variables from domain
//                name = n.getName() == null ? "" : n.getName();
//
//                // Temp disable listener; listener only needed if user does types new value.
//                listenerFlag = false;
//
//                if(!(name.equals(nameTextVal))) {
//                    nameTextVal.setText(name);
//                }
//                listenerFlag = true;
//            }
//        }
	    PictogramElement pe = getSelectedPictogramElement();
	    NonEmfDomainObject n = null;
	    
	    if (pe != null) {
			n = getState(pe);
			
			if (n != null && n instanceof ArroDevice) {
				Logger.out.trace(Logger.STD, "Device get parameters ");
				messageList.clear();
		    	messageList.addAll(((ArroDevice)n).getParameterList());
				messageList.add(new ArroParameter("", "", "", ""));
				
				//Logger.out.trace(Logger.STD, "Value " + messageList.get(0).getValue());
				//Logger.out.trace(Logger.STD, "Subst " + messageList.get(0).getSubstitute());
			}
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
//    private boolean updateDomainAndPE() {
//        final String currentName = name;
//        final Args x = new Args();
//        
//        IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
//                
//            public boolean canExecute(IContext context) {
//                return true;
//            }
//            public void execute(IContext context) {
//                final PictogramElement pe = getSelectedPictogramElement();
//                ArroState n = getState(pe);
//
//                if (n != null) {
//                    x.success = true;
//                    
//                    // Update the domain object
//                    n.setName(currentName);
//                    
//                    // Then sync PE with domain object
//                    UpdateContext updateContext = new UpdateContext(pe);
//                    IUpdateFeature updateFeature = getFeatureProvider().getUpdateFeature(updateContext);
//                    updateFeature.update(updateContext);
//                }
//
//            }
//        };
//        CustomContext context = new CustomContext();
//        execute(feature, context);
//        return x.success;
//    }
    
    public ArroState getState(PictogramElement pe) {
        if (pe != null) {
               IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();

            Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

            if (eObject.length != 0 && eObject[0] instanceof ArroState) {
                return (ArroState)(eObject[0]);
            }
        }
        return null;
    }
}
