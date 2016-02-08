package arro.diagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.DefaultUpdateDiagramFeature;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import arro.domain.ArroState;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;

/**
 * The behavior of the update is straight forward, when the user is changing the domain data
 * directly via the diagram. For example if the user changes a text in the diagram using
 * direct-editing or the property-sheet, then this text should be changed in the diagram immediately.
 * 
 */
public class StateBlockUpdateFeature  extends DefaultUpdateDiagramFeature {

	public StateBlockUpdateFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	/**
	 * Note, this function returning Reason.createTrueReason() would normally
	 * put a red dotted box around the PE. However, since the top level shape
	 * of the PE is the invisibleRecangle, the red dotted box remains invisible..
	 * 
	 * Tried also with box decorator, but that would change the rendering of PE
	 * itself, ugly. So decided to decorate with icon is update needed.
	 */
    public IReason updateNeeded(IUpdateContext context) {
        PictogramElement pictogramElement = context.getPictogramElement();
        NonEmfDomainObject bo = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(pictogramElement, getFeatureProvider());;

        if(bo instanceof ArroState) {
            //String type = ((ArroNode)bo).getType();
            String name = bo.getName();
            
            if (pictogramElement instanceof ContainerShape) {
                ContainerShape cs = (ContainerShape) pictogramElement;
            }
            return Reason.createFalseReason();
        }
            
    	return super.updateNeeded(context);
    }
    
    public boolean canUpdate(IUpdateContext context) {
		return true;
    }
 
    /*
     * Guess this is not automatically called unless AutoUpdate flags are set. See tutorial.
     */
    public boolean update(IUpdateContext context) {
        PictogramElement pictogramElement = context.getPictogramElement();
        NonEmfDomainObject bo = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(pictogramElement, getFeatureProvider());
        
        return true;
    }
    
}
