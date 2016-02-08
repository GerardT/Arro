package arro.diagram.features;

import org.eclipse.graphiti.features.ICustomUndoableFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.ui.features.DefaultDeleteFeature;

import util.Logger;
import arro.Constants;
import arro.domain.ArroNode;
import arro.domain.ArroState;
import arro.domain.ArroStateDiagram;
import arro.domain.DomainModule;
import arro.editors.FunctionDiagramEditor;
import arro.editors.StateDiagramEditor;

public class StateBlockDeleteFeature extends DefaultDeleteFeature implements ICustomUndoableFeature {

	public StateBlockDeleteFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	public boolean canDelete(IDeleteContext context) {
		return true;
	}
	
	public void delete(IDeleteContext context) {
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof StateDiagramEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        } else {
        	DomainModule domainModule =  ((StateDiagramEditor)dc).getDomainModule();
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
		if(bo instanceof ArroNode) {
			ArroState obj = (ArroState)bo;
			
			obj.getParent().removeState(obj);
			
//			obj.getParent().purgeConnections();
		}
	}

	@Override
	public void undo(IContext context) {
		DomainModule domainModule = (DomainModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "undo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_REDO_NODE_KEY, domainModule.getStateDiagram().clone());
		Object undo = context.getProperty(Constants.PROP_UNDO_NODE_KEY);
		domainModule.setStateDiagram((ArroStateDiagram) undo);
		
//        context.putProperty(Constants.PROP_REDO_CONNECTION_KEY, domainModule.cloneConnectionList());
//		undoList = context.getProperty(Constants.PROP_UNDO_CONNECTION_KEY);
//		domainModule.setConnectionList(undoList);
	}

	@Override
	public boolean canRedo(IContext context) {
		return true;
	}

	@Override
	public void redo(IContext context) {
		DomainModule domainModule = (DomainModule) context.getProperty(Constants.PROP_DOMAIN_MODULE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "redo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainModule.getStateDiagram().clone());
		Object redo = context.getProperty(Constants.PROP_REDO_NODE_KEY);
		domainModule.setStateDiagram((ArroStateDiagram) redo);
		
//        context.putProperty(Constants.PROP_UNDO_CONNECTION_KEY, domainModule.cloneConnectionList());
//		redoList = context.getProperty(Constants.PROP_REDO_CONNECTION_KEY);
//		domainModule.setConnectionList(redoList);
	}
}
