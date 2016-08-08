package property;

import org.eclipse.graphiti.features.IFeatureProvider;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import arro.domain.ArroSequenceChart;

public class ArroPublishedActionsSection  extends ArroGenericSection {

	private TableViewer viewer;
    private ArroSequenceChart sfc = null;

	/**
	 * Read the contents from domain data and show in table.
	 */
	@Override
	public void refresh() {
	    PictogramElement pe = getSelectedPictogramElement();
	    
	    if (pe != null) {

			sfc = getSfc(pe);
			
			if (sfc != null) {
	            viewer.setInput(sfc.getPublishedActions());
			}
	    }
	    viewer.refresh();

	}
	
	
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
	    super.createControls(parent, tabbedPropertySheetPage);

		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);

		TableViewerColumn col1 = createTableViewerColumn("Action", 200, 0);
		ColumnLabelStrategy cls1 = new ColumnLabelStrategy() {
			@Override
			public String getText(Object element) {
				return ((String)element);
			}
			@Override
			public void setText(String value, Object element) {
			    if(sfc != null) {
			        sfc.updatePublishedActions((String)element, value);
			    }
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
		

		// ArrayContentProvider handles objects in arrays. Each element in the
		// table, ColumnsLabelProvider.getText is called to provide content.
		viewer.setContentProvider(new ArrayContentProvider());
		
		// get the content for the viewer, setInput will call getElements in the contentProvider
		//viewer.setInput(messageList);
		
		// make the selection available to other views
		//PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite().setSelectionProvider(viewer);
		// set the sorter for the table

		// define layout for the viewer
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		//gridData.horizontalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		//gridData.horizontalAlignment = GridData.FILL;
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


	public ArroSequenceChart getSfc(PictogramElement pe) {
	    if (pe != null) {
	       	IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();

	        Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

	        if (eObject.length != 0 && eObject[0] instanceof ArroSequenceChart) {
	        	return (ArroSequenceChart)(eObject[0]);
	        }
	    }
	    return null;
	}
	
	
	@Override
	public void update() {
		refresh();
	}

}
