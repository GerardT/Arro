package arro.diagram.types;


import java.util.HashMap;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.dt.AbstractDiagramTypeProvider;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.platform.IDiagramBehavior;
import org.eclipse.graphiti.tb.IToolBehaviorProvider;

import arro.workspace.MyResourceChangeReporter;
import util.Logger;

/**
 * Even for same diagram type there will be one instance of this class.
 *
 */
public class FunctionNodeDiagramTypeProvider extends AbstractDiagramTypeProvider {
	
	private static HashMap<Diagram, FunctionNodeDiagramTypeProvider> diagrams = new HashMap<Diagram, FunctionNodeDiagramTypeProvider>();
	public FunctionNodeDiagramTypeProvider() {
		super();
		setFeatureProvider(new FunctionNodeDiagramFeatureProvider(this));
	    IResourceChangeListener listener = new MyResourceChangeReporter();
	    ResourcesPlugin.getWorkspace().addResourceChangeListener(
	      listener, IResourceChangeEvent.POST_CHANGE);
	}
	
	@Override
	public void resourcesSaved(Diagram diagram, Resource[] resources) {
		super.resourcesSaved(diagram, resources);
//		
//		ArroNodeFeatureProvider.getIndependeceSolver().storePOJOObjects(diagram.getName());
	}
	
	@Override
	public void init(Diagram diagram, IDiagramBehavior diagramBehavior) {
		super.init(diagram, diagramBehavior);
		
		Logger.out.trace(Logger.EDITOR, "Opening diagram " + diagram.getName());
		
		diagrams.put(diagram, this);
					
//		if(ArroNodeFeatureProvider.getIndependeceSolver().loadPOJOObjects(diagram.getName()) == false) {
//	    	// Then it must be that a new diagram is created...
//	    	ArroNode node = new ArroNode();
//	    	node.setName(diagram.getName());
//		}
	}
	
//	public static ArroNodeDiagramTypeProvider getDiagramTypeProviderFromDiagram(Diagram d) {
//		return diagrams.get(d);
//	}
	   private IToolBehaviorProvider[] toolBehaviorProviders;
	   
	    @Override
	    public IToolBehaviorProvider[] getAvailableToolBehaviorProviders() {
	        if (toolBehaviorProviders == null) {
	            toolBehaviorProviders =
	                new IToolBehaviorProvider[] { new FunctionDiagramToolBehaviorProvider(
	                    this) };
	        }
	        return toolBehaviorProviders;
	    }
	    
}
