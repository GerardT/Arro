package arro.diagram.features;

import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
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
import arro.domain.ArroStep;
import util.Logger;
import util.WidgetUtil;

public class StepHelper {
	
	private ContainerShape containerShape;  // this will be the PictogramElement: (ContainerShape) pictogramElement
	private Rectangle invisibleRectangle;
	private Ellipse roundedRectangle;
	private Text text;
	private GraphicsAlgorithm anch1, anch2;
    private Diagram diagram;	
	
    public StepHelper(Diagram d) {
        diagram = d;
    }
    
	// Can't make it a object attribute since this code is called from different
	// contexts (so different object instances)!
	
	public ContainerShape create(IAddContext context, ArroStep addedDomainObject) {
	    final int width = 100;
	    final int height = 100;

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
            if(addedDomainObject.getName().equalsIgnoreCase(Constants.PROP_CONTEXT_READY_STEP)) {
                gaService.setLocationAndSize(invisibleRectangle, context.getX() + 50, context.getY() + 50, width, height);
            } else if(addedDomainObject.getName().equalsIgnoreCase(Constants.PROP_CONTEXT_TERM_STEP)) {
                gaService.setLocationAndSize(invisibleRectangle, context.getX() + 50, context.getY() + 250, width, height);
            } else {
                gaService.setLocationAndSize(invisibleRectangle, context.getX(), context.getY(), width, height);
            }
        }

		{
            roundedRectangle = gaService.createEllipse(invisibleRectangle);
			
		}
		
		/////// name text ///////
        {
        	Shape textShape = peCreateService.createShape(containerShape, false);
			text = gaService.createText(textShape, addedDomainObject.getName());
	        Color fg = Graphiti.getGaService().manageColor(diagram, Constants.TEXT_BG);
	        text.setForeground(fg);
			text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
			text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
        }
        
        {
            peCreateService.createChopboxAnchor(containerShape);
        }
        
        redraw();
        
        return containerShape;
       
	}
	
    public void layout(ContainerShape cs) {
        read(cs); 
        redraw();
    }
    

	// Retrieve all the text and anchor objects so they can be updated in layout function.
	// Just rewriting the diagram would break e.g. connections that the anchors may have.
	private void read(ContainerShape cs) {
		containerShape = cs;
		invisibleRectangle = WidgetUtil.getInvisibleRectangle(containerShape);
		roundedRectangle = WidgetUtil.getEllipse(containerShape);
		
	    for (Shape shape : containerShape.getChildren()) {
	    	if(!(shape instanceof ContainerShape)) {
		    	GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();

		        if (graphicsAlgorithm instanceof Text) {
		            text = (Text) shape.getGraphicsAlgorithm();
		            
		    	}
	    	}
	    }
		AnchorContainer anchorList = containerShape;
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
	    
	}
	private void redraw() {
	    final int width = invisibleRectangle.getWidth();
	    final int height = invisibleRectangle.getHeight();
		IGaService gaService = Graphiti.getGaService();
		
        Color fg = Graphiti.getGaService().manageColor(diagram, Constants.STEP_FOREGROUND);
        Color bg = Graphiti.getGaService().manageColor(diagram, Constants.STEP_BACKGROUND);
        
		gaService.setLocationAndSize(roundedRectangle, 0, 0, width, height);
		roundedRectangle.setFilled(true);
		roundedRectangle.setTransparency(0.1);

        roundedRectangle.setForeground(fg);
        roundedRectangle.setBackground(bg);
        roundedRectangle.setLineWidth(2);

        Graphiti.getGaLayoutService().setLocationAndSize(text, 0, height / 2 - 10, roundedRectangle.getWidth(), 20);
	}
}
