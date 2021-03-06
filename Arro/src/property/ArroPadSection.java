package property;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import arro.domain.ArroPad;
import util.Misc;

public class ArroPadSection extends GFPropertySection implements ITabbedPropertyConstants {

	private Text nameTextVal;
    private Button bIn, bOut, bRunCycle;
    private CLabel valueLabel;
    
    private String currentName = "";
    private boolean currentAsInput = true;
    private boolean currentRunCycle = true;
    private boolean hasConnection = false;
    private boolean updateSuccess = false;
	
	
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
	    super.createControls(parent, tabbedPropertySheetPage);
	
	    TabbedPropertySheetWidgetFactory factory = getWidgetFactory();
	    Composite composite = factory.createFlatFormComposite(parent);
	    
	    valueLabel = factory.createCLabel(composite, "Pad name:");
	    nameTextVal = factory.createText(composite, "");
	    bIn = factory.createButton(parent, "Input", SWT.RADIO);
	    bOut = factory.createButton(parent, "Output", SWT.RADIO);
	    bRunCycle = factory.createButton(parent, "Trigger cycle", SWT.CHECK);
	    
	    handleProperty(parent, composite, nameTextVal);
	}
	
	/**
	 * Update the controls to match the values in the model
	 */
	@Override
	public void refresh() {
	    PictogramElement pe = getSelectedPictogramElement();
	    if (pe != null) {
			ArroPad n = getPad(pe);

	        if (n != null) {
		        // Make sure to first read these variables from domain
		        currentName = n.getName();
		        currentAsInput = n.getInput();
		        currentRunCycle = n.getRun();
		        hasConnection = n.hasConnection();
		        
		        nameTextVal.setText(currentName == null ? "" : currentName);
	        	bIn.setSelection(currentAsInput);
	        	bOut.setSelection(!currentAsInput);
		        bRunCycle.setSelection(currentRunCycle);
	        }
	    }
	}

	
	/**
	 * Provide layout and listeners for the controls.
	 * 
	 * @param parent
	 * @param factory
	 * @param composite
	 * @param nameText
	 */
	private void handleProperty(Composite parent, Composite composite, final Text nameText) {
	    FormData data;
	    
	    // http://www.eclipse.org/forums/index.php/t/202738/
        final FormLayout layout = new FormLayout();
        layout.marginWidth = 4;
        layout.marginHeight = 0;
        layout.spacing = 10;//100;
        parent.setLayout(layout);

	    data = new FormData();
	    data.left = new FormAttachment(0, 0);
	    data.top = new FormAttachment(nameText, 0, SWT.CENTER);
	    valueLabel.setLayoutData(data);

	    data = new FormData();
	    data.left = new FormAttachment(valueLabel, 0);
	    //data.right = new FormAttachment(100, 0);
	    data.right = new FormAttachment(50, 0);
	    data.top = new FormAttachment(0, VSPACE);
	    data.width = SWT.BORDER;
	    nameTextVal.setLayoutData(data);
	    
        data = new FormData();
        data.left = new FormAttachment(0, 0);
        data.top = new FormAttachment(valueLabel, 50, SWT.BOTTOM);
        bIn.setLayoutData(data);
        
        data = new FormData();
        data.left = new FormAttachment(0, 70);
        data.top = new FormAttachment(valueLabel, 50);
	    data.width = SWT.DEFAULT;
        bOut.setLayoutData(data);
	
        data = new FormData();
        data.left = new FormAttachment(0, 0);  // correct now
        data.top = new FormAttachment(bIn, 0);
	    data.width = SWT.DEFAULT;
        bRunCycle.setLayoutData(data);
	
	    nameText.addModifyListener(new ModifyListener() {

	    	@Override
            public void modifyText(ModifyEvent e) {
    			currentName = Misc.checkString(nameText);
    			
    			updateModel();
    			if(!currentName.equals("") && updateSuccess) {
	    			nameText.setBackground(null);
    			} else {
	    			// show error indication
	    			Display display = Display.getCurrent();
	    			Color color = display.getSystemColor(SWT.COLOR_RED);
	    			nameText.setBackground(color);
	    		}
	    	}
	    });
	    
	    bIn.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
			    if(currentAsInput == false && hasConnection) {
			        IStatus status = new Status(IStatus.ERROR, "Arro", /*reason*/"Remove connection first");
	                ErrorDialog.openError(null, "Changing Pad", "Cannot change from output to input", status);
	                bIn.setSelection(false);
	                bOut.setSelection(true);
			    } else {
		             currentAsInput = true;
		             updateModel();
			    }
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
	    });
	    bOut.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
                if(currentAsInput == true && hasConnection) {
                    IStatus status = new Status(IStatus.ERROR, "Arro", /*reason*/"Remove connection first");
                    ErrorDialog.openError(null, "Changing Pad", "Cannot change from input to output", status);
                    bIn.setSelection(true);
                    bOut.setSelection(false);
                } else {
        			currentAsInput = false;
        			updateModel();
                }
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
	    });
	    bRunCycle.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				currentRunCycle = bRunCycle.getSelection();
				updateModel();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
	    });
	}
	
	
	/**
	 * Update the ArroPad that this property dialog is connected to
	 */
	private void updateModel() {
		final String typedValue = currentName;
		final boolean typedValue2 = currentAsInput;
		final boolean typedValue3 = currentRunCycle;
		IFeature feature = new AbstractFeature(getDiagramTypeProvider().getFeatureProvider()) {
				
			@Override
            public boolean canExecute(IContext context) {
				return true;
			}
			@Override
            public void execute(IContext context) {
				final PictogramElement pe = getSelectedPictogramElement();
				ArroPad n = getPad(pe);
				updateSuccess = false;

				if (n != null) {
					ArroPad m = n.getParent().getPadByName(typedValue);
					if(m == null || m == n) {
						updateSuccess = true;
						n.setName(typedValue);
						n.setInput(typedValue2);
						n.setRun(typedValue3);
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
	}
	
	/**
	 * Get model object: ArroPad
	 * 
	 * @param pe Pictogram element that this property dialog is connected to.
	 * @return
	 */
	public ArroPad getPad(PictogramElement pe) {
	    if (pe != null) {
	       	IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();

	        Object[] eObject = fp.getAllBusinessObjectsForPictogramElement(pe);

	        if (eObject.length != 0 && eObject[0] instanceof ArroPad) {
	        	return (ArroPad)(eObject[0]);
	        }
	    }
	    return null;
	}
}
