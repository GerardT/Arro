package arro.editors;

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

import arro.Constants;
import arro.domain.ArroModule;
import util.ArroZipFile;
import util.PathUtil;

/**
 * Subclass of DiagramEditor (Graphiti diagram editor), allows override some
 * standard behavior, basically to connect it to the domain object.
 * 
 */
public class FunctionDiagramEditor extends DiagramEditor {

	//public final static String ID = "org.activiti.designer.diagrameditor"; //$NON-NLS-1$
	private int documentType = Constants.FunctionBlock;
	
	private String diagramName;
	private ArroModule domainModule;
	private ArroZipFile zip;
	

	public FunctionDiagramEditor(ArroZipFile zip, int documentType) {
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
		
		ArroModule domainObject = (ArroModule)zip.getDomainDiagram();
		if(domainObject != null && domainObject instanceof ArroModule) {
			domainModule = (ArroModule)domainObject;
		} else {
	    	// Then it must be that a new diagram is created...
	    	domainModule = new ArroModule();
	    	domainModule.setType(diagramName);
		}
	}
	
	public ArroModule getDomainModule() {
		return domainModule;
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
	protected void setInput(final IEditorInput input) {
		super.setInput(input);
	}

}
