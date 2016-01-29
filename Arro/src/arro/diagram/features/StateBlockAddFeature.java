package arro.diagram.features;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICustomUndoableFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.jface.dialogs.ErrorDialog;

import util.Logger;
import arro.Constants;
import arro.domain.ArroNode;
import arro.domain.DomainNodeDiagram;
import arro.editors.FunctionDiagramEditor;


public class StateBlockAddFeature extends AbstractAddFeature implements IAddFeature, ICustomUndoableFeature {
	
	public StateBlockAddFeature(IFeatureProvider fp) {
		super(fp);

	}

	public boolean canAdd(IAddContext context) {
		// TODO: check for right domain object instance below
		return (context.getNewObject() instanceof ArroNode && context.getTargetContainer() instanceof Diagram) ||
		       (context.getNewObject() instanceof IFile && context.getTargetContainer() instanceof Diagram);
	}

	/**
	 * Called when a Node is added to the diagram, both for adding and 
	 * drag and drop (DND).
	 */
	public PictogramElement add(IAddContext context) {
		
		// Can't make it a object attribute since this code is called from different
		// contexts (so different object instances)!
        int width = 200;
        int height = 50;
        
        
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof FunctionDiagramEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        	return null;
        }
        DomainNodeDiagram domainNodeDiagram =  ((FunctionDiagramEditor)dc).getDomainNodeDiagram();
        

        int docType = ((FunctionDiagramEditor)dc).getDocumentType();
        if(docType == Constants.CodeBlockPython || docType == Constants.CodeBlockNative) {
        	// cannot add nodes in device diagram.

        	IStatus status = new Status(IStatus.ERROR, "Arro", /*reason*/"Not allowed to drop Node into Device diagram");
			ErrorDialog.openError(null, "Adding node", "Cannot add node to diagram", status);

        	return null;
        }

        
        String instanceName = "";
        String className = "";
		Object obj = context.getNewObject();
        if(obj instanceof IFile) {
        	// Since we don't invoke the Create feature when dragging a 'file' on the diagram,
        	// no object had been created yet. So we do it here.
            ArroNode newClass = new ArroNode();
            
            // Set 'class' to the name of the file. During refresh the system will
            // obtain contents from file, such as IO pads.
            className = ((IFile) obj).getName();
            int index1 = className.indexOf("." + Constants.NODE_EXT);
            if(index1 > 0) {
                className = className.substring(0, index1);
            } else {
            	return null;
            }
            
            newClass.setType(className);
            instanceName = "a" + className;
            while(domainNodeDiagram.getSubNodeByName(instanceName) != null) {
            	instanceName += "1";
            }
            obj = newClass;
        }
        if(!(obj instanceof ArroNode)) {
        	return null;
        }
        ArroNode addedDomainObject = (ArroNode)obj;
        
        addedDomainObject.setName(instanceName);


		Diagram targetDiagram = (Diagram) context.getTargetContainer();
		IPeCreateService peCreateService = Graphiti.getPeCreateService();  // widget?
		IGaService gaService = Graphiti.getGaService(); // shape?
		

		/////// CONTAINER ///////
		ContainerShape containerShape = peCreateService.createContainerShape(targetDiagram, true);
		
        Graphiti.getPeService().setPropertyValue(containerShape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_NODE);

        // create invisible outer rectangle expanded by
        // the width needed for the anchor
        Rectangle invisibleRectangle = gaService.createInvisibleRectangle(containerShape);
        {
            gaService.setLocationAndSize(invisibleRectangle, context.getX(), context.getY(),
            												 width, height);
     
        }

		{
	        // Shape shape = peCreateService.createShape(containerShape, false);
	        
			RoundedRectangle roundedRectangle = gaService.createRoundedRectangle(invisibleRectangle, 5, 5);
			
			gaService.setLocationAndSize(roundedRectangle, Constants.HALF_PAD_SIZE, 0, width - Constants.PAD_SIZE, height);
			roundedRectangle.setFilled(false);
			
	        roundedRectangle.setForeground(manageColor(Constants.CLASS_FOREGROUND));
	        roundedRectangle.setBackground(manageColor(Constants.CLASS_BACKGROUND));
	        roundedRectangle.setLineWidth(2);
		}
		
		/////// horizontal divider line ///////
		{
	        // create and set graphics algorithm
	        Polyline polyline =
	            gaService.createPolyline(invisibleRectangle, new int[] { 0, 40, width, 40 });
	        polyline.setForeground(manageColor(Constants.CLASS_FOREGROUND));
	        polyline.setLineWidth(2);
		}
 
		/////// class text ///////
        {
    		Shape shape = peCreateService.createShape(containerShape, false);
			Text text = gaService.createText(shape, addedDomainObject.getType() + " :");
	        text.setForeground(manageColor(Constants.CLASS_TEXT_FOREGROUND));
			text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
			text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
       }

		/////// name text ///////
        {
			Shape shape = peCreateService.createShape(containerShape, false);
			Text text = gaService.createText(shape, addedDomainObject.getName());
	        text.setForeground(manageColor(Constants.CLASS_TEXT_FOREGROUND));
			text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
			text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
        }
        
		
        context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainNodeDiagram.cloneNodeList());
        context.putProperty(Constants.PROP_DOMAIN_NODE_KEY, domainNodeDiagram);

        domainNodeDiagram.addSubNode(addedDomainObject);
        
	    // Now link PE (containerShape) to domain object and register diagram in POJOIndependencySolver
		link(containerShape, addedDomainObject);		
		
		// After PE was linked to domain object..
		
		// Update for actual nr or pads.
        UpdateContext updateContext = new UpdateContext(containerShape);
        IUpdateFeature updateFeature = getFeatureProvider().getUpdateFeature(updateContext);
        updateFeature.update(updateContext);

        // To set location and size.
	    LayoutContext layoutContext = new LayoutContext(containerShape);
	    ILayoutFeature layoutFeature = getFeatureProvider().getLayoutFeature(layoutContext);
	    layoutFeature.layout(layoutContext);


		
		return containerShape;
	}

	@Override
	public void undo(IContext context) {
		DomainNodeDiagram domainNodeDiagram = (DomainNodeDiagram) context.getProperty(Constants.PROP_DOMAIN_NODE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "undo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_REDO_NODE_KEY, domainNodeDiagram.cloneNodeList());
		Object undoList = context.getProperty(Constants.PROP_UNDO_NODE_KEY);
		domainNodeDiagram.setNodeList(undoList);
	}

	@Override
	public boolean canRedo(IContext context) {
		return true;
	}

	@Override
	public void redo(IContext context) {
		DomainNodeDiagram domainNodeDiagram = (DomainNodeDiagram) context.getProperty(Constants.PROP_DOMAIN_NODE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "redo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainNodeDiagram.cloneNodeList());
		Object redoList = context.getProperty(Constants.PROP_REDO_NODE_KEY);
		domainNodeDiagram.setNodeList(redoList);
	}
}

