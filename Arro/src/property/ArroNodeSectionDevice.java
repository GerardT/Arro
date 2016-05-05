package property;


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
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import arro.domain.ArroDevice;

public class ArroNodeSectionDevice extends GFPropertySection implements ITabbedPropertyConstants {

	private Text UrlTextVal;
	
    private String currentUrl = "";
    
    Diagram diagram;
	
	
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
	    super.createControls(parent, tabbedPropertySheetPage);
	
	    TabbedPropertySheetWidgetFactory factory = getWidgetFactory();
	    Composite composite = factory.createFlatFormComposite(parent);
	    
	    //valueLabel = factory.createCLabel(composite, "Node name:");
	    UrlTextVal = factory.createText(composite, "");
	    UrlTextVal.setEnabled(false);

	    handleProperty(factory, composite, UrlTextVal);
	}
	
	@Override
	public void refresh() {
//		DomainModule n = null;
//	    PictogramElement pe = getSelectedPictogramElement();
//	    if(pe != null && pe.eContainer() instanceof Diagram) {
//	    	diagram = (Diagram)pe.eContainer();
//	    	n = (DomainModule)getNode(diagram);
//	        if (n != null) {
//		        String name = n.getUrl();
//		        UrlTextVal.setText(name == null ? "" : name);
//	        }
//	    }
	    
	    PictogramElement pe = getSelectedPictogramElement();
	    if (pe != null) {
			ArroDevice n = getNode(pe);

	        if (n != null) {
		        String name = n.getUrl();
		        UrlTextVal.setText(name == null ? "" : name);
	        }
	    }

	}

	
	private void handleProperty(TabbedPropertySheetWidgetFactory factory, Composite composite, final Text nameText) {
	    FormData data;

	    data = new FormData();
	    data.left = new FormAttachment(0, STANDARD_LABEL_WIDTH);
	    data.right = new FormAttachment(100, 0);
	    data.top = new FormAttachment(0, VSPACE);
	    nameText.setLayoutData(data);
	
	    CLabel valueLabel = factory.createCLabel(composite, "URL:");
	    data = new FormData();
	    data.left = new FormAttachment(0, 0);
	    data.right = new FormAttachment(nameText, -HSPACE);
	    data.top = new FormAttachment(nameText, 0, SWT.CENTER);
	    valueLabel.setLayoutData(data);

	    nameText.addModifyListener(new ModifyListener() {

	    	public void modifyText(ModifyEvent e) {
	    		currentUrl = nameText.getText();
	    		if (currentUrl == null) {
	    			currentUrl = "";//$NON-NLS-1$
	    		}
	    		updateDialog();
	    	}
	    });
	}
	
	private void updateDialog() {
		final String typedValue = currentUrl;
		IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
				
			public boolean canExecute(IContext context) {
				return true;
			}
			public void execute(IContext context) {
//				DomainModule n = null;
//			    PictogramElement pe = getSelectedPictogramElement();
//			    if(pe != null && pe.eContainer() instanceof Diagram) {
//			    	diagram = (Diagram)pe.eContainer();
//			    	n = (DomainModule)getNode(diagram);
//			    	n.setUrl(typedValue);
//			    }
				final PictogramElement pe = getSelectedPictogramElement();
				ArroDevice n = getNode(pe);

				if (n == null)
					return;
				n.setUrl(typedValue);
    
				
				TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(getDiagram());
				domain.getCommandStack().execute(new RecordingCommand(domain) {
				    public void doExecute() {

		    			// update UI with new domain data
				    	UpdateContext updateContext = new UpdateContext(diagram);
				    	IUpdateFeature updateFeature = getFeatureProvider().getUpdateFeature(updateContext);
				    	updateFeature.update(updateContext);
//		    
//					    // revisit layout if necessary
//					    LayoutContext layoutContext = new LayoutContext(pe);
//					    ILayoutFeature layoutFeature = getFeatureProvider().getLayoutFeature(layoutContext);
//					    layoutFeature.layout(layoutContext);
				    }
				});
			}
		};
		CustomContext context = new CustomContext();
		execute(feature, context);
	}
	
	
//	public ArroNode getNode(PictogramElement pe) {
//	    if (pe != null) {
//	       	IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();
//
//	        Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);
//
//	        if (eObject.length != 0 && eObject[0] instanceof ArroNode) {
//	        	return (ArroNode)(eObject[0]);
//	        }
//	    }
//	    return null;
//	}
	public ArroDevice getNode(PictogramElement pe) {
	    if (pe != null) {
	       	IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();

	        Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

	        if (eObject.length != 0 && eObject[0] instanceof ArroDevice) {
	        	return (ArroDevice)(eObject[0]);
	        }
	    }
	    return null;
	}
}