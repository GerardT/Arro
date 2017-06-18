package arro.diagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.DefaultUpdateDiagramFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;

import arro.Constants;
import arro.domain.ArroPad;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;

/**
 * The behavior of the update is straight forward, when the user is changing the domain
 * directly via the diagram. For example if the user changes a text in the diagram using
 * direct-editing or the property-sheet, then this text should be changed in the diagram immediately.
 * 
 */
public class ArroPadUpdateFeature  extends DefaultUpdateDiagramFeature {

	public ArroPadUpdateFeature(IFeatureProvider fp) {
		super(fp);
	}
	 
    @Override
    public IReason updateNeeded(IUpdateContext context) {
    	return super.updateNeeded(context);
    }
 
    @Override
    public boolean update(IUpdateContext context) {
        PictogramElement pictogramElement = context.getPictogramElement();
        
        NonEmfDomainObject bo = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(pictogramElement, getFeatureProvider());
        if(bo instanceof ArroPad) {
        	ArroPad pad = (ArroPad) bo;
        	String name = pad.getName();
        	boolean input = pad.getInput();
        	
            // Set name in pictogram
            if (pictogramElement instanceof ContainerShape) {
                ContainerShape cs = (ContainerShape) pictogramElement;
                
    	        Graphiti.getPeService().setPropertyValue(cs, Constants.PROP_PAD_INPUT_KEY,
    	        										input?Constants.PROP_TRUE_VALUE : Constants.PROP_FALSE_VALUE);

                
                int i = 0;
                for (Shape shape : cs.getChildren()) {
                	GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();

                    if (graphicsAlgorithm instanceof org.eclipse.graphiti.mm.algorithms.Text && i == 0) {
//                        Text text = (Text) shape.getGraphicsAlgorithm();
//                        text.setValue(type + " :");
                        i++;
                	}
                	else if(graphicsAlgorithm instanceof org.eclipse.graphiti.mm.algorithms.Text && i == 1) {
                        org.eclipse.graphiti.mm.algorithms.Text text = (org.eclipse.graphiti.mm.algorithms.Text) shape.getGraphicsAlgorithm();
                        text.setValue(name);
     				}
                }
        		ContainerShape containerShape = (ContainerShape) pictogramElement;
        		GraphicsAlgorithm graphicsAlgorithm = containerShape.getGraphicsAlgorithm();
        
        		if (graphicsAlgorithm instanceof RoundedRectangle) {
        			RoundedRectangle rr = (RoundedRectangle) graphicsAlgorithm;
        	        
        	        if(input) {
        	        	rr.setForeground(manageColor(Constants.PAD_FOREGROUND_INPUT));
        	        	rr.setBackground(manageColor(Constants.PAD_BACKGROUND_INPUT));
        	        } else {
        	        	rr.setForeground(manageColor(Constants.PAD_FOREGROUND_OUTPUT));
        	        	rr.setBackground(manageColor(Constants.PAD_BACKGROUND_OUTPUT));
        	        }
        		}

                return true;
            }
            return false;
        }
		
        return true;
    }
    
	@Override
	public boolean hasDoneChanges() {
		return true;
	}
}
