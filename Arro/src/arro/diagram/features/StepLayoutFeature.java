package arro.diagram.features;


import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.impl.AbstractLayoutFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;

import arro.Constants;
import util.WidgetUtil;


public class StepLayoutFeature extends AbstractLayoutFeature implements
		ILayoutFeature {

	public StepLayoutFeature(IFeatureProvider fp) {
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
			ContainerShape cs = (ContainerShape) pictogramElement;
			
			new StepHelper().read(context, cs, manageColor(Constants.CLASS_FOREGROUND), manageColor(Constants.CLASS_BACKGROUND));
			return true;
			
//			Rectangle invisibleRectangle = WidgetUtil.getInvisibleRectangle(cs);
//			
//			RoundedRectangle rect = WidgetUtil.getRoundedRectangle(cs);
//			Graphiti.getGaLayoutService().setLocationAndSize(rect, Constants.HALF_PAD_SIZE, 0,
//					invisibleRectangle.getWidth() - Constants.PAD_SIZE,
//					invisibleRectangle.getHeight());
//			
//		    for (Shape shape : cs.getChildren()) {
//		    	if(!(shape instanceof ContainerShape)) {
//			    	GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();
//
//			        if (graphicsAlgorithm instanceof Text) {
//			            Text text = (Text) shape.getGraphicsAlgorithm();
//			            
//						Graphiti.getGaLayoutService().setLocationAndSize(text, 0, 20, rect.getWidth(), 20);
//			    	}
//		    	}
//		    }
		}
		return false;
	}
}
