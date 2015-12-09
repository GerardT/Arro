package arro.node.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.ui.features.DefaultDeleteFeature;

import util.Logger;

public class ArroBoxDeleteFeature extends DefaultDeleteFeature {

	public ArroBoxDeleteFeature(IFeatureProvider fp) {
		super(fp);
	}
	public void delete(IDeleteContext context) {
		Logger.out.trace(Logger.EDITOR, "trying to remove box");
	}

}
