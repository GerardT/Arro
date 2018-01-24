package arro.diagram.features;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICustomUndoRedoFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.jface.dialogs.ErrorDialog;

import arro.Constants;
import arro.domain.ArroModule;
import arro.domain.ArroNode;
import arro.editors.FunctionDiagramEditor;
import util.Logger;


public class NodeAddFeature extends AbstractAddFeature implements IAddFeature, ICustomUndoRedoFeature {
	
	public NodeAddFeature(IFeatureProvider fp) {
		super(fp);

	}

	@Override
    public boolean canAdd(IAddContext context) {
		// TODO: check for right domain object instance below
		return (context.getNewObject() instanceof ArroNode && context.getTargetContainer() instanceof Diagram) ||
		       (context.getNewObject() instanceof IFile && context.getTargetContainer() instanceof Diagram);
	}

	/**
	 * Called when a Node is added to the diagram, both for adding and 
	 * drag and drop (DND).
	 */
	@Override
    public PictogramElement add(IAddContext context) {
		
		// Can't make it a object attribute since this code is called from different
		// contexts (so different object instances)!
        
        
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof FunctionDiagramEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        	return null;
        }
        ArroModule domainModule =  ((FunctionDiagramEditor)dc).getDomainModule();
        

        int docType = ((FunctionDiagramEditor)dc).getDocumentType();
        if(docType == Constants.CodeBlockPython || docType == Constants.CodeBlockNative) {
        	// cannot add nodes in elemBlock diagram.

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
            while(domainModule.findNodeByName(instanceName) != null) {
            	instanceName += "1";
            }
            obj = newClass;
        }
        if(!(obj instanceof ArroNode)) {
        	return null;
        }
        ArroNode addedDomainObject = (ArroNode)obj;
        
        addedDomainObject.setName(instanceName);

        // If getDiagram() does not work, then use below
        // Diagram targetDiagram = (Diagram) context.getTargetContainer();

        ContainerShape containerShape = new NodeHelper(getDiagram()).create(context, addedDomainObject);

        context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainModule.cloneNodeList());
        context.putProperty(Constants.PROP_DOMAIN_MODULE_KEY, domainModule);

        domainModule.addNode(addedDomainObject);
        
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
	public void preUndo(IContext context) {
		ArroModule domainModule = (ArroModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "undo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_REDO_NODE_KEY, domainModule.cloneNodeList());
		Object undoList = context.getProperty(Constants.PROP_UNDO_NODE_KEY);
		domainModule.setNodeList(undoList);
	}

	@Override
	public boolean canRedo(IContext context) {
		return true;
	}

	@Override
	public void preRedo(IContext context) {
		ArroModule domainModule = (ArroModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "redo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainModule.cloneNodeList());
		Object redoList = context.getProperty(Constants.PROP_REDO_NODE_KEY);
		domainModule.setNodeList(redoList);
	}

	@Override
	public void postUndo(IContext context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postRedo(IContext context) {
		// TODO Auto-generated method stub
		
	}
}

