package arro.diagram.features;


import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.impl.AbstractLayoutFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;

import arro.Constants;
import arro.domain.ArroPad;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;
import util.Logger;

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

	public boolean canLayout(ILayoutContext context) {
		// TODO: check for right domain object instances below

		return context.getPictogramElement() instanceof ContainerShape /* && getBusinessObjectForPictogramElement(context.getPictogramElement()) instanceof <DomainObject> */;

	}

	/**
	 * The layout method. Called if e.g. object is resized in diagram.
	 * Needed specific code here for text placement and anchor placement.
	 */
	public boolean layout(ILayoutContext context) {
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof ContainerShape) {
			ContainerShape containerShape = (ContainerShape) pictogramElement;
			
			GraphicsAlgorithm outerGraphicsAlgorithm = containerShape.getGraphicsAlgorithm();
			if (outerGraphicsAlgorithm instanceof RoundedRectangle) {
				RoundedRectangle roundedRectangle = (RoundedRectangle) outerGraphicsAlgorithm;
				
				layoutNode(containerShape, roundedRectangle, pictogramElement, getFeatureProvider());
				
				return true;
			}
		}
		return false;
	}
	
	private static void layoutNode(ContainerShape containerShape, RoundedRectangle roundedRectangle, PictogramElement pictogramElement, IFeatureProvider fp) {
		
		EList<Shape> children = containerShape.getChildren();
		int i = 0;
		for(Shape shape: children) {
			GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();
			
			// put text at right location; automatic layout would mess (scale) things up.
			if (graphicsAlgorithm instanceof Text && i == 0) {
				Graphiti.getGaLayoutService().setLocationAndSize(graphicsAlgorithm, 0, 0, roundedRectangle.getWidth(), 20);
				i++;
			} else if (graphicsAlgorithm instanceof Text && i == 1) {
				Graphiti.getGaLayoutService().setLocationAndSize(graphicsAlgorithm, 0, 20, roundedRectangle.getWidth(), 20);
				i++;
			}
		}
        //boolean input = (Graphiti.getPeService().getPropertyValue(containerShape, Constants.PROP_PAD_INPUT_KEY).equals(Constants.PROP_TRUE_VALUE));
        NonEmfDomainObject bo = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(pictogramElement, fp);
        boolean input = true;
        if(bo instanceof ArroPad) {
        	input = ((ArroPad) bo).getInput();
        }

    	
		if (pictogramElement instanceof AnchorContainer) {
			AnchorContainer anchorList = (AnchorContainer) pictogramElement;
			EList<Anchor>anchors = anchorList.getAnchors();
			for(Anchor anchor: anchors) {
				Logger.out.trace(Logger.EDITOR, anchor.toString());
    			if(anchor instanceof BoxRelativeAnchor) {
    				BoxRelativeAnchor boxAnchor = (BoxRelativeAnchor)anchor;
    				boxAnchor.setRelativeWidth(input? 1.0: 0.0);
    				
    				GraphicsAlgorithm graphicsAlgorithm = boxAnchor.getGraphicsAlgorithm();
    				IGaService gaService = Graphiti.getGaService();
    	            gaService.setLocationAndSize(graphicsAlgorithm, input? - Constants.PAD_SIZE : 0, 0, Constants.PAD_SIZE, Constants.PAD_SIZE);	            
    			}
			}
		}
	}
}
