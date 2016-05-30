package arro.diagram.features;


import java.util.Collection;
import java.util.HashMap;

import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.impl.AbstractLayoutFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.services.IPeService;

import arro.Constants;
import arro.domain.ArroNode;
import arro.domain.ArroPad;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;
import util.Logger;
import util.WidgetUtil;


public class ArroNodeLayoutFeature extends AbstractLayoutFeature implements
		ILayoutFeature {

	public ArroNodeLayoutFeature(IFeatureProvider fp) {
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
		IGaService gaService = Graphiti.getGaService();
		if (pictogramElement instanceof ContainerShape) {
			ContainerShape cs = (ContainerShape) pictogramElement;
			
			Rectangle invisibleRectangle = WidgetUtil.getInvisibleRectangle(cs);
			
			RoundedRectangle rect = WidgetUtil.getRoundedRectangle(cs);
			Graphiti.getGaLayoutService().setLocationAndSize(rect, Constants.HALF_PAD_SIZE, 0,
					invisibleRectangle.getWidth() - Constants.PAD_SIZE,
					invisibleRectangle.getHeight());

			Graphiti.getGaLayoutService().setLocationAndSize(WidgetUtil.getTextType(cs), 0, 0, rect.getWidth(), 20);
			Graphiti.getGaLayoutService().setLocationAndSize(WidgetUtil.getTextName(cs), 0, 20, rect.getWidth(), 20);
			
			Polyline p = WidgetUtil.getSeparator(cs);
			EList<Point> points = p.getPoints();
			points.get(0).setX(Constants.HALF_PAD_SIZE);
			points.get(1).setX(invisibleRectangle.getWidth() - Constants.HALF_PAD_SIZE);
			
            // Update anchors - first find out how many anchors and their name
            NonEmfDomainObject domainObject = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs, getFeatureProvider());
            if(domainObject instanceof ArroNode) {
            	// Read the Node info using node-type
            	try {
                    Collection<ArroPad> pads = ((ArroNode)domainObject).getAssociatedModule().getPads();
                    Logger.out.trace(Logger.EDITOR, "Found " + pads.size() + " pads");
                    
                    HashMap<String, ContainerShape> anchors = WidgetUtil.getAnchors(cs);

                    int i = 0;
                    for(ArroPad pad: pads) {
                    	ContainerShape anchorContainer = anchors.get(pad.getName());
                    	GraphicsAlgorithm text = anchorContainer.getGraphicsAlgorithm();
                        ArroNodeAnchorPosition pos = new ArroNodeAnchorPosition(rect, pad.getInput(), i);
                        gaService.setLocationAndSize(text, pos.boxPosX(), pos.boxPosY(), pos.boxSizeX(), pos.boxSizeY());
                        i++;
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
		}
		return false;
	}
}
