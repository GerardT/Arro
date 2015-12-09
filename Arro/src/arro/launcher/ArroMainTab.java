package arro.launcher;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import util.Logger;
import util.Misc;
import arro.Constants;
import arro.launcher.TCPClient.Result;



public class ArroMainTab  extends AbstractLaunchConfigurationTab {

	private Text ipAddressText;
	private Text projectNameText;
	private Button checkButton;

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
	    configuration.setAttribute(Constants.ATTR_LAUNCH_PROJECT, "");
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
//		ipAddressText.getAccessible().addAccessibleListener(new AccessibleAdapter() {
//			@Override
//			public void getName(AccessibleEvent e) {
//				Logger.out.trace(Logger.STD, e.toString());
//
//				e.result =  "result";
//			}
//		});
		ipAddressText.addModifyListener(fBasicModifyListener);
		
		projectNameText = createSingleText(comp, 1);
		projectNameText.addModifyListener(fBasicModifyListener);
		
//		checkButton = new Button (comp, SWT.PUSH);
//		checkButton.setText ("Check");
//		checkButton.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				Logger.out.trace(Logger.STD, "Check " + ipAddressText.getText());
//				TCPClient temp = new TCPClient(ipAddressText.getText());
//				testConnection(temp);
//			}
//		});		
	}
	
//	private void testConnection(final TCPClient client) {
//		ExecutorService executor = Executors.newCachedThreadPool();
//		Callable<TCPClient.Result> task = new Callable<TCPClient.Result>() {
//		   public TCPClient.Result call() {
//		      return connectAndTest(client);
//		   }
//		};
//		Future<TCPClient.Result> future = executor.submit(task);
//		try {
//			Result r = future.get(5, TimeUnit.SECONDS);
//			if(r.result == true) {
//				Logger.writeToConsole("Server available");
//			}
//			else {
//				Logger.writeToConsole("Server NOT available");
//			}
//		} catch (TimeoutException ex) {
//		   // handle the timeout
//		} catch (InterruptedException e) {
//		   // handle the interrupts
//		} catch (ExecutionException e) {
//		   // handle other exceptions
//		} finally {
//		   future.cancel(true); // may or may not desire this
//		}
//	}
//	private Result connectAndTest(TCPClient client) {
//		Result r = client.connect(13000);
//		if(r.result) {
//			client.writeln("echo");
//			String s = client.readln();
//			if(s.equals("echo")) {
//			    Logger.out.trace(Logger.STD, "Good");
//			} else {
//				Logger.out.trace(Logger.STD, "Bad");
//			}
//			client.close();
//		}
//		return r;
//	}

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

}
