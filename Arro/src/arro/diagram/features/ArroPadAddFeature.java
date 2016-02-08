package arro.diagram.features;

import org.eclipse.core.resources.IFile;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICustomUndoableFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;

import util.Logger;
import arro.Constants;
import arro.domain.ArroPad;
import arro.domain.DomainModule;
import arro.editors.FunctionDiagramEditor;


public class ArroPadAddFeature extends AbstractAddFeature implements IAddFeature, ICustomUndoableFeature {

	public ArroPadAddFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canAdd(IAddContext context) {
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof FunctionDiagramEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        	return false;
        }
        int docType = ((FunctionDiagramEditor)dc).getDocumentType();
        if(docType == Constants.CodeBlockPython || docType == Constants.CodeBlockNative) {
    		// If target diagram is device diagram then only allow dropping inside the box.
    		return context.getNewObject() instanceof IFile && !(context.getTargetContainer() instanceof Diagram);
        } else {
    		// TODO: check for right domain object instance below
    		return (context.getNewObject() instanceof ArroPad && context.getTargetContainer() instanceof Diagram) ||
    		       (context.getNewObject() instanceof IFile && context.getTargetContainer() instanceof Diagram);
        }

	}

	/**
	 * Called when a Node is added to the diagram, both for adding and 
	 * drag and drop (DND).
	 */
	public PictogramElement add(IAddContext context) {
		
		// Can't make it a object attribute since this code is called from different
		// contexts (so different object instances)!

        int width = 120;
        int height = 42;
        
        String instanceName = "aPad";
        String className = "";
        
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof FunctionDiagramEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        	return null;
        }
        DomainModule domainModule =  ((FunctionDiagramEditor)dc).getDomainModule();

        Object obj = context.getNewObject();
        if(obj instanceof IFile) {
        	// Since we don't invoke the Create feature when dragging a 'file' on the diagram,
        	// no object had been created yet. So we do it here.
            ArroPad newPad = new ArroPad();
            
            // Set 'class' to the name of the file. During refresh the system will
            // obtain contents from file, such as IO pads.
            className = ((IFile) obj).getName();
            int index = className.indexOf("." + Constants.MESSAGE_EXT);
            if(index < 0) {
            	return null;
            }
            
            className = className.substring(0, index);
            newPad.setType(className);
            instanceName = "a" + className;
            while(domainModule.getPadByName(instanceName) != null) {
            	instanceName += "1";
            }
            obj = newPad;
        }
        if(!(obj instanceof ArroPad)) {
        	return null;
        }
        ArroPad addedDomainObject = (ArroPad)obj;
        
        addedDomainObject.setName(instanceName);


		ContainerShape targetDiagram = (ContainerShape) context.getTargetContainer();
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
			
	        roundedRectangle.setForeground(manageColor(Constants.PAD_FOREGROUND_INPUT));
	        roundedRectangle.setBackground(manageColor(Constants.PAD_BACKGROUND_INPUT));
	        roundedRectangle.setLineWidth(2);
		}
		
		/////// class text ///////
        {
			Shape shape = peCreateService.createShape(containerShape, false);
			Text text = gaService.createText(shape, addedDomainObject.getType() + " :");
	        text.setForeground(manageColor(Constants.PAD_TEXT_FOREGROUND));
			text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
			text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
        }

		/////// name text ///////
        {
			Shape shape = peCreateService.createShape(containerShape, false);
			Text text = gaService.createText(shape, addedDomainObject.getName());
	        text.setForeground(manageColor(Constants.PAD_TEXT_FOREGROUND));
			text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
			text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
        }
        
        ///////////////////////
// 		final BoxRelativeAnchor boxAnchor = peCreateService.createBoxRelativeAnchor(containerShape);                 		
//		boxAnchor.setReferencedGraphicsAlgorithm(containerShape.getGraphicsAlgorithm());
//		
//		
////		if(leftSide) {
////    		boxAnchor.setRelativeWidth(0.0);
////		} else {
////    		boxAnchor.setRelativeWidth(0.0);
////		}
//		boxAnchor.setRelativeHeight(0.0/*0.38*/); // Use golden section
//        
//        //Graphiti.getPeService().setPropertyValue(shape, Constants.PROP_PAD_NAME_KEY, padName);
//
//        RoundedRectangle anch = gaService.createRoundedRectangle(boxAnchor, 5, 5);
//        anch.setFilled(true);
//        anch.setForeground(manageColor(Constants.ANCHOR_FG));
//        anch.setBackground(manageColor(Constants.ANCHOR_BG));
//        anch.setLineWidth(2);
//        
        ///////////////////////

        int docType = ((FunctionDiagramEditor)dc).getDocumentType();
        if(docType != Constants.CodeBlockPython && docType != Constants.CodeBlockNative) {
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
            ellipse.setForeground(manageColor(Constants.ANCHOR_FG));
            ellipse.setBackground(manageColor(Constants.ANCHOR_BG));
            ellipse.setLineWidth(2);
            gaService.setLocationAndSize(ellipse, - Constants.PAD_SIZE, 0, Constants.PAD_SIZE, Constants.PAD_SIZE);
        }

        // To set location and size.
	    LayoutContext layoutContext = new LayoutContext(containerShape);
	    ILayoutFeature layoutFeature = getFeatureProvider().getLayoutFeature(layoutContext);
	    layoutFeature.layout(layoutContext);

		
        context.putProperty(Constants.PROP_UNDO_PAD_KEY, domainModule.clonePadList());
        context.putProperty(Constants.PROP_DOMAIN_MODULE_KEY, domainModule);

        domainModule.addPad(addedDomainObject);
        
	    // Now link PE (containerShape) to domain object and register diagram in POJOIndependencySolver
		link(containerShape, addedDomainObject);
		//POJOIndependenceSolver.getInstance().RegisterPOJOObject(addedDomainObject);
		
		return containerShape;
	}

	@Override
	public void undo(IContext context) {
		DomainModule domainModule = (DomainModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "undo " + context.getProperty(Constants.PROP_UNDO_PAD_KEY));
        context.putProperty(Constants.PROP_REDO_PAD_KEY, domainModule.clonePadList());
		Object undoList = context.getProperty(Constants.PROP_UNDO_PAD_KEY);
		domainModule.setPadList(undoList);
	}

	@Override
	public boolean canRedo(IContext context) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void redo(IContext context) {
		DomainModule domainModule = (DomainModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "redo " + context.getProperty(Constants.PROP_UNDO_PAD_KEY));
        context.putProperty(Constants.PROP_UNDO_PAD_KEY, domainModule.clonePadList());
		Object redoList = context.getProperty(Constants.PROP_REDO_PAD_KEY);
		domainModule.setPadList(redoList);
	}
}


