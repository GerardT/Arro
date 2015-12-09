package arro.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import util.ArroZipFile;
import util.PathUtil;
import arro.Constants;
import arro.domain.DomainNodeDiagram;
import arro.domain.POJOIndependenceSolver;
import arro.domain.ResourceCache;

/**
 * Subclass of DiagramEditor (Graphiti diagram editor), allows override some
 * standard behavior, basically to connect it to the domain object.
 * 
 */
public class SubGraphitiEditor extends DiagramEditor {

	//public final static String ID = "org.activiti.designer.diagrameditor"; //$NON-NLS-1$
	private int documentType = Constants.FunctionFile;
	
	private String pojoFilePath;
	private String diagramName;
	private DomainNodeDiagram domainNodeDiagram;
	private ArroZipFile zip;
	

	public SubGraphitiEditor(ArroZipFile zip, int documentType) {
		super();
		this.documentType = documentType;
		this.zip = zip;
	}
	
	@Override
	public void setFocus() {
		super.setFocus();
		if(getDiagramBehavior() != null) {
			getDiagramBehavior().refreshContent();
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		
		diagramName = PathUtil.truncExtension(zip.getName());
		
		//DomainNodeDiagram domainObject = POJOIndependenceSolver.getInstance().loadNodeDiagram(diagramName, pojoFilePath);
		DomainNodeDiagram domainObject = (DomainNodeDiagram)zip.getDomainDiagram();
		if(domainObject != null && domainObject instanceof DomainNodeDiagram) {
			domainNodeDiagram = (DomainNodeDiagram)domainObject;
		} else {
	    	// Then it must be that a new diagram is created...
	    	domainNodeDiagram = new DomainNodeDiagram();
	    	domainNodeDiagram.setType(diagramName);
		}
	}
	
	public DomainNodeDiagram getDomainNodeDiagram() {
		return domainNodeDiagram;
	}
	
	public int getDocumentType() {
		return documentType;
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		GraphicalViewer graphicalViewer = (GraphicalViewer) getAdapter(GraphicalViewer.class);
		if (graphicalViewer != null
				&& graphicalViewer.getEditPartRegistry() != null) {
			ScalableFreeformRootEditPart rootEditPart = (ScalableFreeformRootEditPart) graphicalViewer
					.getEditPartRegistry().get(LayerManager.ID);
			IFigure gridFigure = ((LayerManager) rootEditPart)
					.getLayer(LayerConstants.GRID_LAYER);
			gridFigure.setVisible(true);
		}
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
		// Regular save
		try {
			super.doSave(monitor);
			
			ResourceCache.getInstance().storeDomainDiagram(zip);

		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void setInput(final IEditorInput input) {
		super.setInput(input);
	}

}
