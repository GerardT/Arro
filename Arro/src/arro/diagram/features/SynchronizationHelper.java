package arro.diagram.features;

import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;

import arro.Constants;
import arro.domain.ArroSynchronization;
import util.Logger;
import util.WidgetUtil;

public class SynchronizationHelper {
	
	private ContainerShape containerShape;  // this will be the PictogramElement: (ContainerShape) pictogramElement
	private Rectangle invisibleRectangle;
	private Polyline polyline1, polyline2;
	private GraphicsAlgorithm anch1, anch2;
	
	
	// Can't make it a object attribute since this code is called from different
	// contexts (so different object instances)!
	
	public ContainerShape create(IAddContext context, ArroSynchronization addedDomainObject, Color fg, Color bg) {
	    final int width = 200;
	    final int height = 50;

	    Diagram targetDiagram = (Diagram) context.getTargetContainer();
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();
		BoxRelativeAnchor boxAnchor1, boxAnchor2;

		/////// CONTAINER ///////
		containerShape = peCreateService.createContainerShape(targetDiagram, true);
		
		if(addedDomainObject.getStart()) {
	        Graphiti.getPeService().setPropertyValue(containerShape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_SYNCHRONIZATION_IN);
		} else {
	        Graphiti.getPeService().setPropertyValue(containerShape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_SYNCHRONIZATION_OUT);
		}

        // create invisible outer rectangle expanded by
        // the width needed for the anchor
        invisibleRectangle = gaService.createInvisibleRectangle(containerShape);
        {
            gaService.setLocationAndSize(invisibleRectangle, context.getX(), context.getY(), width, height);
     
        }

		/////// horizontal line1 ///////
		{
	        // create and set graphics algorithm
	        polyline1 =
	            gaService.createPolyline(invisibleRectangle, new int[] { 0, Constants.PAD_SIZE, width, Constants.PAD_SIZE });
	        polyline1.setForeground(fg);
	        polyline1.setLineWidth(2);
		}
		/////// horizontal line2 ///////
		{
	        // create and set graphics algorithm
	        polyline2 =
	            gaService.createPolyline(invisibleRectangle, new int[] { 0, Constants.PAD_SIZE + 5, width, Constants.PAD_SIZE + 5 });
	        polyline2.setForeground(fg);
	        polyline2.setLineWidth(2);
		}
        {
     		boxAnchor1 = peCreateService.createBoxRelativeAnchor(containerShape);                 		
	
    		boxAnchor1.setRelativeWidth(0.0);
    		boxAnchor1.setRelativeHeight(0.0/*0.38*/); // Use golden section
            
            anch1 = gaService.createRoundedRectangle(boxAnchor1, 50, 50);
        }
        {
     		boxAnchor2 = peCreateService.createBoxRelativeAnchor(containerShape);                 		
	
    		boxAnchor2.setRelativeWidth(0.0);
    		boxAnchor2.setRelativeHeight(0.0/*0.38*/); // Use golden section
            
            anch2 = gaService.createRoundedRectangle(boxAnchor2, 50, 50);
        }
        if(addedDomainObject.getStart()) {
            Graphiti.getPeService().setPropertyValue(boxAnchor1, Constants.PROP_PAD_NAME_KEY, Constants.PROP_PAD_NAME_SYNC_START_IN);
            Graphiti.getPeService().setPropertyValue(boxAnchor2, Constants.PROP_PAD_NAME_KEY, Constants.PROP_PAD_NAME_SYNC_START_OUT);
        } else {
            Graphiti.getPeService().setPropertyValue(boxAnchor1, Constants.PROP_PAD_NAME_KEY, Constants.PROP_PAD_NAME_SYNC_STOP_IN);
            Graphiti.getPeService().setPropertyValue(boxAnchor2, Constants.PROP_PAD_NAME_KEY, Constants.PROP_PAD_NAME_SYNC_STOP_OUT);
        }
        
        layout(addedDomainObject.getStart(), fg, bg);
        
        return containerShape;
       
	}
	public void read(ILayoutContext context, ContainerShape cs, Color fg, Color bg) {
		boolean in;
		containerShape = cs;
		invisibleRectangle = WidgetUtil.getInvisibleRectangle(containerShape);
		
		String val = Graphiti.getPeService().getPropertyValue(containerShape, Constants.PROP_PICT_KEY);
		if(val.equals(Constants.PROP_PICT_SYNCHRONIZATION_IN)) {
			in = true;
		} else {
			in = false;
		}

		EList<GraphicsAlgorithm> list = invisibleRectangle.getGraphicsAlgorithmChildren();
		
		for(GraphicsAlgorithm innerGa: list) {
			if (innerGa instanceof Polyline && polyline1 == null) {
				polyline1 = (Polyline) innerGa;
			} else if (innerGa instanceof Polyline && polyline2 == null) {
				polyline2 = (Polyline) innerGa;
			}
		}

		
		AnchorContainer anchorList = (AnchorContainer) containerShape;
		EList<Anchor>anchors = anchorList.getAnchors();
		for(Anchor anchor: anchors) {
			Logger.out.trace(Logger.EDITOR, anchor.toString());
			if(anchor instanceof BoxRelativeAnchor && anch1 == null) {
				anch1 = ((BoxRelativeAnchor)anchor).getGraphicsAlgorithm();
			}
			else if(anchor instanceof BoxRelativeAnchor && anch2 == null) {
				anch2 = ((BoxRelativeAnchor)anchor).getGraphicsAlgorithm();
			}
		}
        layout(in, fg, bg);
	    
	}
	private void layout(boolean in, Color fg, Color bg) {
	    final int width = invisibleRectangle.getWidth();
		IGaService gaService = Graphiti.getGaService();
		
		EList<Point> points = polyline1.getPoints();
		points.get(0).setX(0);
		points.get(0).setY(Constants.PAD_SIZE);
		points.get(1).setX(width);
		points.get(1).setY(Constants.PAD_SIZE);
        
		points = polyline2.getPoints();
		points.get(0).setX(0);
		points.get(0).setY(Constants.PAD_SIZE + 5);
		points.get(1).setX(width);
		points.get(1).setY(Constants.PAD_SIZE + 5);
		
		if(in) {
			polyline2.setLineStyle(LineStyle.DASH);
		} else {
			polyline1.setLineStyle(LineStyle.DASH);
		}
        
        anch1.setFilled(true);
        anch1.setForeground(fg);
        anch1.setBackground(bg);
        anch1.setLineWidth(2);
        gaService.setLocationAndSize(anch1, (width / 2) - Constants.HALF_PAD_SIZE, 0,
        		                            Constants.PAD_SIZE, Constants.PAD_SIZE);

        anch2.setFilled(true);
        anch2.setForeground(fg);
        anch2.setBackground(bg);
        anch2.setLineWidth(2);
        gaService.setLocationAndSize(anch2, (width / 2) - Constants.HALF_PAD_SIZE, Constants.PAD_SIZE + 5,
        		                            Constants.PAD_SIZE, Constants.PAD_SIZE);
	}
}
