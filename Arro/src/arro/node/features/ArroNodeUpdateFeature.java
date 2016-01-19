package arro.node.features;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.impl.DefaultUpdateDiagramFeature;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.AbstractStyle;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.services.IPeService;

import util.Logger;
import util.WidgetUtil;
import arro.Constants;
import arro.domain.ArroNode;
import arro.domain.ArroPad;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;

/**
 * The behavior of the update is straight forward, when the user is changing the domain data
 * directly via the diagram. For example if the user changes a text in the diagram using
 * direct-editing or the property-sheet, then this text should be changed in the diagram immediately.
 * 
 */
public class ArroNodeUpdateFeature  extends DefaultUpdateDiagramFeature {

	public ArroNodeUpdateFeature(IFeatureProvider fp) {
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
        NonEmfDomainObject bo = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(pictogramElement);
        
        String type = ((ArroNode)bo).getType();
        String name = bo.getName();
        
        if (pictogramElement instanceof ContainerShape) {
            ContainerShape cs = (ContainerShape) pictogramElement;
            
            // Update anchors - first find out how many anchors and their name
            NonEmfDomainObject domainObject = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs);
            if(domainObject instanceof ArroNode) {
            
	            // cannot do updates here, just check if the same
	            if(WidgetUtil.getTextType(cs).getValue().equals(type + " :") == false) {
	    			((ArroNode)domainObject).setNeedsUpdate(true);
	    			return Reason.createTrueReason("Type changed");
	            } else if(WidgetUtil.getTextName(cs).getValue().equals(name) == false) {
	    			((ArroNode)domainObject).setNeedsUpdate(true);
	    			return Reason.createTrueReason("Name changed");
	            }
            
            	// Read the Node info using node-type
            	Collection<ArroPad> domainPads = ((ArroNode)domainObject).getPads();
            	Logger.out.trace(Logger.EDITOR, "Found " + domainPads.size() + " pads");
                
        		HashMap<String, ContainerShape> diagramPads = WidgetUtil.getAnchors(cs);

            	for(ArroPad pad: domainPads) {
            		//Logger.out.trace(Logger.EDITOR, "Node has a Pad " + pad.toString());
            		ContainerShape a = diagramPads.get(pad.getName());
            		
            		if(a != null) {
            			// Domain anchor also found in diagram: remove from oldAnchors, do nothing.
            			diagramPads.remove(pad.getName());
            		} else {
            			// Domain anchor not found in diagram
            			((ArroNode)domainObject).setNeedsUpdate(true);
            			return Reason.createTrueReason("Pad was added");
            		}
                }
            	
            	if(diagramPads.isEmpty() == false) {
            		// Apparently pads were added in the domain.
        			((ArroNode)domainObject).setNeedsUpdate(true);
        			return Reason.createTrueReason("Pad was removed or changed");
            	}

    			((ArroNode)domainObject).setNeedsUpdate(false);
                return Reason.createFalseReason();
            }
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
        NonEmfDomainObject bo = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(pictogramElement);
        
        String type = ((ArroNode)bo).getType();
        String name = bo.getName();
        
        if (pictogramElement instanceof ContainerShape) {
            ContainerShape cs = (ContainerShape) pictogramElement;
            
			RoundedRectangle rect = WidgetUtil.getRoundedRectangle(cs);
            
            // update the node name and node type text fields
            WidgetUtil.getTextType(cs).setValue(type + " :");
            WidgetUtil.getTextName(cs).setValue(name);
            
            
            // new plan: remove texts, then do update as below, then add back all new texts.
            
            // Update anchors - first find out how many anchors and their name
            NonEmfDomainObject domainObject = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs);
            if(domainObject instanceof ArroNode) {
            	boolean changed = false;
            	
            	// Read the Node info using node-type
            	Collection<ArroPad> domainPads = ((ArroNode)domainObject).getPads();
            	Logger.out.trace(Logger.EDITOR, "Found " + domainPads.size() + " pads");
                
           		IPeCreateService peCreateService = Graphiti.getPeCreateService();
        		IGaService gaService = Graphiti.getGaService();
        		
           		IPeService peService = Graphiti.getPeService();
        		HashMap<String, ContainerShape> diagramPads = WidgetUtil.getAnchors(cs);

            	int i = 0;
            	for(ArroPad pad: domainPads) {
            		//Logger.out.trace(Logger.EDITOR, "Node has a Pad " + pad.toString());
            		ContainerShape a = diagramPads.get(pad.getName());
            		
            		if(a != null) {
            			// Domain anchor also found in diagram: remove from oldAnchors, do nothing.
            			diagramPads.remove(pad.getName());
            		} else {
            			// Domain anchor not found in diagram: create new anchor
            			changed = true;
            			
            			// Create container for Anchor, Text and Rectangle
            			// NOTE: the container must be an active shape: http://www.eclipse.org/forums/index.php/t/174858/
            			// So we need to disallow moving, removing, deleting it.
            			// GA for this shape is the Text below.
        	    		ContainerShape shape = peCreateService.createContainerShape(cs, true);
                        final Rectangle invs = gaService.createInvisibleRectangle(shape);
                        ArroNodeAnchorPosition pos = new ArroNodeAnchorPosition(rect, pad.getInput(), i);
                        gaService.setLocationAndSize(invs, pos.boxPosX(), pos.boxPosY(), pos.boxSizeX(), pos.boxSizeY());
                      
                        Graphiti.getPeService().setPropertyValue(shape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_PASSIVE);

            			createAnchor(shape, invs, pad.getInput(), pad.getName());
            		}
            		
                    i++;
                }
            	
        		// NOTE: Deleting anchors will remove connections.
            	Collection<ContainerShape> toRemove = diagramPads.values();
        		for(ContainerShape anchor: toRemove) {
        			peService.deletePictogramElement(anchor);
        			
        			changed = true;
        		}

            	// NOTE: this code would resize invisibleRectangle back to default size even if it
            	// was increased by user (see Layout). So we only set back to default if pads
        		// are added or removed.
            	if(changed) {
        			Rectangle invisibleRectangle = WidgetUtil.getInvisibleRectangle(cs);
        			
                    int x = invisibleRectangle.getX();
                    int y = invisibleRectangle.getY();
                    int width = invisibleRectangle.getWidth();
                    int height = 50 + (15 * i);
        			gaService.setLocationAndSize(invisibleRectangle, x, y, width, height);
        			
        			Graphiti.getGaLayoutService().setLocationAndSize(rect, Constants.HALF_PAD_SIZE, 0, invisibleRectangle.getWidth() - Constants.PAD_SIZE, invisibleRectangle.getHeight());
        			
        			Logger.out.trace(Logger.EDITOR, "locsize (" + x + "," + y + ") (" + width+ "," + height + ")");
        			
        	        // To set location and size.
        		    LayoutContext layoutContext = new LayoutContext(cs);
        		    ILayoutFeature layoutFeature = getFeatureProvider().getLayoutFeature(layoutContext);
        		    layoutFeature.layout(layoutContext);

            	}
    			((ArroNode)domainObject).setNeedsUpdate(false);
            }
            return true;
        }
        return true;
    }
    
    private void createAnchor(ContainerShape shape, Rectangle invs, boolean leftSide, String padName) {
   		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();
   		
 		final BoxRelativeAnchor boxAnchor = peCreateService.createBoxRelativeAnchor(shape);                 		
		boxAnchor.setReferencedGraphicsAlgorithm(shape.getGraphicsAlgorithm());
		
		
		boxAnchor.setRelativeWidth(0.0);
		boxAnchor.setRelativeHeight(0.0/*0.38*/); // Use golden section
        
        Graphiti.getPeService().setPropertyValue(shape, Constants.PROP_PAD_NAME_KEY, padName);

        RoundedRectangle anch = gaService.createRoundedRectangle(boxAnchor, 5, 5);
        anch.setFilled(true);
        anch.setForeground(manageColor(Constants.ANCHOR_FG));
        anch.setBackground(manageColor(Constants.ANCHOR_BG));
        anch.setLineWidth(2);
        
        
        // assign a rectangle graphics algorithm for the box relative anchor
        // note, that the rectangle is inside the border of the rectangle shape
        // NOTE: it seems this is necessary to make the BoxRelativeAnchor work!
		Text text = gaService.createText(invs, padName);
		text.setFont(manageFont(getDiagram(), "Tahoma", 8));
		text.setBackground(manageColor(Constants.ANCHOR_BG));
		text.setFilled(false);
        Graphiti.getPeService().setPropertyValue(text, Constants.PROP_PICT_KEY, Constants.PROP_PICT_PASSIVE);

		
        ArroNodeAnchorPosition pos = new ArroNodeAnchorPosition(text, leftSide, 0);
        gaService.setLocationAndSize(text, pos.textPosX(), pos.textPosY(), pos.textSizeX(), pos.textSizeY());
        gaService.setLocationAndSize(anch, pos.anchorPosX(), pos.anchorPosY(), pos.anchorSizeX(), pos.anchorSizeY());
        text.setHorizontalAlignment(leftSide ? Orientation.ALIGNMENT_LEFT : Orientation.ALIGNMENT_RIGHT);
    	
    }
    
	@Override
	public boolean hasDoneChanges() {
		return true;
	}
	
}
