package arro.diagram.types;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IMoveAnchorFeature;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.IRemoveFeature;
import org.eclipse.graphiti.features.IResizeShapeFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.context.IMoveAnchorContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IRemoveContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.features.DefaultFeatureProvider;

import arro.Constants;
import arro.diagram.features.ArroIDAddFeature;
import arro.diagram.features.NullRemoveFeature;
import arro.diagram.features.ReadyAddFeature;
import arro.diagram.features.StepAddFeature;
import arro.diagram.features.StepCreateFeature;
import arro.diagram.features.StepDeleteFeature;
import arro.diagram.features.StepLayoutFeature;
import arro.diagram.features.StepUpdateFeature;
import arro.domain.ArroModule;
import arro.domain.ArroSequenceChart;
import arro.domain.ArroStep;
import arro.domain.POJOIndependenceSolver;
import util.Logger;


public class SfcLeafFeatureProvider extends DefaultFeatureProvider {
	
	public SfcLeafFeatureProvider(IDiagramTypeProvider dtp) {
		super(dtp);
		
		setIndependenceSolver(POJOIndependenceSolver.getInstance());
		
	}
	public Diagram getDiagram() {
		return this.getDiagramTypeProvider().getDiagram();
	}

	@Override
	public ICreateFeature[] getCreateFeatures() {
		return new ICreateFeature[] {new StepCreateFeature(this)};
	}
	
	@Override
	public ICreateConnectionFeature[] getCreateConnectionFeatures() {
		return new ICreateConnectionFeature[] {};
	}
	
	@Override
	public IAddFeature getAddFeature(IAddContext context) {
		Logger.out.trace(Logger.EDITOR, " ");
		if (context instanceof IAddContext && 
				(context.getNewObject() instanceof ArroModule || context.getNewObject() instanceof ArroSequenceChart)) {
			return new ArroIDAddFeature(this);
        } else if (context instanceof IAddContext && context.getNewObject() instanceof ArroStep &&
                context.getProperty(Constants.PROP_CONTEXT_NAME_KEY) != null) {
            return new ReadyAddFeature(this);
        } else if (context instanceof IAddContext && context.getNewObject() instanceof ArroStep) {
            return new StepAddFeature(this);
		}

		return super.getAddFeature(context);
	}
	

	public IRemoveFeature getRemoveFeature(IRemoveContext context) {
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			return new NullRemoveFeature(this);				
		}
		return super.getRemoveFeature(context);

	}
	
	@Override
	public IDeleteFeature getDeleteFeature(IDeleteContext context) {
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			ContainerShape cs = (ContainerShape)pictogramElement;
			
			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_STEP)) {
				return  new StepDeleteFeature(this);				
			} 
		}
		
		return super.getDeleteFeature(context);
	}
	
	@Override
	public ILayoutFeature getLayoutFeature(ILayoutContext context) {
		
		// Note Layout is not supported for connections (Transition).

		if(context.getPictogramElement() instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			ContainerShape cs = (ContainerShape)context.getPictogramElement();
			
			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_STEP)) {
				return new StepLayoutFeature(this);				
			}
		}
	
		return super.getLayoutFeature(context);
	}
	
	@Override
	public IUpdateFeature getUpdateFeature(IUpdateContext context) {
		if (context.getPictogramElement() instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			ContainerShape cs = (ContainerShape)context.getPictogramElement();

			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_STEP)) {
				return  new StepUpdateFeature(this);				
			}
		}
		
		return super.getUpdateFeature(context);
	}
	
	public IMoveAnchorFeature getMoveAnchorFeature(IMoveAnchorContext context) {
		return null;
	}
	
	@Override
	public IResizeShapeFeature getResizeShapeFeature(IResizeShapeContext context) {
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			ContainerShape cs = (ContainerShape)pictogramElement;
			
			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_PASSIVE)) {
				return  null;				
			}
		}
		return super.getResizeShapeFeature(context);
	}
	@Override
	public IMoveShapeFeature getMoveShapeFeature(IMoveShapeContext context) {
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			ContainerShape cs = (ContainerShape)pictogramElement;
			
			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_PASSIVE)) {
				return  null;				
			}
		}
		return super.getMoveShapeFeature(context);
	}

			
}
