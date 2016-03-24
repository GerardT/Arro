package arro.diagram.features;

import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICustomUndoRedoFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;

import arro.Constants;
import arro.domain.ArroModule;
import arro.domain.ArroTransition;
import arro.editors.StateDiagramEditor;
import util.Logger;


public class TransitionAddFeature extends AbstractAddFeature implements IAddFeature, ICustomUndoRedoFeature {
	
	public TransitionAddFeature(IFeatureProvider fp) {
		super(fp);

	}

	public boolean canAdd(IAddContext context) {
		// TODO: check for right domain object instance below
		return (context.getNewObject() instanceof ArroTransition && context.getTargetContainer() instanceof Diagram);
	}

	/**
	 * Called when a State is added to the diagram.
	 */
	public PictogramElement add(IAddContext context) {
		
		// Can't make it a object attribute since this code is called from different
		// contexts (so different object instances)!
        int width = 200;
        int height = 20;
        
        
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof StateDiagramEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        	return null;
        }
        ArroModule domainModule =  ((StateDiagramEditor)dc).getDomainModule();
        
  		Object obj = context.getNewObject();
		
        if(!(obj instanceof ArroTransition)) {
        	return null;
        }
        
        ArroTransition addedDomainObject = (ArroTransition)obj;
        
        String instanceName = "a" + "State";
        while(domainModule.getStateDiagram().getStateByName(instanceName) != null) {
        	instanceName += "1";
        }
        addedDomainObject.setName(instanceName);

		Diagram targetDiagram = (Diagram) context.getTargetContainer();
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();
		

		/////// CONTAINER ///////
		ContainerShape containerShape = peCreateService.createContainerShape(targetDiagram, true);
		
        Graphiti.getPeService().setPropertyValue(containerShape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_TRANSITION);

        // create invisible outer rectangle expanded by
        // the width needed for the anchor
        Rectangle invisibleRectangle = gaService.createInvisibleRectangle(containerShape);
        {
            gaService.setLocationAndSize(invisibleRectangle, context.getX(), context.getY(),
            												 width, height);
     
        }

		/////// horizontal divider line ///////
		{
	        // create and set graphics algorithm
	        Polyline polyline =
	            gaService.createPolyline(invisibleRectangle, new int[] { 0, height / 2, width, height / 2 });
	        polyline.setForeground(manageColor(Constants.CLASS_FOREGROUND));
	        polyline.setLineWidth(2);
		}

		context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainModule.cloneNodeList());
        context.putProperty(Constants.PROP_DOMAIN_MODULE_KEY, domainModule);

        domainModule.getStateDiagram().addTransition(addedDomainObject);
        
	    // Now link PE (containerShape) to domain object and register diagram in POJOIndependencySolver
		link(containerShape, addedDomainObject);
		
		// After PE was linked to domain object..

        // To set location and size.
	    LayoutContext layoutContext = new LayoutContext(containerShape);
	    ILayoutFeature layoutFeature = getFeatureProvider().getLayoutFeature(layoutContext);
	    layoutFeature.layout(layoutContext);


		
		return containerShape;
	}

	@Override
	public void preUndo(IContext context) {
		ArroModule domainModule = (ArroModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "undo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_REDO_NODE_KEY, domainModule.cloneNodeList());
		Object undoList = context.getProperty(Constants.PROP_UNDO_NODE_KEY);
		domainModule.setNodeList(undoList);
	}

	@Override
	public boolean canRedo(IContext context) {
		return true;
	}

	@Override
	public void preRedo(IContext context) {
		ArroModule domainModule = (ArroModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "redo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainModule.cloneNodeList());
		Object redoList = context.getProperty(Constants.PROP_REDO_NODE_KEY);
		domainModule.setNodeList(redoList);
	}

	@Override
	public void postUndo(IContext context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postRedo(IContext context) {
		// TODO Auto-generated method stub
		
	}
}

