package arro.diagram.features;

import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;

import arro.Constants;
import arro.domain.ArroPad;
import arro.domain.NonEmfDomainObject;
import arro.domain.POJOIndependenceSolver;
import util.Logger;

public class ArroPadHelper {
    private Diagram diagram;    
    int width = 120;
    int height = 42;
    
    public ArroPadHelper(Diagram d) {
        diagram = d;
    }

    public ContainerShape create(IAddContext context, ArroPad addedDomainObject, int docType, boolean isInput) {

        ContainerShape targetDiagram = context.getTargetContainer();
        IPeCreateService peCreateService = Graphiti.getPeCreateService();
        IGaService gaService = Graphiti.getGaService();
        

        /////// CONTAINER ///////
        ContainerShape containerShape = peCreateService.createContainerShape(targetDiagram, true);
        Graphiti.getPeService().setPropertyValue(containerShape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_PAD);
        Graphiti.getPeService().setPropertyValue(containerShape, Constants.PROP_PAD_INPUT_KEY, Constants.PROP_TRUE_VALUE);

        
        RoundedRectangle roundedRectangle = gaService.createRoundedRectangle(containerShape, 36, 36 /*5, 5*/);
        {
            gaService.setLocationAndSize(roundedRectangle, context.getX(), context.getY(), width, height);
            roundedRectangle.setFilled(true);
            if(isInput) {
                roundedRectangle.setForeground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_FOREGROUND_INPUT));
                roundedRectangle.setBackground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_BACKGROUND_INPUT));
            } else {
                roundedRectangle.setForeground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_FOREGROUND_OUTPUT));
                roundedRectangle.setBackground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_BACKGROUND_OUTPUT));
            }
            roundedRectangle.setLineWidth(2);
        }
        
        /////// class text ///////
        {
            Shape shape = peCreateService.createShape(containerShape, false);
            Text text = gaService.createText(shape, addedDomainObject.getType() + " :");
            text.setForeground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_TEXT_FOREGROUND));
            text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
            text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
        }

        /////// name text ///////
        {
            Shape shape = peCreateService.createShape(containerShape, false);
            Text text = gaService.createText(shape, addedDomainObject.getName());
            text.setForeground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_TEXT_FOREGROUND));
            text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
            text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
        }

        if(docType != Constants.CodeBlockPython && docType != Constants.CodeBlockNative && docType != Constants.CodeBlockHtml) {
            /////// anchor ///////
            // create an additional box relative anchor at middle-right
            final BoxRelativeAnchor boxAnchor = peCreateService.createBoxRelativeAnchor(containerShape);
           
            boxAnchor.setRelativeWidth(1.0);
            boxAnchor.setRelativeHeight(0.38); // Use golden section
            boxAnchor.setReferencedGraphicsAlgorithm(roundedRectangle);

            // assign a rectangle graphics algorithm for the box relative anchor
            // note, that the rectangle is inside the border of the rectangle shape
            // NOTE: it seems this is necessary to make the BoxRelativeAnchor work!
            final Ellipse ellipse = gaService.createEllipse(boxAnchor);
            ellipse.setForeground(Graphiti.getGaService().manageColor(diagram, Constants.ANCHOR_FG));
            ellipse.setBackground(Graphiti.getGaService().manageColor(diagram, Constants.ANCHOR_BG));
            ellipse.setLineWidth(2);
            gaService.setLocationAndSize(ellipse, - Constants.PAD_SIZE, 0, Constants.PAD_SIZE, Constants.PAD_SIZE);
        }
        return containerShape;
    }

    public boolean update(ContainerShape cs, IFeatureProvider featureProvider, boolean input, String name) {
        
        Graphiti.getPeService().setPropertyValue(cs, Constants.PROP_PAD_INPUT_KEY,
                                                input?Constants.PROP_TRUE_VALUE : Constants.PROP_FALSE_VALUE);

        
        int i = 0;
        for (Shape shape : cs.getChildren()) {
            GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();

            if (graphicsAlgorithm instanceof org.eclipse.graphiti.mm.algorithms.Text && i == 0) {
//                Text text = (Text) shape.getGraphicsAlgorithm();
//                text.setValue(type + " :");
                i++;
            }
            else if(graphicsAlgorithm instanceof org.eclipse.graphiti.mm.algorithms.Text && i == 1) {
                org.eclipse.graphiti.mm.algorithms.Text text = (org.eclipse.graphiti.mm.algorithms.Text) shape.getGraphicsAlgorithm();
                text.setValue(name);
            }
        }
        GraphicsAlgorithm graphicsAlgorithm = cs.getGraphicsAlgorithm();

        if (graphicsAlgorithm instanceof RoundedRectangle) {
            RoundedRectangle rr = (RoundedRectangle) graphicsAlgorithm;
            
            if(input) {
                rr.setForeground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_FOREGROUND_INPUT));
                rr.setBackground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_BACKGROUND_INPUT));
            } else {
                rr.setForeground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_FOREGROUND_OUTPUT));
                rr.setBackground(Graphiti.getGaService().manageColor(diagram, Constants.PAD_BACKGROUND_OUTPUT));
            }
        }

        layout(cs, featureProvider);
        
        return true;
    }

    public boolean layout(ContainerShape containerShape, IFeatureProvider featureProvider) {
        PictogramElement pictogramElement = (ContainerShape) containerShape;
        
        GraphicsAlgorithm outerGraphicsAlgorithm = containerShape.getGraphicsAlgorithm();
        if (outerGraphicsAlgorithm instanceof RoundedRectangle) {
            RoundedRectangle roundedRectangle = (RoundedRectangle) outerGraphicsAlgorithm;
            
            EList<Shape> children = containerShape.getChildren();
            int i = 0;
            for(Shape shape: children) {
                GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();
                
                // put text at right location; automatic layout would mess (scale) things up.
                if (graphicsAlgorithm instanceof Text && i == 0) {
                    Graphiti.getGaLayoutService().setLocationAndSize(graphicsAlgorithm, 0, 0, roundedRectangle.getWidth(), 20);
                    i++;
                } else if (graphicsAlgorithm instanceof Text && i == 1) {
                    Graphiti.getGaLayoutService().setLocationAndSize(graphicsAlgorithm, 0, 20, roundedRectangle.getWidth(), 20);
                    i++;
                }
            }
            //boolean input = (Graphiti.getPeService().getPropertyValue(containerShape, Constants.PROP_PAD_INPUT_KEY).equals(Constants.PROP_TRUE_VALUE));
            NonEmfDomainObject bo = POJOIndependenceSolver.getInstance().findPOJOObjectByPictureElement(pictogramElement, featureProvider);
            boolean input = true;
            if(bo instanceof ArroPad) {
                input = ((ArroPad) bo).getInput();
            }

            if (pictogramElement instanceof AnchorContainer) {
                AnchorContainer anchorList = (AnchorContainer) pictogramElement;
                EList<Anchor>anchors = anchorList.getAnchors();
                for(Anchor anchor: anchors) {
                    Logger.out.trace(Logger.EDITOR, anchor.toString());
                    if(anchor instanceof BoxRelativeAnchor) {
                        BoxRelativeAnchor boxAnchor = (BoxRelativeAnchor)anchor;
                        boxAnchor.setRelativeWidth(input? 1.0: 0.0);
                        
                        GraphicsAlgorithm graphicsAlgorithm = boxAnchor.getGraphicsAlgorithm();
                        IGaService gaService = Graphiti.getGaService();
                        gaService.setLocationAndSize(graphicsAlgorithm, input? - (Constants.PAD_SIZE + 4) : 4, 0, Constants.PAD_SIZE, Constants.PAD_SIZE);                
                    }
                }
            }
            
            return true;
        }
        return false;        
    }
    
}
