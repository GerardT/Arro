package arro.wizards.codeblock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.services.GraphitiUi;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import arro.Constants;
import arro.domain.ArroDevice;
import arro.domain.DomainModule;
import arro.wizards.FileService;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * Constants.NODE_EXT. If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class NewCodeBlockWizard extends Wizard implements INewWizard {
	private NewCodeBlockWizardPage page;
	private ISelection selection;
	private DomainModule nodeDiagram;
	private ArroDevice device;
	


	/**
	 * Constructor for SampleNewWizard.
	 */
	public NewCodeBlockWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new NewCodeBlockWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		final String language = page.getLanguage();

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, language, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */

	private void doFinish(String containerName,	String nodeName, String language, IProgressMonitor monitor) throws CoreException {
		// create a sample file
		monitor.beginTask("Creating " + nodeName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		String fileName = nodeName + "." + arro.Constants.NODE_EXT;
		
		IFolder f = null;
		if(resource instanceof IProject) {
			f = ((IProject)resource).getFolder(Constants.FOLDER_DEVICES);
			try {
				f.create(false, true, null);
			} catch (CoreException e) {
				// Ignore if already exists..
			}
		} else {
			throwCoreException("Container \"" + containerName + "\" is not a project.");
		}

		final IFile file = f.getFile(new Path(fileName));
		try {
			// Create a zip file...
			
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
					
			// works like a filter, writing to ZipOutputStream writes to Zip file (after selecting an entry).
			ZipOutputStream out = new ZipOutputStream(bao);
			{

		        // name the META file inside the zip file 
		        out.putNextEntry(new ZipEntry("META"));
		        
		        // fill with initial data
		        // Not very nice: we borrow the file for temporarily writing the diagram data into.
		        // The file is actually used for storing the ZIP file
				InputStream stream = openMETAStream(file, nodeName, language);
		        byte[] b = new byte[1024];
		        int count;
	
		        while ((count = stream.read(b)) > 0) {
		            out.write(b, 0, count);
		        }	        
				stream.close();
			}
			{

		        // name the file inside the zip file 
		        out.putNextEntry(new ZipEntry(Constants.HIDDEN_RESOURCE + fileName));
		        
		        // fill with initial data
		        // Not very nice: we borrow the file for temporarily writing the diagram data into.
		        // The file is actually used for storing the ZIP file
				InputStream stream = openContentStream(file, nodeName);
		        byte[] b = new byte[1024];
		        int count;
	
		        while ((count = stream.read(b)) > 0) {
		            out.write(b, 0, count);
		        }	        
				stream.close();
			}
			{
				// name the xml file inside the zip file 
		        out.putNextEntry(new ZipEntry(Constants.HIDDEN_RESOURCE + fileName + ".xml"));
		        
		        // fill with initial data
		        // Not very nice: we borrow the file for temporarily writing the diagram data into.
		        // The file is actually used for storing the ZIP file
				InputStream stream = openXmlStream(file, nodeName, language);
		        byte[] b = new byte[1024];
		        int count;

		        while ((count = stream.read(b)) > 0) {
		            out.write(b, 0, count);
		        }
	        	// close here in order to flush
				stream.close();
			}
			{
				// name the python file inside the zip file 
		        out.putNextEntry(new ZipEntry(Constants.HIDDEN_RESOURCE + fileName + ".py"));
		        
		        // fill with initial data
		        // Not very nice: we borrow the file for temporarily writing the diagram data into.
		        // The file is actually used for storing the ZIP file
				InputStream stream = openPythonStream(file, nodeName);
		        byte[] b = new byte[1024];
		        int count;

		        while ((count = stream.read(b)) > 0) {
		            out.write(b, 0, count);
		        }
	        	// close here in order to flush
				stream.close();
			}
        	out.close();
			
			// ZIP file finished. Continue working with the file
			if (file.exists()) {
				file.setContents(new ByteArrayInputStream(bao.toByteArray()), true, true, monitor);
			} else {
				file.create(new ByteArrayInputStream(bao.toByteArray()), true, monitor);
			}
			
			bao.close();

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}
	
	/**
	 * We will initialize file contents with a sample text.
	 * @param file 
	 * @throws CoreException 
	 */

	private InputStream openMETAStream(IFile file, String diagramName, String language) throws CoreException {
		String contents = 	"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
							"<metadata>\n" +
							"	<entry key=\"name\" value=\"" + diagramName + "\"/>\n" +
							"	<entry key=\"type\" value=\"" + Constants.CODE_BLOCK + "\"/>\n" +
							"	<entry key=\"language\" value=\"" + language + "\"/>\n" +
							"	<entry key=\"version\" value=\"0.90\"/>\n" +
							"</metadata>\n";
		return new ByteArrayInputStream(contents.getBytes());
	}

	private InputStream openContentStream(IFile file, String diagramName) throws CoreException {
		final String diagramTypeId = arro.Constants.FUNCTION_DIAGRAM_TYPE;
		
		Diagram diagram = Graphiti.getPeCreateService().createDiagram(diagramTypeId, diagramName, true);
		
		IFile diagramFile = file;
		
		URI uri = URI.createPlatformResourceURI(diagramFile.getFullPath().toString(), true);
		
		// Create 'ID' in device diagram
		IDiagramTypeProvider dtp=GraphitiUi.getExtensionManager().createDiagramTypeProvider(diagram, "Arro.FunctionDiagramTypeProvider");
		AddContext context = new AddContext();
		
		nodeDiagram = new DomainModule();

		context.setNewObject(nodeDiagram);
		context.setTargetContainer(diagram);
		
		IAddFeature f = dtp.getFeatureProvider().getAddFeature(context);
		f.add(context);

		// Create 'BOX' in device diagram
		device = new ArroDevice();
		context.setNewObject(device);

		f = dtp.getFeatureProvider().getAddFeature(context);
		f.add(context);

		FileService.createEmfFileForDiagram(uri, diagram);
		
		return diagramFile.getContents();
	}

	private InputStream openXmlStream(IFile file, String diagramName, String language) throws CoreException {
		String contents = 	"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
							"<diagram>\n" +
							"    <nodedefinition id=\"" + nodeDiagram.getId() + "\" type=\"" + diagramName + "\">\n" +
							"        <device id=\"" + device.getId() + "\" url=\"" + language + ":" + diagramName + "\"/>\n" +
							"    </nodedefinition>\n" +
							"</diagram>\n";
		return new ByteArrayInputStream(contents.getBytes());
	}

	private InputStream openPythonStream(IFile file, String diagramName) throws CoreException {
		String contents = 	"class " + diagramName + ":\n" +
					        "   b1 = 20\n" +
					        "\n" +
					        "   def __init__(self, usefonts=0):\n" +
					        "       self.b2 = 30\n" +
					        "\n" +
					        "   def runCycle(self):\n" +
					        "       while True:\n" +
					        "           status = getMessage(self)\n" +
					        "           print \"one loop\"\n" +
					        "               break\n" +
					        "               \n" +
					        "           print status\n" +
					        "           if status[0] == \"aTick\":\n" +
					        "               message = arro_pb2.Tick()\n" +
					        "               message.ParseFromString(status[1])\n" +
					        "               print message.ms\n" +
					        "               \n" +
					        "               output = arro_pb2.Value()\n" +
					        "               output.value = 17\n" +
					        "               ser = output.SerializeToString()\n" +
					        "               sendMessage(self, \"output\", ser)\n";

		return new ByteArrayInputStream(contents.getBytes());
	}


	private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "Arro", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}