package arro.editors;

import arro.Constants;
import util.ArroZipFile;

public class StateDiagramEditor extends FunctionDiagramEditor {
    public StateDiagramEditor(ArroZipFile zip) {
        super(zip, Constants.FunctionBlock);
    }
}


///**
// * Subclass of DiagramEditor (Graphiti diagram editor), allows override some
// * standard behavior, basically to connect it to the domain object.
// * 
// */
//public class StateDiagramEditor extends DiagramEditor {
//
//	//public final static String ID = "org.activiti.designer.diagrameditor"; //$NON-NLS-1$
//	
//	private String diagramName;
//	private ArroModule domainModule;
//	private ArroZipFile zip;
//	
//
//	public StateDiagramEditor(ArroZipFile zip) {
//		super();
//		this.zip = zip;
//	}
//	
//	@Override
//	public void setFocus() {
//		super.setFocus();
//		if(getDiagramBehavior() != null) {
//			getDiagramBehavior().refreshContent();
//		}
//	}
//
//	@Override
//	public void init(IEditorSite site, IEditorInput input)
//			throws PartInitException {
//		super.init(site, input);
//		
//		diagramName = PathUtil.truncExtension(zip.getName());
//		
//		ArroModule domainObject = (ArroModule)zip.getDomainDiagram();
//		if(domainObject != null && domainObject instanceof ArroModule) {
//			domainModule = (ArroModule)domainObject;
//		} else {
//	    	// Then it must be that a new diagram is created...
//	    	domainModule = new ArroModule();
//	    	domainModule.setType(diagramName);
//		}
//	}
//	
//	public ArroModule getDomainModule() {
//		return domainModule;
//	}
//	
//	@Override
//	public void createPartControl(Composite parent) {
//		super.createPartControl(parent);
//		GraphicalViewer graphicalViewer = (GraphicalViewer) getAdapter(GraphicalViewer.class);
//		if (graphicalViewer != null
//				&& graphicalViewer.getEditPartRegistry() != null) {
//			ScalableFreeformRootEditPart rootEditPart = (ScalableFreeformRootEditPart) graphicalViewer
//					.getEditPartRegistry().get(LayerManager.ID);
//			IFigure gridFigure = ((LayerManager) rootEditPart)
//					.getLayer(LayerConstants.GRID_LAYER);
//			gridFigure.setVisible(true);
//		}
//	}
//
//	@Override
//	protected void setInput(final IEditorInput input) {
//		super.setInput(input);
//	}
//
//}
