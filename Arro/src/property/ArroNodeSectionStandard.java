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
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import arro.domain.ArroNode;
import util.Misc;

public class ArroNodeSectionStandard extends GFPropertySection implements ITabbedPropertyConstants {

	private Text nameTextVal;
	
    private String currentName = "";
    
    
	
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
	    super.createControls(parent, tabbedPropertySheetPage);
	
	    TabbedPropertySheetWidgetFactory factory = getWidgetFactory();
	    Composite composite = factory.createFlatFormComposite(parent);
	    
	    //valueLabel = factory.createCLabel(composite, "Node name:");
	    nameTextVal = factory.createText(composite, "");
	    
	    handleProperty(factory, composite, nameTextVal);
	}
	
	@Override
	public void refresh() {
	    PictogramElement pe = getSelectedPictogramElement();
	    if (pe != null) {
			ArroNode n = getNode(pe);

	        if (n != null) {
		        String name = n.getName();
		        nameTextVal.setText(name == null ? "" : name);
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
	
	    CLabel valueLabel = factory.createCLabel(composite, "Name:");
	    data = new FormData();
	    data.left = new FormAttachment(0, 0);
	    data.right = new FormAttachment(nameText, -HSPACE);
	    data.top = new FormAttachment(nameText, 0, SWT.CENTER);
	    valueLabel.setLayoutData(data);
	    
	    nameText.addModifyListener(new ModifyListener() {

	    	@Override
            public void modifyText(ModifyEvent e) {
    			currentName = Misc.checkString(nameText);
    			
    			if(!currentName.equals("") && updateDialog()) {
	    			nameText.setBackground(null);
    			} else {
	    			// show error indication
	    			Display display = Display.getCurrent();
	    			Color color = display.getSystemColor(SWT.COLOR_RED);
	    			nameText.setBackground(color);
	    		}
	    	}
	    });
	}
	
	// Use a class so we can declare it as final and use as closure.
	class X {
		public boolean success = false;
	}

	
	private boolean updateDialog() {
		final String typedValue = currentName;
		final X x = new X();
		IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
				
			@Override
            public boolean canExecute(IContext context) {
				return true;
			}
			@Override
            public void execute(IContext context) {
				final PictogramElement pe = getSelectedPictogramElement();
				ArroNode n = getNode(pe);

				if (n != null) {
					ArroNode m = n.getParent().getNodeByName(typedValue);
					if(m == null || m == n) {
						x.success = true;
						n.setName(typedValue);
						
						TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(getDiagram());
						domain.getCommandStack().execute(new RecordingCommand(domain) {
						    @Override
                            public void doExecute() {

				    			// update UI with new domain data
						    	UpdateContext updateContext = new UpdateContext(pe);
						    	IUpdateFeature updateFeature = getFeatureProvider().getUpdateFeature(updateContext);
						    	updateFeature.update(updateContext);
						    }
						});
						
					}
				}
			}
		};
		CustomContext context = new CustomContext();
		execute(feature, context);
		return x.success;
	}
	
	
	public ArroNode getNode(PictogramElement pe) {
	    if (pe != null) {
	       	IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();

	        Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

	        if (eObject.length != 0 && eObject[0] instanceof ArroNode) {
	        	return (ArroNode)(eObject[0]);
	        }
	    }
	    return null;
	}
}