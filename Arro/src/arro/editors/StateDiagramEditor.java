package arro.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import util.ArroZipFile;
import util.PathUtil;
import arro.domain.DomainModule;
import arro.domain.ResourceCache;

/**
 * Subclass of DiagramEditor (Graphiti diagram editor), allows override some
 * standard behavior, basically to connect it to the domain object.
 * 
 */
public class StateDiagramEditor extends DiagramEditor {

	//public final static String ID = "org.activiti.designer.diagrameditor"; //$NON-NLS-1$
	
	private String diagramName;
	private DomainModule domainModule;
	private ArroZipFile zip;
	

	public StateDiagramEditor(ArroZipFile zip) {
		super();
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
		
		DomainModule domainObject = (DomainModule)zip.getDomainDiagram();
		if(domainObject != null && domainObject instanceof DomainModule) {
			domainModule = (DomainModule)domainObject;
		} else {
	    	// Then it must be that a new diagram is created...
	    	domainModule = new DomainModule();
	    	domainModule.setType(diagramName);
		}
	}
	
	public DomainModule getDomainModule() {
		return domainModule;
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
	protected void setInput(final IEditorInput input) {
		super.setInput(input);
	}

}
