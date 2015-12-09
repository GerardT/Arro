package arro.node.features;

import org.eclipse.graphiti.features.ICustomUndoableFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.ui.features.DefaultDeleteFeature;

import util.Logger;
import arro.Constants;
import arro.domain.ArroConnection;
import arro.domain.DomainNodeDiagram;
import arro.editors.SubGraphitiEditor;

public class ArroConnectionDeleteFeature extends DefaultDeleteFeature implements ICustomUndoableFeature {

	public ArroConnectionDeleteFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	public void delete(IDeleteContext context) {
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof SubGraphitiEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        } else {
        	DomainNodeDiagram domainNodeDiagram =  ((SubGraphitiEditor)dc).getDomainNodeDiagram();
	        context.putProperty(Constants.PROP_UNDO_CONNECTION_KEY, domainNodeDiagram.cloneConnectionList());
	        context.putProperty(Constants.PROP_DOMAIN_NODE_KEY, domainNodeDiagram);
        }
		super.delete(context);
	}


	@Override
	protected void deleteBusinessObjects(Object[] businessObjects) {
		super.deleteBusinessObjects(businessObjects);
	}

	@Override
	protected void deleteBusinessObject(Object bo) {
		if(bo instanceof ArroConnection) {
			ArroConnection obj = (ArroConnection)bo;
			
			obj.getParent().removeConnection(obj);
			
			//POJOIndependenceSolver.getInstance().removeBusinessObject(bo);
		}
	}

	@Override
	public void undo(IContext context) {
		DomainNodeDiagram domainNodeDiagram = (DomainNodeDiagram) context.getProperty(Constants.PROP_DOMAIN_NODE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "undo " + context.getProperty(Constants.PROP_UNDO_CONNECTION_KEY));
        context.putProperty(Constants.PROP_REDO_CONNECTION_KEY, domainNodeDiagram.cloneConnectionList());
		Object undoList = context.getProperty(Constants.PROP_UNDO_CONNECTION_KEY);
		domainNodeDiagram.setConnectionList(undoList);
	}

	@Override
	public boolean canRedo(IContext context) {
		return true;
	}

	@Override
	public void redo(IContext context) {
		DomainNodeDiagram domainNodeDiagram = (DomainNodeDiagram) context.getProperty(Constants.PROP_DOMAIN_NODE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "redo " + context.getProperty(Constants.PROP_UNDO_CONNECTION_KEY));
        context.putProperty(Constants.PROP_UNDO_CONNECTION_KEY, domainNodeDiagram.cloneConnectionList());
		Object redoList = context.getProperty(Constants.PROP_REDO_CONNECTION_KEY);
		domainNodeDiagram.setConnectionList(redoList);
	}
}
