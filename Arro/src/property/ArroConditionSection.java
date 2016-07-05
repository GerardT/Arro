package property;

import java.util.ArrayList;

import org.eclipse.graphiti.features.IFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.impl.AbstractFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import arro.domain.ArroAction;
import arro.domain.ArroTransition;

public class ArroConditionSection extends ArroGenericSection {

    private Text nameTextVal;
    private CLabel valueLabel;
    CCombo allowedModes;
    
    private String name = "";
    private boolean listenerFlag = false;

    /**
     * Note: createControls is not called between selection PE of same type (e.g. transition).
     */
    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
        super.createControls(parent, tabbedPropertySheetPage);

        TabbedPropertySheetWidgetFactory factory = getWidgetFactory();
        Composite composite = factory.createFlatFormComposite(parent);

        valueLabel = factory.createCLabel(composite, "Condition:");
        nameTextVal = factory.createText(composite, "");
        
        allowedModes = factory.createCCombo(composite);

        addLayout(parent);

        nameTextVal.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                //name = Misc.checkString(nameTextVal);
                name = nameTextVal.getText();
                
                if(listenerFlag) {
                    if(!name.equals("") && updateDomainAndPE()) {
                        nameTextVal.setBackground(null);
                    } else {
                        // show error indication
                        Display display = Display.getCurrent();
                        Color color = display.getSystemColor(SWT.COLOR_RED);
                        nameTextVal.setBackground(color);
                    }
                }
            }
        });
    }
    
    
	/**
	 * Refresh the contents of the control - sync properties with domain info.
	 * Since this may trigger the listener(s) attached, we prevent that using
	 * listenerFlag.
	 */
    @Override
    public void refresh() {
        PictogramElement pe = getSelectedPictogramElement();
        if (pe != null) {
            ArroTransition n = getTransition();

            if (n != null) {
                // Make sure to first read these variables from domain
                name = n.getCondition() == null ? "" : n.getCondition();

                // Temp disable listener; listener only needed if user does types new value.
                listenerFlag = false;

                if(!(name.equals(nameTextVal))) {
                    nameTextVal.setText(name);
                }
                
                listenerFlag = true;
            }
        }
    }
    
    @SuppressWarnings("unused")
    private String[] getAcceptedNodeNames(ArroAction cond) {
        String[] ret;
        
        ArroTransition n = getTransition();
        ArrayList<String> list = n.getParent().getParent().getNodeNames();
        // Add empty option in order to remove the entry
        list.add(0, "");

        ret = list.toArray(new String[list.size()]);
        
        return ret;
    }

    /**
     * Provide layout and listeners for the controls.
     * 
     * @param parent
     */
    private void addLayout(Composite parent) {
        FormData data;

        // http://www.eclipse.org/forums/index.php/t/202738/
        final FormLayout layout = new FormLayout();
        layout.marginWidth = 4;
        layout.marginHeight = 0;
        layout.spacing = 10;//100;
        parent.setLayout(layout);

        data = new FormData();
        data.left = new FormAttachment(0, 0);
        data.top = new FormAttachment(nameTextVal, 0, SWT.CENTER);
        valueLabel.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(valueLabel, 0);
        //data.right = new FormAttachment(100, 0);
        //data.right = new FormAttachment(50, 0);
        data.width = 200;
        data.top = new FormAttachment(0, VSPACE);
        //data.width = SWT.BORDER;
        nameTextVal.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(0, 0);
        data.width = 200;
        //data.right = new FormAttachment(100, 0);
        //data.right = new FormAttachment(50, 0);
        data.top = new FormAttachment(nameTextVal, VSPACE);
        //data.width = SWT.BORDER;
        allowedModes.setLayoutData(data);
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
        final String currentName = name;
        final Args x = new Args();
        
        IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
                
            public boolean canExecute(IContext context) {
                return true;
            }
            public void execute(IContext context) {
                ArroTransition n = getTransition();

                if (n != null) {
                    x.success = true;
                    
                    // Update the domain object
                    n.setCondition(currentName);
                }

            }
        };
        CustomContext context = new CustomContext();
        execute(feature, context);
        return x.success;
    }
    
    public ArroTransition getTransition() {
        PictogramElement pe = getSelectedPictogramElement();
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
//		updateDomainAndPE();
		refresh();
	}
}
