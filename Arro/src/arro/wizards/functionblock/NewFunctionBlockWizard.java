package arro.wizards.functionblock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import org.eclipse.core.commands.ExecutionException;
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
import arro.domain.ArroModule;
import arro.domain.ArroSequenceChart;
import arro.domain.ArroStep;
import arro.wizards.FileService;
import arro.workspace.FileEntry;
import arro.workspace.OutputStreamWrapper;
import arro.workspace.ResourceCache;

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

public class NewFunctionBlockWizard extends Wizard implements INewWizard {
	private NewFunctionBlockWizardPage page;
	private ISelection selection;
	private ArroModule nodeDiagram;
    private ArroSequenceChart stateNode;
    private ArroStep readyStep;
    private ArroStep termStep;



	/**
	 * Constructor for SampleNewWizard.
	 */
	public NewFunctionBlockWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	@Override
    public void addPages() {
		page = new NewFunctionBlockWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	@Override
    public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();

		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, monitor);
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

	private void doFinish(String containerName,	String nodeName, IProgressMonitor monitor) throws CoreException {
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
			f = ((IProject)resource).getFolder(Constants.FOLDER_DIAGRAMS);
			try {
				f.create(false, true, null);
			} catch (CoreException e) {
				// Ignore if already exists..
			}
		} else {
			throwCoreException("Container \"" + containerName + "\" is not a project.");
		}
		
		nodeDiagram = new ArroModule();
        stateNode = new ArroSequenceChart();
        nodeDiagram.setStateDiagram(stateNode);
        readyStep = new ArroStep();
        termStep = new ArroStep();
        readyStep.setName(Constants.PROP_CONTEXT_READY_STEP);
        termStep.setName(Constants.PROP_CONTEXT_TERM_STEP);
        
