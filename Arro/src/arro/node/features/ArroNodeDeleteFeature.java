package arro.node.features;

import org.eclipse.graphiti.features.ICustomUndoableFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.ui.features.DefaultDeleteFeature;

import util.Logger;
import arro.Constants;
import arro.domain.ArroNode;
import arro.domain.DomainNodeDiagram;
import arro.editors.SubGraphitiEditor;

public class ArroNodeDeleteFeature extends DefaultDeleteFeature implements ICustomUndoableFeature {

	public ArroNodeDeleteFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	public boolean canDelete(IDeleteContext context) {
		return true;
	}
	
	public void delete(IDeleteContext context) {
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof SubGraphitiEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        } else {
        	DomainNodeDiagram domainNodeDiagram =  ((SubGraphitiEditor)dc).getDomainNodeDiagram();
	        context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainNodeDiagram.cloneNodeList());
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
		if(bo instanceof ArroNode) {
			ArroNode obj = (ArroNode)bo;
			
			obj.getParent().removeSubNode(obj);
			obj.getParent().purgeConnections();
			
			//POJOIndependenceSolver.getInstance().removeBusinessObject(bo);
		}
	}

	@Override
	public void undo(IContext context) {
		DomainNodeDiagram domainNodeDiagram = (DomainNodeDiagram) context.getProperty(Constants.PROP_DOMAIN_NODE_KEY);
		
		Logger.out.trace(Logger.EDITOR, "undo " + context.getProperty(Constants.PROP_UNDO_NODE_KEY));
        context.putProperty(Constants.PROP_REDO_NODE_KEY, domainNodeDiagram.cloneNodeList());
		Object undoList = context.getProperty(Constants.PROP_UNDO_NODE_KEY);
		domainNodeDiagram.setNodeList(undoList);
		
        context.putProperty(Constants.PROP_REDO_CONNECTION_KEY, domainNodeDiagram.cloneConnectionList());
		undoList = context.getProperty(Constants.PROP_UNDO_CONNECTION_KEY);
		domainNodeDiagram.setConnectionList(undoList);
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
		
        context.putProperty(Constants.PROP_UNDO_CONNECTION_KEY, domainNodeDiagram.cloneConnectionList());
		redoList = context.getProperty(Constants.PROP_REDO_CONNECTION_KEY);
		domainNodeDiagram.setConnectionList(redoList);
	}
}
