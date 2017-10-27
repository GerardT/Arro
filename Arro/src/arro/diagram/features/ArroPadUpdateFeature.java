package arro.diagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.DefaultUpdateDiagramFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

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
                return new ArroPadHelper(getDiagram()).update(cs, getFeatureProvider(), input, name);

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
