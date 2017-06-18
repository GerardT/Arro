package arro.diagram.features;

import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;

import arro.Constants;
import arro.domain.ArroNode;
import arro.domain.ArroPad;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;
import util.Logger;
import util.WidgetUtil;

public class ArroConnectionCreateFeature extends AbstractCreateConnectionFeature
		implements ICreateConnectionFeature {

	public ArroConnectionCreateFeature(IFeatureProvider fp) {
		super(fp, "Create Connection", "Creates a new connection between two pads");
	}

	public boolean canStartConnection(ICreateConnectionContext context) {
		// TODO: check for right domain object instance below
		// return getBusinessObjectForPictogramElement(context.getSourcePictogramElement()) instanceof <DomainObject>;

		return true;
	}
	class StringParam {
		String s;
		StringParam() {
			s = "";
		}
	}

	/**
	 * From an anchor retrieve the name of the object that the anchor belongs to.
	 * 
	 * @param pictogramElement
	 * @param ref
	 * @return
	 */
	private ArroPad fetchPad(PictogramElement pictogramElement, StringParam ref) {
		ArroPad pad = null;
		if(pictogramElement instanceof BoxRelativeAnchor) {
			BoxRelativeAnchor anchor = (BoxRelativeAnchor)pictogramElement;
			
			// which anchor is being connected?
			// Need to store pad name in anchor properties. With pad name we can find
			// the referenced pad.
			// Having PE point to original pad (outside of zip file) seems not good since easily
			// broken.
			
			ContainerShape cs = WidgetUtil.getCsFromAnchor(anchor);	
			
	        NonEmfDomainObject domainObject = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs, getFeatureProvider());
	        if(domainObject instanceof ArroNode) {
		        String name = Graphiti.getPeService().getPropertyValue(anchor.getParent(), Constants.PROP_PAD_NAME_KEY);
		        Logger.out.trace(Logger.EDITOR, "parent " + anchor + " name " + name);
		        pad = ((ArroNode)domainObject).getPadByName(name);
		        if(pad != null) {
		        	ref.s = domainObject.getName() + "." + pad.getName();
		        	Logger.out.trace(Logger.EDITOR, "found pad on Node" + pad.getName() + " type " + pad.getType());
		        }
	        } else if(domainObject instanceof ArroPad) {
		        pad = (ArroPad)domainObject;
		        if(pad != null) {
		        	ref.s = pad.getName();
		        	Logger.out.trace(Logger.EDITOR, "found Pad " + pad.getName() + " type " + pad.getType());
		        }
	        }
		}
	    return pad;
	}
	
	private String getFullPadReference(PictogramElement pictogramElement) {
		StringParam ref = new StringParam();
		fetchPad(pictogramElement, ref);
		
	    return ref.s;
	}


	/**
	 * check if connection allowed while hovering over anchors.
	 */
	public boolean canCreate(ICreateConnectionContext context) {
		StringParam ref = new StringParam();
		ArroPad source = fetchPad(context.getSourcePictogramElement(), ref);
		ArroPad target = fetchPad(context.getTargetPictogramElement(), ref);
		Logger.out.trace(Logger.EDITOR, "source pad " + source + " target pad " + target);
		
		if(source != null && target != null && source != target && source.getType().equals(target.getType())) {
			return true;
		}
		return false;
	}

	public Connection create(ICreateConnectionContext context) {
		Connection newConnection = null;

        // TODO see also ArroTransitionCreateFeature -> model object created, which is better?
		Object newDomainObjectConnection = null;

        AddConnectionContext addContext = new AddConnectionContext(context.getSourceAnchor(), context.getTargetAnchor());
		addContext.putProperty(Constants.PROP_SOURCE_PAD_KEY, getFullPadReference(context.getSourcePictogramElement()));
		addContext.putProperty(Constants.PROP_TARGET_PAD_KEY, getFullPadReference(context.getTargetPictogramElement()));
		addContext.setNewObject(newDomainObjectConnection);
		newConnection = (Connection) getFeatureProvider().addIfPossible(addContext);

		return newConnection;
	}
}
