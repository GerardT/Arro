package arro.diagram.features;

import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import arro.domain.ArroSequenceChart;
import arro.domain.ArroModule;

/**
 * One feature for both Function and State diagrams.
 *
 */
public class ArroIDAddFeature extends AbstractAddFeature implements IAddFeature {

	public ArroIDAddFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canAdd(IAddContext context) {
		return context.getTargetContainer() instanceof Diagram;
	}

	/**
	 * Used to register a business object ID to the diagram.
	 */
	public PictogramElement add(IAddContext context) {
		Object d = context.getNewObject();
		
		if(d instanceof ArroModule || d instanceof ArroSequenceChart) {
		    // Now link PE (containerShape) to domain object and register diagram in POJOIndependencySolver
			link(getDiagram(), d);
		}

		return getDiagram();
	}
}


