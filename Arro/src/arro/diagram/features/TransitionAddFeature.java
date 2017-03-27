package arro.diagram.features;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICustomUndoRedoFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.util.IColorConstant;

import arro.Constants;
import arro.domain.ArroModule;
import arro.domain.ArroTransition;
import arro.editors.StateDiagramEditor;
import util.Logger;


public class TransitionAddFeature extends AbstractAddFeature implements IAddFeature, ICustomUndoRedoFeature {
	
	public TransitionAddFeature(IFeatureProvider fp) {
		super(fp);

	}

	public boolean canAdd(IAddContext context) {
		// TODO: check for right domain object instance below
		return (context.getNewObject() instanceof ArroTransition);
	}

	/**
	 * Called when a Transition is added to the diagram.
	 */
	public PictogramElement add(IAddContext context) {
        
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof StateDiagramEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        	return null;
        }
        ArroModule domainModule =  ((StateDiagramEditor)dc).getDomainModule();
        
        // instead of this
  		Object obj = context.getNewObject();
		
        if(!(obj instanceof ArroTransition)) {
        	return null;
        }
        
        ArroTransition addedDomainObject = (ArroTransition)obj;
        // we could do this
        // ArroTransition addedDomainObject = new ArroTransition();
        
        // TODO not right to use names here, names may change..
        addedDomainObject.setSource((String) context.getProperty(Constants.PROP_SOURCE_PAD_KEY));
        addedDomainObject.setTarget((String) context.getProperty(Constants.PROP_TARGET_PAD_KEY));

        
        String instanceName = "a" + "Transition";
        while(domainModule.getStateDiagram().getTransitionByName(instanceName) != null) {
        	instanceName += "1";
        }
        addedDomainObject.setName(instanceName);

		Connection connection = new TransitionHelper(getDiagram()).create(context, addedDomainObject, getDiagram());

		context.putProperty(Constants.PROP_UNDO_NODE_KEY, domainModule.cloneNodeList());
        context.putProperty(Constants.PROP_DOMAIN_MODULE_KEY, domainModule);

        try {
            domainModule.getStateDiagram().addTransition(addedDomainObject);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
		link(connection, addedDomainObject);
		
		return connection;
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

