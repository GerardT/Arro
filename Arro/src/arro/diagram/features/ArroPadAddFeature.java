package arro.diagram.features;

import org.eclipse.core.resources.IFile;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICustomUndoRedoFeature;
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

import arro.Constants;
import arro.domain.ArroModule;
import arro.domain.ArroPad;
import arro.editors.FunctionDiagramEditor;
import util.Logger;


public class ArroPadAddFeature extends AbstractAddFeature implements IAddFeature, ICustomUndoRedoFeature {

	public ArroPadAddFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
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
	@Override
    public PictogramElement add(IAddContext context) {
		
		// Can't make it a object attribute since this code is called from different
		// contexts (so different object instances)!

        String instanceName = "aPad";
        String className = "";
        

        ArroModule domainModule;
        boolean isInput = true;
        int docType;
        Object obj = context.getNewObject();
        if(obj instanceof IFile) {
        	// Since we don't invoke the Create feature when dragging a 'file' on the diagram,
        	// no object had been created yet. So we do it here.
            IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
            if(!(dc instanceof FunctionDiagramEditor)) {
                Logger.out.trace(Logger.EDITOR, "not an editor");
                return null;
            }
            docType = ((FunctionDiagramEditor)dc).getDocumentType();
            domainModule =  ((FunctionDiagramEditor)dc).getDomainModule();
            ArroPad newPad = new ArroPad();
            
            // Set 'class' to the name of the file. During refresh the system will
            // obtain contents from file, such as IO pads.
            className = ((IFile) obj).getName();
            int index = className.indexOf("." + Constants.MESSAGE_EXT);
            if(index < 0) {
            	return null;
            }
            
            className = className.substring(0, index);
            instanceName = "a" + className;
            while(domainModule.getPadByName(instanceName) != null) {
            	instanceName += "1";
            }
            obj = newPad;
        } else {
            domainModule = (ArroModule) context.getProperty(Constants.PROP_CONTEXT_MOD_KEY);
            docType = Constants.CodeBlockHtml;
            instanceName = (String) context.getProperty(Constants.PROP_CONTEXT_NAME_KEY);
            className = "Json";
            if(instanceName.equals("input")) {
                isInput = true;
            } else {
                isInput = false;
            }

        }
        if(!(obj instanceof ArroPad)) {
        	return null;
        }
        ArroPad addedDomainObject = (ArroPad)obj;
        
        addedDomainObject.setType(className);
        addedDomainObject.setName(instanceName);

        ContainerShape containerShape = new ArroPadHelper(getDiagram()).create(context, addedDomainObject, docType, isInput);

        // To set location and size.
	    LayoutContext layoutContext = new LayoutContext(containerShape);
	    ILayoutFeature layoutFeature = getFeatureProvider().getLayoutFeature(layoutContext);
	    layoutFeature.layout(layoutContext);

		
        context.putProperty(Constants.PROP_UNDO_PAD_KEY, domainModule.clonePadList());
        context.putProperty(Constants.PROP_DOMAIN_MODULE_KEY, domainModule);

        domainModule.addPad(addedDomainObject);
        
	    // Now link PE (containerShape) to domain object and register diagram in POJOIndependencySolver
		link(containerShape, addedDomainObject);
		
		return containerShape;
	}

	@Override
	public void preUndo(IContext context) {
		ArroModule domainModule = (ArroModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
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
	public void preRedo(IContext context) {
		ArroModule domainModule = (ArroModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "redo " + context.getProperty(Constants.PROP_UNDO_PAD_KEY));
        context.putProperty(Constants.PROP_UNDO_PAD_KEY, domainModule.clonePadList());
		Object redoList = context.getProperty(Constants.PROP_REDO_PAD_KEY);
		domainModule.setPadList(redoList);
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


