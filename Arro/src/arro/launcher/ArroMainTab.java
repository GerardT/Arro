package arro.launcher;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import arro.Constants;
import util.Logger;



public class ArroMainTab  extends AbstractLaunchConfigurationTab {

	private Text ipAddressText;
	private Text projectNameText;

	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		comp.setLayout(new GridLayout(1, true));
		comp.setFont(parent.getFont());
		createServerConnectionComponent(comp);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setContainer(null);
	    configuration.setAttribute(Constants.ATTR_LAUNCH_IP_ADDRESS, "localhost");
	    configuration.setAttribute(Constants.ATTR_LAUNCH_PROJECT, "<your project name>");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			ipAddressText.setText(configuration.getAttribute(Constants.ATTR_LAUNCH_IP_ADDRESS, "localhost"));
			// FIXME projectNameText must be equal to project name, otherwise will fail opening arro.proto.
			projectNameText.setText(configuration.getAttribute(Constants.ATTR_LAUNCH_PROJECT, ""));
		} catch (CoreException e) {
			Logger.out.trace(Logger.ERROR, e.toString());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
	    configuration.setAttribute(Constants.ATTR_LAUNCH_IP_ADDRESS, ipAddressText.getText());		
	    configuration.setAttribute(Constants.ATTR_LAUNCH_PROJECT, projectNameText.getText());		
	}

	@Override
	public String getName() {
		return "&Runtime"; 
	}

	/*
	 * This triggers calling performApply upon changing a text box.
	 */
	private ModifyListener fBasicModifyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent evt) {
			scheduleUpdateJob();
		}
	};
	
	private void createServerConnectionComponent(Composite parent) {
		Group group = createGroup(parent, "Remote system", 3, 2, GridData.FILL_HORIZONTAL);
		Composite comp = createComposite(group, parent.getFont(), 3, 3, GridData.FILL_BOTH, 0, 0);
		
		
		ipAddressText = createSingleText(comp, 1);
		ipAddressText.addModifyListener(fBasicModifyListener);
		
		projectNameText = createSingleText(comp, 1);
		projectNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent evt) {
				
				if(!validateProjectName()) {
					setErrorMessage("Invalid Project Name");
				}
				
				scheduleUpdateJob();
			}

		});
	}
	

	private Group createGroup(Composite parent, String text, int columns, int hspan, int fill) {
    	Group g = new Group(parent, SWT.NONE);
    	g.setLayout(new GridLayout(columns, false));
    	g.setText(text);
    	g.setFont(parent.getFont());
    	GridData gd = new GridData(fill);
		gd.horizontalSpan = hspan;
    	g.setLayoutData(gd);
    	return g;
    }
	private Composite createComposite(Composite parent, Font font, int columns, int hspan, int fill, int marginwidth, int marginheight) {
		Composite g = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(columns, false);
		layout.marginWidth = marginwidth;
		layout.marginHeight = marginheight;
    	g.setLayout(layout);
    	g.setFont(font);
    	GridData gd = new GridData(fill);
		gd.horizontalSpan = hspan;
    	g.setLayoutData(gd);
    	return g;
	}
	private Text createSingleText(Composite parent, int hspan) {
    	Text t = new Text(parent, SWT.SINGLE | SWT.BORDER);
    	t.setFont(parent.getFont());
    	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    	gd.horizontalSpan = hspan;
    	t.setLayoutData(gd);
    	return t;
    }
	
	private boolean validateProjectName() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectNameText.getText());
		return project.exists();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(ILaunchConfiguration config) {
		setMessage(null);
		setErrorMessage(null);
		
		return validateProjectName();
	}
}
