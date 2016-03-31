package arro.diagram.features;

import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;

import arro.Constants;
import arro.domain.ArroState;
import util.Logger;
import util.WidgetUtil;

public class StepHelper {
	
	private ContainerShape containerShape;  // this will be the PictogramElement: (ContainerShape) pictogramElement
	private Rectangle invisibleRectangle;
	private RoundedRectangle roundedRectangle;
	private Text text;
	private GraphicsAlgorithm anch1, anch2;
	
	
	// Can't make it a object attribute since this code is called from different
	// contexts (so different object instances)!
	
	public ContainerShape create(IAddContext context, ArroState addedDomainObject, Color fg, Color bg) {
	    final int width = 200;
	    final int height = 50;

	    Diagram targetDiagram = (Diagram) context.getTargetContainer();
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		/////// CONTAINER ///////
		containerShape = peCreateService.createContainerShape(targetDiagram, true);
		
        Graphiti.getPeService().setPropertyValue(containerShape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_STEP);

        // create invisible outer rectangle expanded by
        // the width needed for the anchor
        invisibleRectangle = gaService.createInvisibleRectangle(containerShape);
        {
            gaService.setLocationAndSize(invisibleRectangle, context.getX(), context.getY(), width, height);
        }

		{
			roundedRectangle = gaService.createRoundedRectangle(invisibleRectangle, 5, 5);
			
		}
		
		/////// name text ///////
        {
        	Shape textShape = peCreateService.createShape(containerShape, false);
			text = gaService.createText(textShape, addedDomainObject.getName());
	        text.setForeground(fg);
			text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
			text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
        }
        
        {
     		final BoxRelativeAnchor boxAnchor = peCreateService.createBoxRelativeAnchor(containerShape);                 		
	
    		boxAnchor.setRelativeWidth(0.0);
    		boxAnchor.setRelativeHeight(0.0/*0.38*/); // Use golden section
            
            Graphiti.getPeService().setPropertyValue(boxAnchor, Constants.PROP_PAD_NAME_KEY, Constants.PROP_PAD_NAME_STEP_IN);

            anch1 = gaService.createRoundedRectangle(boxAnchor, 50, 50);
        }

        {
     		final BoxRelativeAnchor boxAnchor = peCreateService.createBoxRelativeAnchor(containerShape);                 		
	
    		boxAnchor.setRelativeWidth(0.0);
    		boxAnchor.setRelativeHeight(0.0/*0.38*/); // Use golden section
            
            Graphiti.getPeService().setPropertyValue(boxAnchor, Constants.PROP_PAD_NAME_KEY, Constants.PROP_PAD_NAME_STEP_OUT);

            anch2 = gaService.createRoundedRectangle(boxAnchor, 50, 50);
        }
        
        layout(fg, bg);
        
        return containerShape;
       
	}
	public void read(ILayoutContext context, ContainerShape cs, Color fg, Color bg) {
		containerShape = cs;
		invisibleRectangle = WidgetUtil.getInvisibleRectangle(containerShape);
		roundedRectangle = WidgetUtil.getRoundedRectangle(containerShape);
		
	    for (Shape shape : containerShape.getChildren()) {
	    	if(!(shape instanceof ContainerShape)) {
		    	GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();

		        if (graphicsAlgorithm instanceof Text) {
		            text = (Text) shape.getGraphicsAlgorithm();
		            
		    	}
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
        layout(fg, bg);
	    
	}
	private void layout(Color fg, Color bg) {
	    final int width = invisibleRectangle.getWidth();
	    final int height = invisibleRectangle.getHeight();
		IGaService gaService = Graphiti.getGaService();
		
		gaService.setLocationAndSize(roundedRectangle,
				Constants.HALF_PAD_SIZE, Constants.HALF_PAD_SIZE,
				width - Constants.PAD_SIZE, height - Constants.PAD_SIZE);
		roundedRectangle.setFilled(false);
        roundedRectangle.setForeground(fg);
        roundedRectangle.setBackground(bg);
        roundedRectangle.setLineWidth(2);

        Graphiti.getGaLayoutService().setLocationAndSize(text, 0, 20, roundedRectangle.getWidth(), 20);
        
        anch1.setFilled(true);
        anch1.setForeground(fg);
        anch1.setBackground(bg);
        anch1.setLineWidth(2);
        gaService.setLocationAndSize(anch1, width / 2 - Constants.HALF_PAD_SIZE, 0,
        		                            Constants.PAD_SIZE, Constants.PAD_SIZE);

        anch2.setFilled(true);
        anch2.setForeground(fg);
        anch2.setBackground(bg);
        anch2.setLineWidth(2);
        gaService.setLocationAndSize(anch2, width / 2 - Constants.HALF_PAD_SIZE, height - Constants.PAD_SIZE,
        		                            Constants.PAD_SIZE, Constants.PAD_SIZE);
	}
}