        try {
            stateNode.addState(readyStep);
            stateNode.addState(termStep);
        } catch (ExecutionException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Since the zip file is being created already, it will trigger resource updates
        // that are incomplete and cause issues such as missing META.
        ResourceCache.getInstance().lock();
		final IFile file = f.getFile(new Path(fileName));
		try {
			// Create a zip file...
			
			ByteArrayOutputStream bao = new ByteArrayOutputStream();

			// works like a filter, writing to OutputStreamWrapper writes to Zip file (after selecting an entry).
			OutputStreamWrapper out = new OutputStreamWrapper(bao);
			{

		        // name the META file inside the zip file 
		        out.putNextEntry(new FileEntry("META"));
		        
		        // fill with initial data
		        // Not very nice: we borrow the file for temporarily writing the diagram data into.
		        // The file is actually used for storing the ZIP file
				InputStream stream = openMETAStream(file, nodeName);
		        byte[] b = new byte[1024];
		        int count;
	
		        while ((count = stream.read(b)) > 0) {
		            out.write(b, 0, count);
		        }	        
				stream.close();
			}
			{
		        
				// name the diagram file inside the zip file 
		        out.putNextEntry(new FileEntry(Constants.FUNCTION_FILE_NAME));
		        
		        // fill with initial data
		        // Not very nice: we borrow the file for temporarily writing the diagram data into.
		        // The file is actually used for storing the ZIP file
				InputStream stream = openFunctionDiagramStream(file, nodeName);
		        byte[] b = new byte[1024];
		        int count;

		        while ((count = stream.read(b)) > 0) {
		            out.write(b, 0, count);
		        }
		        
				stream.close();
			}
			{
		        
				// name the diagram file inside the zip file 
		        out.putNextEntry(new FileEntry(Constants.SFC_FILE_NAME));
		        
		        // fill with initial data
		        // Not very nice: we borrow the file for temporarily writing the diagram data into.
		        // The file is actually used for storing the ZIP file
				InputStream stream = openStateDiagramStream(file, nodeName);
		        byte[] b = new byte[1024];
		        int count;

		        while ((count = stream.read(b)) > 0) {
		            out.write(b, 0, count);
		        }
		        
				stream.close();
			}
			{
				// name the xml file inside the zip file 
		        out.putNextEntry(new FileEntry(Constants.MODULE_FILE_NAME));
		        
		        // fill with initial data
		        // Not very nice: we borrow the file for temporarily writing the diagram data into.
		        // The file is actually used for storing the ZIP file
				InputStream stream = openXmlStream(file, nodeName);
		        byte[] b = new byte[1024];
		        int count;

		        while ((count = stream.read(b)) > 0) {
		            out.write(b, 0, count);
		        }
		        
		        // close here in order to flush
		        out.close();
				stream.close();
			}

			// ZIP file finished. Continue working with the file
			if (file.exists()) {
				file.setContents(new ByteArrayInputStream(bao.toByteArray()), true, true, monitor);
			} else {
				file.create(new ByteArrayInputStream(bao.toByteArray()), true, monitor);
			}
			
			bao.close();
	        ResourceCache.getInstance().unlock();

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			@Override
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

	private InputStream openMETAStream(IFile file, String diagramName) throws CoreException {
		String contents = 	"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
							"<metadata>\n" +
							"	<entry key=\"name\" value=\"" + diagramName + "\"/>\n" +
							"	<entry key=\"type\" value=\"" + Constants.FUNCTION_BLOCK + "\"/>\n" +
							"	<entry key=\"version\" value=\"0.90\"/>\n" +
                            "   <entry key=\"UUID\" value=\"" + UUID.randomUUID().toString() + "\"/>\n" +
							"</metadata>\n";
		return new ByteArrayInputStream(contents.getBytes());
	}

	private InputStream openFunctionDiagramStream(IFile diagramFile, String diagramName) throws CoreException {
		final String diagramTypeId = arro.Constants.FUNCTION_NODE_DIAGRAM_TYPE;
		
		// Create empty diagram.
		Diagram diagram = Graphiti.getPeCreateService().createDiagram(diagramTypeId, diagramName, true);
		
		URI uri = URI.createPlatformResourceURI(diagramFile.getFullPath().toString(), true);
		
		// Create 'ID' in diagram, calling ArroIDAddFeature
		AddContext context = new AddContext();	
		context.setNewObject(nodeDiagram);
		context.setTargetContainer(diagram);
		
		IDiagramTypeProvider dtp=GraphitiUi.getExtensionManager().createDiagramTypeProvider(diagram, Constants.FUNCTION_NODE_DIAGRAM_TYPE_PROVIDER);
		IAddFeature f = dtp.getFeatureProvider().getAddFeature(context);
		f.add(context);

		FileService.createEmfFileForDiagram(uri, diagram);
		
		return diagramFile.getContents();
	}
	
	private InputStream openStateDiagramStream(IFile diagramFile, String diagramName) throws CoreException {
		final String diagramTypeId = arro.Constants.STATE_NODE_DIAGRAM_TYPE;
		
		// Create empty diagram.
		Diagram diagram = Graphiti.getPeCreateService().createDiagram(diagramTypeId, diagramName, true);
		
		URI uri = URI.createPlatformResourceURI(diagramFile.getFullPath().toString(), true);
		
        // Create 'ID' in diagram, calling ArroIDAddFeature.
        AddContext context = new AddContext();  
        context.setNewObject(stateNode);
        context.setTargetContainer(diagram);
        
        IDiagramTypeProvider dtp=GraphitiUi.getExtensionManager().createDiagramTypeProvider(diagram, Constants.STATE_NODE_DIAGRAM_TYPE_PROVIDER);
        IAddFeature f = dtp.getFeatureProvider().getAddFeature(context);
        f.add(context);

        // Create '_ready' step in diagram, calling .
        context = new AddContext();  
        context.setNewObject(readyStep);
        context.setTargetContainer(diagram);
        context.putProperty(Constants.PROP_CONTEXT_NAME_KEY, Constants.PROP_CONTEXT_READY_STEP);
        
        f = dtp.getFeatureProvider().getAddFeature(context);
        f.add(context);

        // Create '_terminated' step in diagram, calling .
        context = new AddContext();  
        context.setNewObject(termStep);
        context.setTargetContainer(diagram);
        context.putProperty(Constants.PROP_CONTEXT_NAME_KEY, Constants.PROP_CONTEXT_TERM_STEP);
        
        f = dtp.getFeatureProvider().getAddFeature(context);
        f.add(context);

        // Serialize the diagram into XML.
		FileService.createEmfFileForDiagram(uri, diagram);
		
		return diagramFile.getContents();
	}
	
	private InputStream openXmlStream(IFile file, String diagramName) throws CoreException {
		String contents = 	"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
							"<module id=\"" + nodeDiagram.getId() + "\" type=\"" + diagramName + "\">\n" +
						    "    <sfc id=\"" + stateNode.getId() + "\" name=\"_sfc\" type=\"_Sfc\">\n" +
                            "        <step id=\"" + readyStep.getId() + "\" name=\"" + Constants.PROP_CONTEXT_READY_STEP + "\"/>" +
                            "        <step id=\"" + termStep.getId() + "\" name=\"" + Constants.PROP_CONTEXT_TERM_STEP + "\"/>" +
                            "    </sfc>\n" +
							"</module>\n";
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
	@Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}