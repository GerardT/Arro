package arro.diagram.features;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
import org.eclipse.graphiti.mm.algorithms.styles.Font;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.services.IPeService;

import arro.Constants;
import arro.domain.ArroModule;
import arro.domain.ArroNode;
import arro.domain.ArroPad;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;
import util.Logger;
import util.WidgetUtil;

public class NodeHelper {
	
	private ContainerShape containerShape;  // this will be the PictogramElement: (ContainerShape) pictogramElement
	private Rectangle invisibleRectangle;
	private RoundedRectangle rectangle;
	private Polyline divider;
	private Text nodeNameText;
	private Text nodeTypeText;
	private Diagram diagram;
	private Collection<ArroPad> domainPads = null;
	private HashMap<String, ContainerShape> anchors = null;
	
	public NodeHelper(Diagram d) {
	    diagram = d;
	}
	
	// Can't make it a object attribute since this code is called from different
	// contexts (so different object instances)!
	
	public ContainerShape create(IAddContext context, ArroNode addedDomainObject) {
        IPeCreateService peCreateService = Graphiti.getPeCreateService();  // widget?
        IGaService gaService = Graphiti.getGaService(); // shape?
        
        int width = 200;
        int height = 50;

        /////// CONTAINER ///////
        containerShape = peCreateService.createContainerShape(diagram, true);
        
        Graphiti.getPeService().setPropertyValue(containerShape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_NODE);

        // create invisible outer rectangle expanded by
        // the width needed for the anchor
        invisibleRectangle = gaService.createInvisibleRectangle(containerShape);
        {
            gaService.setLocationAndSize(invisibleRectangle, context.getX(), context.getY(),
                                                             width, height);
     
        }

        rectangle = gaService.createRoundedRectangle(invisibleRectangle, 5, 5);
        
        divider = gaService.createPolyline(invisibleRectangle, new int[] { 0, 40, width, 40 });
 
        Shape nodeTypeShape = peCreateService.createShape(containerShape, false);
        nodeTypeText = gaService.createText(nodeTypeShape, addedDomainObject.getType() + " :");
        
        Shape nodeNameShape = peCreateService.createShape(containerShape, false);
        nodeNameText = gaService.createText(nodeNameShape, addedDomainObject.getName());

        redraw();
        
        return containerShape;
       
	}
    public void layout(ContainerShape cs, IFeatureProvider fp) {
        read(cs, fp); 
        redraw();
    }
    
    
	
    public IReason updateNeeded(IUpdateContext context, ContainerShape cs, IFeatureProvider fp) {
        NonEmfDomainObject bo = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs, fp);
        
        read(cs, fp);

        String type = ((ArroNode)bo).getType();
        String name = bo.getName();
        
