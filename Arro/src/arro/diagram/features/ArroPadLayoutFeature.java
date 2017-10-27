package arro.diagram.features;


import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.impl.AbstractLayoutFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

/**
 * The layout feature is for supporting correct resizing. Graphiti provides the concept of layout features,
 * which mainly supports the recalculation of positions and sizes inside the pictogram.
 * 
 */
public class ArroPadLayoutFeature extends AbstractLayoutFeature implements
		ILayoutFeature {

	public ArroPadLayoutFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
    public boolean canLayout(ILayoutContext context) {
		// TODO: check for right domain object instances below

		return context.getPictogramElement() instanceof ContainerShape /* && getBusinessObjectForPictogramElement(context.getPictogramElement()) instanceof <DomainObject> */;

	}

	/**
	 * The layout method. Called if e.g. object is resized in diagram.
	 * Needed specific code here for text placement and anchor placement.
	 */
	@Override
    public boolean layout(ILayoutContext context) {
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof ContainerShape) {
			ContainerShape containerShape = (ContainerShape) pictogramElement;
			
            return new ArroPadHelper(getDiagram()).layout(containerShape, getFeatureProvider());
		}
		return false;
	}
}
