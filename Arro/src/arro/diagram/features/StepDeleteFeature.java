package arro.diagram.features;

import org.eclipse.graphiti.features.ICustomUndoRedoFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.ui.features.DefaultDeleteFeature;

import arro.Constants;
import arro.domain.ArroModule;
import arro.domain.ArroNode;
import arro.domain.ArroStep;
import arro.domain.ArroSequenceChart;
import arro.editors.StateDiagramEditor;
import util.Logger;

public class StepDeleteFeature extends DefaultDeleteFeature implements ICustomUndoRedoFeature {

	public StepDeleteFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	public boolean canDelete(IDeleteContext context) {
	    PictogramElement pe = context.getPictogramElement();
        if (pe != null) {
            Object[] eObject = getAllBusinessObjectsForPictogramElement(pe);

            if (eObject.length != 0 && eObject[0] instanceof ArroStep) {
                ArroStep step = (ArroStep) eObject[0];
                if(step.getName().equals("_ready")|| step.getName().equals("_terminated")) {
                    return false;
                }
            }
        }
   
	    
		return true;
	}
	
	public void delete(IDeleteContext context) {
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof StateDiagramEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        } else {
        	ArroModule domainModule =  ((StateDiagramEditor)dc).getDomainModule();
	        context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainModule.getStateDiagram().clone());
	        //context.putProperty(Constants.PROP_UNDO_CONNECTION_KEY, domainModule.cloneConnectionList());
	        context.putProperty(Constants.PROP_DOMAIN_MODULE_KEY, domainModule);
        }
		super.delete(context);
	}


	@Override
	protected void deleteBusinessObjects(Object[] businessObjects) {
		super.deleteBusinessObjects(businessObjects);
	}

	@Override
	protected void deleteBusinessObject(Object bo) {
		if(bo instanceof ArroStep) {
			ArroStep obj = (ArroStep)bo;
			
			obj.getParent().removeStep(obj);
			
//			obj.getParent().purgeConnections();
		}
	}

	@Override
	public void preUndo(IContext context) {
		ArroModule domainModule = (ArroModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "undo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_REDO_NODE_KEY, domainModule.getStateDiagram().clone());
		Object undo = context.getProperty(Constants.PROP_UNDO_NODE_KEY);
		domainModule.setStateDiagram((ArroSequenceChart) undo);
		
//        context.putProperty(Constants.PROP_REDO_CONNECTION_KEY, domainModule.cloneConnectionList());
//		undoList = context.getProperty(Constants.PROP_UNDO_CONNECTION_KEY);
//		domainModule.setConnectionList(undoList);
	}

	@Override
	public boolean canRedo(IContext context) {
		return true;
	}

	@Override
	public void preRedo(IContext context) {
		ArroModule domainModule = (ArroModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "redo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainModule.getStateDiagram().clone());
		Object redo = context.getProperty(Constants.PROP_REDO_NODE_KEY);
		domainModule.setStateDiagram((ArroSequenceChart) redo);
		
//        context.putProperty(Constants.PROP_UNDO_CONNECTION_KEY, domainModule.cloneConnectionList());
//		redoList = context.getProperty(Constants.PROP_REDO_CONNECTION_KEY);
//		domainModule.setConnectionList(redoList);
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
