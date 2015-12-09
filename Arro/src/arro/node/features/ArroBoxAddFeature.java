package arro.node.features;

import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;

import arro.Constants;
import arro.domain.ArroNode;
import arro.domain.DomainNodeDiagram;


public class ArroBoxAddFeature extends AbstractAddFeature implements
		IAddFeature {

	public ArroBoxAddFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canAdd(IAddContext context) {
		// TODO: check for right domain object instance below
		return /* context.getNewObject() instanceof DomainObject && */ context.getTargetContainer() instanceof Diagram;
	}

	public PictogramElement add(IAddContext context) {
		
        int width = 500;
        int height = 300;
        
        int middleOfDiagramX = 100;
        int middleOfDiagramY = 100;

        Diagram targetDiagram = (Diagram) context.getTargetContainer();
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		ContainerShape containerShape = peCreateService.createContainerShape(targetDiagram, true);
		
        Graphiti.getPeService().setPropertyValue(containerShape, Constants.PROP_PICT_KEY, Constants.PROP_PICT_BOX);

		RoundedRectangle roundedRectangle = gaService.createRoundedRectangle(containerShape, 25, 25);
		{
			gaService.setLocationAndSize(roundedRectangle, middleOfDiagramX, middleOfDiagramY, width, height);
			roundedRectangle.setFilled(false);
			
	        roundedRectangle.setForeground(manageColor(Constants.CLASS_FOREGROUND));
	        roundedRectangle.setBackground(manageColor(Constants.CLASS_BACKGROUND));
	        roundedRectangle.setLineWidth(2);
		}
		
//		Shape shape = peCreateService.createShape(containerShape, false);
//		{
//			Text text = gaService.createText(shape, "Device");
//			text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
//			text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
//			gaService.setLocationAndSize(text, middleOfDiagramX, middleOfDiagramY, width, height);
//		}
		
	    // Now link PE (containerShape) to domain object and register diagram in POJOIndependencySolver
		link(containerShape, context.getNewObject());
		//POJOIndependenceSolver.getInstance().RegisterPOJOObject(addedDomainObject);
		
		return containerShape;
	}
}
