package arro.diagram.features;

import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import arro.Constants;
import arro.domain.ArroStep;
import arro.domain.ArroTransition;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;
import util.Logger;

public class TransitionCreateFeature extends AbstractCreateConnectionFeature
		implements ICreateConnectionFeature {

	public TransitionCreateFeature(IFeatureProvider fp) {
		super(fp, "Create Transition", "Creates a new transition between two steps");
	}

	@Override
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
        
        NonEmfDomainObject domainObject = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(pictogramElement, getFeatureProvider());
        if(domainObject != null && domainObject instanceof ArroStep) {
            name = ((ArroStep) domainObject).getName();
            Logger.out.trace(Logger.EDITOR, " name " + name);
        }
        return name;
    }

	/**
	 * check if connection allowed while hovering over anchors.
	 * We might also check that the source anchor is of the right type.
	 */
	@Override
    public boolean canCreate(ICreateConnectionContext context) {
		Anchor source = context.getSourceAnchor();
		Anchor target = context.getTargetAnchor();		
		
		if(source != null && target != null && source != target) {
			return true;
		} else {
		    return false;
		}
	}

	@Override
    public Connection create(ICreateConnectionContext context) {
	    // Create a model element and add it to the resource of the diagram.
	    // TODO see also ArroConnectionCreateFeature -> no model object created, which is better?
        ArroTransition newClass = new ArroTransition();
		
		AddConnectionContext addContext = new AddConnectionContext(context.getSourceAnchor(), context.getTargetAnchor());
        addContext.putProperty(Constants.PROP_SOURCE_PAD_KEY, fetchStep(context.getSourcePictogramElement()));
        addContext.putProperty(Constants.PROP_TARGET_PAD_KEY, fetchStep(context.getTargetPictogramElement()));
		addContext.setNewObject(newClass);
		Connection newConnection = (Connection) getFeatureProvider().addIfPossible(addContext);

		return newConnection;
	}
}