        // Update anchors - first find out how many anchors and their name
        NonEmfDomainObject domainObject = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs, fp);
        if(domainObject instanceof ArroNode) {
        
            // cannot do updates here, just check if the same
            if(nodeTypeText.getValue().equals(type + " :") == false) {
                ((ArroNode)domainObject).setNeedsUpdate(true);
                return Reason.createTrueReason("Type changed");
            } else if(nodeNameText.getValue().equals(name) == false) {
                ((ArroNode)domainObject).setNeedsUpdate(true);
                return Reason.createTrueReason("Name changed");
            }
        
            try {
                HashMap<String, ContainerShape> oldAnchors = new HashMap<String, ContainerShape>();
                oldAnchors.putAll(anchors);
                for(ArroPad pad: domainPads) {
                    //Logger.out.trace(Logger.EDITOR, "Node has a Pad " + pad.toString());
                    ContainerShape a = oldAnchors.get(pad.getName());
                    
                    if(a != null) {
                        // Domain anchor also found in diagram: remove from oldAnchors, do nothing.
                        oldAnchors.remove(pad.getName());
                    } else {
                        // Domain anchor not found in diagram
                        ((ArroNode)domainObject).setNeedsUpdate(true);
                        return Reason.createTrueReason("Pad was added");
                    }
                }
                
                if(oldAnchors.isEmpty() == false) {
                    // Apparently pads were added in the domain.
                    ((ArroNode)domainObject).setNeedsUpdate(true);
                    return Reason.createTrueReason("Pad was removed or changed");
                }
            } catch (RuntimeException e) {
                // TODO Auto-generated catch block
                // TODO e.printStackTrace();
            }

            ((ArroNode)domainObject).setNeedsUpdate(false);
            return Reason.createFalseReason();
        }

        return null;
    }
    
    /*
     * Guess this is not automatically called unless AutoUpdate flags are set. See tutorial.
     */
    public boolean update(IUpdateContext context, ContainerShape cs, Font font, IFeatureProvider fp) {
        NonEmfDomainObject bo = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs, fp);
        
        Color fg = Graphiti.getGaService().manageColor(diagram, Constants.MODULE_GONE);
        Color bg = Graphiti.getGaService().manageColor(diagram, Constants.MODULE_GONE);
        
        read(cs, fp);

        String type = ((ArroNode)bo).getType();
        String name = bo.getName();
        
        RoundedRectangle rect = WidgetUtil.getRoundedRectangle(cs);
        
        // update the node name and node type text fields
        nodeTypeText.setValue(type + " :");
        nodeNameText.setValue(name);
        
        // new plan: remove texts, then do update as below, then add back all new texts.
        
        // Update anchors - first find out how many anchors and their name
        NonEmfDomainObject domainObject = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs, fp);
        if(domainObject instanceof ArroNode) {
            boolean changed = false;
            
            IGaService gaService;
            IPeService peService;
            int i;
            
            try {
                // Read the Node info using node-type
                IPeCreateService peCreateService = Graphiti.getPeCreateService();
                gaService = Graphiti.getGaService();
                
                peService = Graphiti.getPeService();

                HashMap<String, ContainerShape> oldAnchors = new HashMap<String, ContainerShape>();
                oldAnchors.putAll(anchors);
                i = 0;
                for(ArroPad pad: domainPads) {
                    //Logger.out.trace(Logger.EDITOR, "Node has a Pad " + pad.toString());
                    ContainerShape a = oldAnchors.get(pad.getName());
                    
                    if(a != null) {
                        // Domain anchor also found in diagram: remove from oldAnchors, do nothing.
                        oldAnchors.remove(pad.getName());
                    } else {
                        // Domain anchor not found in diagram: create new anchor
                        changed = true;
                        
                        // Create container for Anchor, Text and Rectangle
                        // NOTE: the container must be an active shape: http://www.eclipse.org/forums/index.php/t/174858/
                        // So we need to disallow moving, removing, deleting it.
                        // GA for this shape is the Text below.
                        ContainerShape shape = peCreateService.createContainerShape(cs, true);
                        final Rectangle invs = gaService.createInvisibleRectangle(shape);
                        NodeAnchorPosition pos = new NodeAnchorPosition(rect, pad.getInput(), i);
                        gaService.setLocationAndSize(invs, pos.boxPosX(), pos.boxPosY(), pos.boxSizeX(), pos.boxSizeY());
                      
                        Graphiti.getPeService().setPropertyValue(shape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_PASSIVE);

                        createAnchor(shape, font, invs, pad.getInput(), pad.getName());
                    }
                    
                    i++;
                }
                // NOTE: Deleting anchors will remove connections.
                Collection<ContainerShape> toRemove = oldAnchors.values();
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
                    
                    redraw();
                }
                ((ArroNode)domainObject).setNeedsUpdate(false);
            } catch (Exception e) {
                rect.setBackground(bg);
                rect.setForeground(fg);
            }
            
        }
        return true;
    }
    
    private void createAnchor(ContainerShape shape, Font font, Rectangle invs, boolean leftSide, String padName) {
        IPeCreateService peCreateService = Graphiti.getPeCreateService();
        IGaService gaService = Graphiti.getGaService();
        
        final BoxRelativeAnchor boxAnchor = peCreateService.createBoxRelativeAnchor(shape);                         
        boxAnchor.setReferencedGraphicsAlgorithm(shape.getGraphicsAlgorithm());
        
        Color fg = Graphiti.getGaService().manageColor(diagram, Constants.ANCHOR_FG);
        Color bg = Graphiti.getGaService().manageColor(diagram, Constants.ANCHOR_BG);
        Color tx = Graphiti.getGaService().manageColor(diagram, Constants.TEXT_BG);
        
        boxAnchor.setRelativeWidth(0.0);
        boxAnchor.setRelativeHeight(0.0/*0.38*/); // Use golden section
        
        Graphiti.getPeService().setPropertyValue(shape, Constants.PROP_PAD_NAME_KEY, padName);

        RoundedRectangle anch = gaService.createRoundedRectangle(boxAnchor, 5, 5);
        anch.setFilled(true);
        anch.setForeground(fg);
        anch.setBackground(bg);
        anch.setLineWidth(2);

        // assign a rectangle graphics algorithm for the box relative anchor
        // note, that the rectangle is inside the border of the rectangle shape
        // NOTE: it seems this is necessary to make the BoxRelativeAnchor work!
        Text text = gaService.createText(invs, padName);
        text.setFont(font);
        text.setBackground(tx);
        text.setFilled(false);
        Graphiti.getPeService().setPropertyValue(text, Constants.PROP_PICT_KEY, Constants.PROP_PICT_PASSIVE);

        NodeAnchorPosition pos = new NodeAnchorPosition(text, leftSide, 0);
        gaService.setLocationAndSize(text, pos.textPosX(), pos.textPosY(), pos.textSizeX(), pos.textSizeY());
        gaService.setLocationAndSize(anch, pos.anchorPosX(), pos.anchorPosY(), pos.anchorSizeX(), pos.anchorSizeY());
        text.setHorizontalAlignment(leftSide ? Orientation.ALIGNMENT_LEFT : Orientation.ALIGNMENT_RIGHT);
    }
    
    // Retrieve all the text and anchor objects so they can be updated in layout function.
    // Just rewriting the diagram would break e.g. connections that the anchors may have.
    private void read(ContainerShape cs, IFeatureProvider fp) {
        containerShape = cs;
        invisibleRectangle = WidgetUtil.getInvisibleRectangle(containerShape);
        
        rectangle = WidgetUtil.getRoundedRectangle(cs);
        
        nodeTypeText = WidgetUtil.getTextType(cs);
        nodeNameText = WidgetUtil.getTextName(cs);

        divider = WidgetUtil.getSeparator(cs);
        
        // Update anchors - first find out how many anchors and their name
        NonEmfDomainObject domainObject = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(cs, fp);
        if(domainObject instanceof ArroNode) {
            // Read the Node info using node-type
            try {
                // TODO make it better
                ArroModule m = ((ArroNode)domainObject).getAssociatedModule();
                if(m == null) {
                    // for now, just create empty set.
                    domainPads = new Vector<ArroPad>();
                } else {
                    domainPads = ((ArroNode)domainObject).getAssociatedModule().getPads();
                }
                Logger.out.trace(Logger.EDITOR, "Found " + domainPads.size() + " pads");

                // Each pad has its name as property. Function getAnchors returns a map with name as key.
                anchors = WidgetUtil.getAnchors(cs);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }
    private void redraw() {
        final int width = invisibleRectangle.getWidth();
        final int height = invisibleRectangle.getHeight();
        IGaService gaService = Graphiti.getGaService();
        
        Color fg = Graphiti.getGaService().manageColor(diagram, Constants.CLASS_FOREGROUND);
        Color bg = Graphiti.getGaService().manageColor(diagram, Constants.CLASS_BACKGROUND);
        Color tx = Graphiti.getGaService().manageColor(diagram, Constants.TEXT_BG);
        
        rectangle.setFilled(true);
        rectangle.setTransparency(0.1);
        
        rectangle.setForeground(fg);
        rectangle.setBackground(bg);
        rectangle.setLineWidth(2);
        
        gaService.setLocationAndSize(rectangle, Constants.HALF_PAD_SIZE, 0, width - Constants.PAD_SIZE, height);
        
        EList<Point> points = divider.getPoints();
        points.get(0).setX(Constants.HALF_PAD_SIZE);
        points.get(1).setX(invisibleRectangle.getWidth() - Constants.HALF_PAD_SIZE);
        divider.setForeground(fg);
        divider.setLineWidth(2);

        Graphiti.getGaLayoutService().setLocationAndSize(nodeNameText, 0, 20, rectangle.getWidth(), 20);
        Graphiti.getGaLayoutService().setLocationAndSize(nodeTypeText, 0, 0, rectangle.getWidth(), 20);
        
        nodeTypeText.setForeground(tx);
        nodeTypeText.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
        nodeTypeText.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
        nodeNameText.setForeground(tx);
        nodeNameText.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
        nodeNameText.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);

        if(domainPads != null && anchors != null) {
            int i = 0;
            for(ArroPad pad: domainPads) {
                ContainerShape anchorContainer = anchors.get(pad.getName());
                GraphicsAlgorithm text = anchorContainer.getGraphicsAlgorithm();
                NodeAnchorPosition pos = new NodeAnchorPosition(rectangle, pad.getInput(), i);
                Graphiti.getGaService().setLocationAndSize(text, pos.boxPosX(), pos.boxPosY(), pos.boxSizeX(), pos.boxSizeY());
                i++;
            }
        }
    }

}
