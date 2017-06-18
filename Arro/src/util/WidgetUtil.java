package util;

import java.util.HashMap;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;

import arro.Constants;


/**
 * 
 * 		PE -> widget (what swt calls 'shape')?
 *   	GA -> shape?
 *
 */
public class WidgetUtil {

	public static Text getTextType(ContainerShape cs) {
	    // update the node name and node type text fields
	    int i = 0;
	    for (Shape shape : cs.getChildren()) {
	    	if(!(shape instanceof ContainerShape)) {
		    	GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();

		    	// First Text is 'type'
		        if (graphicsAlgorithm instanceof Text && i == 0) {
		            Text text = (Text) shape.getGraphicsAlgorithm();
		            return text;
		    	}
	    	}
	    }
	    return null;
	}
	
	public static Text getTextName(ContainerShape cs) {
	    // update the node name and node type text fields
	    int i = 0;
	    for (Shape shape : cs.getChildren()) {
	    	if(!(shape instanceof ContainerShape)) {
		    	GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();

		    	// Second Text is 'name'
		        if (graphicsAlgorithm instanceof Text && i == 0) {
		            i++; // this is the 'type' so skip
		    	}
		    	else if(graphicsAlgorithm instanceof Text && i == 1) {
		            Text text = (Text) shape.getGraphicsAlgorithm();
		            return text;
				}
	    	}
	    }
	    return null;
	}

    public static RoundedRectangle getRoundedRectangle(ContainerShape cs) {
        GraphicsAlgorithm invisibleRectangle = cs.getGraphicsAlgorithm();
        
        EList<GraphicsAlgorithm> list = invisibleRectangle.getGraphicsAlgorithmChildren();
        
        for(GraphicsAlgorithm innerGa: list) {
            if (innerGa instanceof RoundedRectangle) {
                return (RoundedRectangle) innerGa;
            }
        }
        return null;
    }
    
    public static Ellipse getEllipse(ContainerShape cs) {
        GraphicsAlgorithm invisibleRectangle = cs.getGraphicsAlgorithm();
        
        EList<GraphicsAlgorithm> list = invisibleRectangle.getGraphicsAlgorithmChildren();
        
        for(GraphicsAlgorithm innerGa: list) {
            if (innerGa instanceof RoundedRectangle) {
                return (Ellipse) innerGa;
            }
        }
        return null;
    }
    
	public static Polyline getSeparator(ContainerShape cs) {
		GraphicsAlgorithm invisibleRectangle = cs.getGraphicsAlgorithm();
		
		EList<GraphicsAlgorithm> list = invisibleRectangle.getGraphicsAlgorithmChildren();
		
		for(GraphicsAlgorithm innerGa: list) {
			if (innerGa instanceof Polyline) {
				return (Polyline) innerGa;
			}
		}
		return null;
	}
	
	public static Rectangle getInvisibleRectangle(ContainerShape cs) {
		GraphicsAlgorithm invisibleRectangle = cs.getGraphicsAlgorithm();
		if(invisibleRectangle instanceof Rectangle) {
			return (Rectangle) invisibleRectangle;
		} else {
			return null;
		}
	}

	public static HashMap<String, ContainerShape> getAnchors(ContainerShape cs) {
		HashMap<String, ContainerShape> pads = new HashMap<String, ContainerShape>();
		
		EList<Shape> list = cs.getChildren();
	    for (Shape shape : list) {
	    	if(shape instanceof ContainerShape) {
                String name = Graphiti.getPeService().getPropertyValue(shape, Constants.PROP_PAD_NAME_KEY);
                
				pads.put(name, (ContainerShape)shape);
	    	}
	    }
	    return pads;
	}
	
    /**
     * Get the parent shape of the anchor.
     * 
     * @param anchor
     * @return
     */
    public static ContainerShape getCsFromAnchor(BoxRelativeAnchor anchor) {
        AnchorContainer cont = anchor.getParent();
        EObject main = cont.eContainer();
        if(main instanceof Diagram /* FIXME this is to make ArroPad work */) {
            return (ContainerShape) cont;
        } else {
            if(main instanceof ContainerShape) {
                return (ContainerShape)main;
            }
        }
        Logger.out.trace(Logger.ERROR, "Couldn't find parent container");

        return null;
    }
    
    public static ContainerShape getCsFromChopBoxAnchor(ChopboxAnchor anchor) {
        AnchorContainer cont = ((Anchor) anchor).getParent();
        EObject main = cont.eContainer();
        if(main instanceof Diagram /* FIXME this is to make ArroPad work */) {
            return (ContainerShape) cont;
        } else {
            if(main instanceof ContainerShape) {
                return (ContainerShape)main;
            }
        }
        Logger.out.trace(Logger.ERROR, "Couldn't find parent container");

        return null;
    }
    
    
}
