package arro.diagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IRemoveContext;
import org.eclipse.graphiti.features.impl.DefaultRemoveFeature;

import util.Logger;

public class NullRemoveFeature extends DefaultRemoveFeature {

	public NullRemoveFeature(IFeatureProvider fp) {
		super(fp);

	}
	
	/**
	 * We need to implement like this. For deletion, super.delete(cs)
	 * will call remove(cs) in order to remove PE off the diagram, without
	 * checking canRemove(cs) first.
	 */
	@Override
    public boolean canRemove(IRemoveContext context) {
		return false;
	}
	
	
	@Override
    public void remove(IRemoveContext context) {
		super.remove(context);
		Logger.out.trace(Logger.EDITOR, "trying to remove ...");
	}

}
