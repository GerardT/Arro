package arro.node.diagram;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.context.IMoveAnchorContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IRemoveContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.mm.pictograms.BoxRelativeAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.features.DefaultFeatureProvider;

import util.Logger;
import arro.Constants;
import arro.domain.ArroDevice;
import arro.domain.ArroNode;
import arro.domain.DomainNodeDiagram;
import arro.domain.POJOIndependenceSolver;
import arro.node.features.ArroBoxAddFeature;
import arro.node.features.ArroConnectionAddFeature;
import arro.node.features.ArroConnectionCreateFeature;
import arro.node.features.ArroConnectionDeleteFeature;
import arro.node.features.ArroIDAddFeature;
import arro.node.features.ArroNodeAddFeature;
import arro.node.features.ArroNodeCreateFeature;
import arro.node.features.ArroNodeDeleteFeature;
import arro.node.features.ArroNodeLayoutFeature;
import arro.node.features.ArroNodeUpdateFeature;
import arro.node.features.ArroPadAddFeature;
import arro.node.features.ArroPadDeleteFeature;
import arro.node.features.ArroPadLayoutFeature;
import arro.node.features.ArroPadUpdateFeature;
import arro.node.features.NullRemoveFeature;


public class ArroNodeFeatureProvider extends DefaultFeatureProvider {
	
	public ArroNodeFeatureProvider(IDiagramTypeProvider dtp) {
		super(dtp);
		
		POJOIndependenceSolver pojoIndependenceSolver = POJOIndependenceSolver.getInstance();
		pojoIndependenceSolver.setFeatureProvider(this);

		setIndependenceSolver(pojoIndependenceSolver);
		
	}
	public Diagram getDiagram() {
		return this.getDiagramTypeProvider().getDiagram();
	}

	@Override
	public ICreateFeature[] getCreateFeatures() {
		/* original
		return new ICreateFeature[] {new ArroNodeCreateFeature(this)};
		*/
		ICreateFeature[] features = new ICreateFeature[1];
		
		// Add 1 Create...features 1 right side of screen.
		// FIXME: remove this
		features[0] = new ArroNodeCreateFeature(this);
		return features;
	}
	
	@Override
	public ICreateConnectionFeature[] getCreateConnectionFeatures() {
		return new ICreateConnectionFeature[] {new ArroConnectionCreateFeature(this)};
	}
	
	@Override
	public IAddFeature getAddFeature(IAddContext context) {
		Logger.out.trace(Logger.EDITOR, " ");
		if (context instanceof IAddConnectionContext /* && context.getNewObject() instanceof <DomainObject> */) {
			return new ArroConnectionAddFeature(this);
		} else if (context instanceof IAddContext && context.getNewObject() instanceof ArroNode) {
			return new ArroNodeAddFeature(this);
		} else if (context instanceof IAddContext && context.getNewObject() instanceof DomainNodeDiagram) {
			return new ArroIDAddFeature(this);
		} else if (context instanceof IAddContext && context.getNewObject() instanceof ArroDevice) {
			return new ArroBoxAddFeature(this);
		} else if (context instanceof IAddContext && context.getNewObject() instanceof IFile) {
			IFile file = (IFile)context.getNewObject();
			if(file.getName().endsWith("." + Constants.NODE_EXT)) {
				// Add a Node even if a diagram was added...
				return new ArroNodeAddFeature(this);
			} else if(file.getName().endsWith("." + Constants.DEVICE_EXT)) {
				// Add a Node even if a diagram was added...
				return new ArroNodeAddFeature(this);
			} else if(file.getName().endsWith(".amsg")) {
				// Add a Node even if a diagram was added...
				return new ArroPadAddFeature(this);
			}
		}

		return super.getAddFeature(context);
	}
	

	public IRemoveFeature getRemoveFeature(IRemoveContext context) {
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			ContainerShape cs = (ContainerShape)pictogramElement;
			
			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			return new NullRemoveFeature(this);				
		} else if(pictogramElement instanceof BoxRelativeAnchor) {
			return null;
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
			
			if(pict != null && pict.equals(Constants.PROP_PICT_NODE)) {
				return  new ArroNodeDeleteFeature(this);				
			} else if(pict != null && pict.equals(Constants.PROP_PICT_PAD)) {
				return  new ArroPadDeleteFeature(this);				
			} else if(pict != null && pict.equals(Constants.PROP_PICT_CONNECTION)) {
				return  new ArroConnectionDeleteFeature(this);				
			} else if(pict != null && pict.equals(Constants.PROP_PICT_BOX)) {
				return  null; //new ArroBoxDeleteFeature(this);				
			} else if(pict != null && pict.equals(Constants.PROP_PICT_PASSIVE)) {
				return  null; //new ArroBoxDeleteFeature(this);				
			} 
		} else if(pictogramElement instanceof FreeFormConnection) {
			FreeFormConnection ffc = (FreeFormConnection)pictogramElement;
			
			String pict = Graphiti.getPeService().getPropertyValue(ffc, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_CONNECTION)) {
				return  new ArroConnectionDeleteFeature(this);				
			} 
		} else if(pictogramElement instanceof BoxRelativeAnchor) {
			return null;
		}
		
		return super.getDeleteFeature(context);
	}

	
	@Override
	public ILayoutFeature getLayoutFeature(ILayoutContext context) {
		if(context.getPictogramElement() instanceof ContainerShape) {
			Logger.out.trace(Logger.EDITOR, " ");
			ContainerShape cs = (ContainerShape)context.getPictogramElement();
			
			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_NODE)) {
				return  new ArroNodeLayoutFeature(this);				
			} else if(pict != null && pict.equals(Constants.PROP_PICT_PAD)) {
				return  new ArroPadLayoutFeature(this);				
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
			
			if(pict != null && pict.equals(Constants.PROP_PICT_NODE)) {
				return  new ArroNodeUpdateFeature(this);				
			} else if(pict != null && pict.equals(Constants.PROP_PICT_PAD)) {
				return  new ArroPadUpdateFeature(this);				
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
