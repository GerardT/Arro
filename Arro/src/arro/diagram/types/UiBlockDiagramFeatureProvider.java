package arro.diagram.types;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.EList;
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
import org.eclipse.graphiti.mm.Property;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.features.DefaultFeatureProvider;

import arro.Constants;
import arro.diagram.features.ArroBoxAddFeature;
import arro.diagram.features.ArroIDAddFeature;
import arro.diagram.features.ArroPadAddFeature;
import arro.diagram.features.ArroPadLayoutFeature;
import arro.diagram.features.ArroPadUpdateFeature;
import arro.diagram.features.NullRemoveFeature;
import arro.domain.ArroDevice;
import arro.domain.ArroModule;
import arro.domain.ArroPad;
import arro.domain.POJOIndependenceSolver;
import util.Logger;


public class UiBlockDiagramFeatureProvider extends DefaultFeatureProvider {
	
	public UiBlockDiagramFeatureProvider(IDiagramTypeProvider dtp) {
		super(dtp);
		
		setIndependenceSolver(POJOIndependenceSolver.getInstance());
		
	}
	public Diagram getDiagram() {
		return this.getDiagramTypeProvider().getDiagram();
	}

	@Override
	public ICreateFeature[] getCreateFeatures() {
        // Nodes are only 'created' by dragging into diagram.
        return new ICreateFeature[] {};
	}
	
	@Override
	public ICreateConnectionFeature[] getCreateConnectionFeatures() {
		return new ICreateConnectionFeature[] {};
	}
	
	@Override
	public IAddFeature getAddFeature(IAddContext context) {
		Logger.out.trace(Logger.EDITOR, " ");
		if (context instanceof IAddContext && context.getNewObject() instanceof ArroPad) {
			return new ArroPadAddFeature(this);
		} else if (context instanceof IAddContext && context.getNewObject() instanceof ArroModule) {
            return new ArroIDAddFeature(this);
        } else if (context instanceof IAddContext && context.getNewObject() instanceof ArroDevice) {
            return new ArroBoxAddFeature(this);
        } else if (context instanceof IAddContext && context.getNewObject() instanceof IFile) {
            IFile file = (IFile)context.getNewObject();
            if(file.getName().endsWith(".amsg")) {
                // Add a Node even if a diagram was added...
                return new ArroPadAddFeature(this);
            }
        }


		return super.getAddFeature(context);
	}
	

	@Override
    public IRemoveFeature getRemoveFeature(IRemoveContext context) {
        return new NullRemoveFeature(this);             
	}
	
	@Override
	public IDeleteFeature getDeleteFeature(IDeleteContext context) {
        return null;
	}

	
	@Override
	public ILayoutFeature getLayoutFeature(ILayoutContext context) {
		if(context.getPictogramElement() instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			ContainerShape cs = (ContainerShape)context.getPictogramElement();

			EList<Property> list = cs.getProperties();
			for(Property kv : list) {
			    if(kv.getKey().equals(Constants.PROP_PICT_KEY)) {
                    return  new ArroPadLayoutFeature(this);             
			    }
			    
			}
// likely bug in Graphiti...
//			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
//			
//			if((pict != null && pict.equals(Constants.PROP_PICT_PAD))) {
//				return  new ArroPadLayoutFeature(this);				
//			} 
		}
	
		return super.getLayoutFeature(context);
	}
	
	@Override
	public IUpdateFeature getUpdateFeature(IUpdateContext context) {
		if (context.getPictogramElement() instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			ContainerShape cs = (ContainerShape)context.getPictogramElement();

			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_PAD)) {
				return  new ArroPadUpdateFeature(this);				
			}
		}
		
		return super.getUpdateFeature(context);
	}
	
	@Override
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
