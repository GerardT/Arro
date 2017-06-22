package arro.diagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.DefaultUpdateDiagramFeature;
import org.eclipse.graphiti.mm.algorithms.styles.Font;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

/**
 * The behavior of the update is straight forward, when the user is changing the domain data
 * directly via the diagram. For example if the user changes a text in the diagram using
 * direct-editing or the property-sheet, then this text should be changed in the diagram immediately.
 * 
 * TODO: implement undo to keep model and diagram in sync!
 * 
 */
public class NodeUpdateFeature  extends DefaultUpdateDiagramFeature {

	public NodeUpdateFeature(IFeatureProvider fp) {
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
	
    @Override
    public IReason updateNeeded(IUpdateContext context) {
        IReason reason = null;
        PictogramElement pictogramElement = context.getPictogramElement();
        if (pictogramElement instanceof ContainerShape) {
            ContainerShape cs = (ContainerShape) pictogramElement;
            reason = new NodeHelper(getDiagram()).updateNeeded(context, cs, getFeatureProvider());
        }
        if(reason != null) {
            return reason;
        } else {
            return super.updateNeeded(context);
        }
    }

    @Override
    public boolean canUpdate(IUpdateContext context) {
        return true;
    }
 
    /*
     * Guess this is not automatically called unless AutoUpdate flags are set. See tutorial.
     */
    @Override
    public boolean update(IUpdateContext context) {
        Font font = manageFont(getDiagram(), "Tahoma", 8);

        PictogramElement pictogramElement = context.getPictogramElement();
        if (pictogramElement instanceof ContainerShape) {
            ContainerShape cs = (ContainerShape) pictogramElement;
            return new NodeHelper(getDiagram()).update(context, cs, font, getFeatureProvider());
        }
        return super.update(context); // TODO correct?
     }
    
	@Override
	public boolean hasDoneChanges() {
		return true;
	}
	
}
