package arro.diagram.features;

import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.impl.AbstractCreateFeature;
import org.eclipse.graphiti.mm.pictograms.Diagram;

import arro.domain.ArroStep;

public class StepCreateFeature extends AbstractCreateFeature implements
		ICreateFeature {

	public StepCreateFeature(IFeatureProvider fp) {
		super(fp, "Step", "Creates a new Step");
	}

	public boolean canCreate(ICreateContext context) {
		return context.getTargetContainer() instanceof Diagram;
	}

	public Object[] create(ICreateContext context) {
	    // Create a model element and add it to the resource of the diagram.
		ArroStep newClass = new ArroStep();

		addGraphicalRepresentation(context, newClass);
		return new Object[] { newClass };
	}
}

