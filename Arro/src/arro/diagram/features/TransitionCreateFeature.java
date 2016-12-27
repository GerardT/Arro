package arro.diagram.features;

import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;

import arro.Constants;
import arro.diagram.features.ArroConnectionCreateFeature.StringParam;
import arro.domain.ArroNode;
import arro.domain.ArroPad;
import arro.domain.ArroStep;
import arro.domain.ArroTransition;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;
import util.Logger;
import util.WidgetUtil;

public class TransitionCreateFeature extends AbstractCreateConnectionFeature
		implements ICreateConnectionFeature {

	public TransitionCreateFeature(IFeatureProvider fp) {
		super(fp, "Create Transition", "Creates a new transition between two steps");
	}

	public boolean canStartConnection(ICreateConnectionContext context) {
		// TODO: check for right domain object instance below
		// return getBusinessObjectForPictogramElement(context.getSourcePictogramElement()) instanceof <DomainObject>;

		return true;
	}

	   /**
     * From an anchor retrieve the name of the object that the anchor belongs to.
     * 
     * @param pictogramElement
     * @param ref
     * @return
     */
    private String fetchStep(PictogramElement pictogramElement) {
        String name = null;
        if(pictogramElement instanceof BoxRelativeAnchor) {
            BoxRelativeAnchor anchor = (BoxRelativeAnchor)pictogramElement;
            
            ContainerShape cs = WidgetUtil.getCsFromAnchor(anchor); 
            
            NonEmfDomainObject domainObject = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs, getFeatureProvider());
            if(domainObject instanceof ArroStep) {
                ArroStep step = (ArroStep)domainObject;
                name = step.getName();
                Logger.out.trace(Logger.EDITOR, "parent " + anchor + " name " + name);
            }
        }
        return name;
    }

	/**
	 * check if connection allowed while hovering over anchors.
	 */
	public boolean canCreate(ICreateConnectionContext context) {
		Anchor source = context.getSourceAnchor();
		Anchor target = context.getTargetAnchor();
		
		if(source == null || target == null) {
			return false;
		}
		
        String sourceType = Graphiti.getPeService().getPropertyValue(source, Constants.PROP_PAD_NAME_KEY);
        String targetType = Graphiti.getPeService().getPropertyValue(target, Constants.PROP_PAD_NAME_KEY);
        
        assert(sourceType != null);
        assert(targetType != null);
        
		// The following combinations are allowed:
        if((
        		(sourceType.equals(Constants.PROP_PAD_NAME_STEP_OUT) && targetType.equals(Constants.PROP_PAD_NAME_STEP_IN))
        		
        		)) {
        	return true;
        }
        
        
        
		
		// PROP_PAD_NAME_STEP_IN can have 0...1 connections.
		// PROP_PAD_NAME_STEP_OUT can have 0...n connections.
		// PROP_PAD_NAME_SYNC_START_IN can have 0...1 connections.
		// PROP_PAD_NAME_SYNC_START_OUT can have 0...n connections.
		// PROP_PAD_NAME_SYNC_STOP_IN can have 0...n connections.
		// PROP_PAD_NAME_SYNC_STOP_OUT can have 0...1 connections.
		
		return false;
	}

	public Connection create(ICreateConnectionContext context) {
        ArroTransition newClass = new ArroTransition();
        //ArroTransition newClass = null;
		
		AddConnectionContext addContext = new AddConnectionContext(context.getSourceAnchor(), context.getTargetAnchor());
        addContext.putProperty(Constants.PROP_SOURCE_PAD_KEY, fetchStep(context.getSourcePictogramElement()));
        addContext.putProperty(Constants.PROP_TARGET_PAD_KEY, fetchStep(context.getTargetPictogramElement()));
		addContext.setNewObject(newClass);
		Connection newConnection = (Connection) getFeatureProvider().addIfPossible(addContext);

		return newConnection;
	}
}
