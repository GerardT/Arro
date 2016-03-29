package arro.diagram.features;


import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.impl.AbstractLayoutFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import arro.Constants;


public class SynchronizationLayoutFeature extends AbstractLayoutFeature implements
		ILayoutFeature {

	public SynchronizationLayoutFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canLayout(ILayoutContext context) {
		return context.getPictogramElement() instanceof ContainerShape /* && getBusinessObjectForPictogramElement(context.getPictogramElement()) instanceof <DomainObject> */;
	}

	/**
	 * The layout method. Called if e.g. object is resized in diagram.
	 * Needed specific code here for text placement and anchor placement.
	 */
	public boolean layout(ILayoutContext context) {
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof ContainerShape) {
			ContainerShape cs = (ContainerShape) pictogramElement;
			
			new SynchronizationHelper().read(context, cs, manageColor(Constants.CLASS_FOREGROUND), manageColor(Constants.CLASS_BACKGROUND));
			return true;
		}
		return false;
	}
}
