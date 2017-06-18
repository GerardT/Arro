package arro.diagram.features;

import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import arro.Constants;
import arro.domain.ArroStep;

// Ready steps are standard steps _ready and _terminated.
public class ReadyAddFeature extends AbstractAddFeature implements
		IAddFeature {

	public ReadyAddFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
    public boolean canAdd(IAddContext context) {
		// TODO: check for right domain object instance below
		return /* context.getNewObject() instanceof DomainObject && */ context.getTargetContainer() instanceof Diagram;
	}

	@Override
    public PictogramElement add(IAddContext context) {
		
        Object obj = context.getNewObject();
        
        if(!(obj instanceof ArroStep)) {
            return null;
        }
        
        ArroStep addedDomainObject = (ArroStep)obj;
        
        if(context.getProperty(Constants.PROP_CONTEXT_NAME_KEY) == null) {
            return null;
        }
                
        String name = (String) context.getProperty(Constants.PROP_CONTEXT_NAME_KEY);
        
        addedDomainObject.setName(name);

        ContainerShape containerShape = new StepHelper(getDiagram()).create(context, addedDomainObject);

        // Now link PE (containerShape) to domain object and register diagram in POJOIndependencySolver
        link(containerShape, addedDomainObject);
		
		return containerShape;
	}
}
