package arro.diagram.features;

import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;

import arro.Constants;
import arro.domain.ArroTransition;

public class TransitionHelper {
	
	private Connection connection;
	private Polyline polyline, bar;
	private ConnectionDecorator cd;
	
	
	public Connection create(IAddContext context, ArroTransition addedDomainObject, Color fg, Color bg, Diagram targetDiagram) {
		IAddConnectionContext addConContext = (IAddConnectionContext) context;
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();
		
		Anchor start = addConContext.getSourceAnchor();
		Anchor end = addConContext.getTargetAnchor();
		
		if(start != end) {
	        connection = peCreateService.createFreeFormConnection(targetDiagram);
	        connection.setStart(addConContext.getSourceAnchor());
	        connection.setEnd(addConContext.getTargetAnchor());
	        
            Graphiti.getPeService().setPropertyValue(connection, Constants.PROP_PICT_KEY, Constants.PROP_PICT_TRANSITION);
            polyline = gaService.createPlainPolyline(connection);
            
            // add static graphical decorator (composition and navigable)
            cd = peCreateService.createConnectionDecorator(connection, false, 1.0, true);
            // Coordinates relative to line!
            bar = gaService.createPolyline(cd, new int[] { -15, 10, 0, 0, -15, -10 });
            
            polyline.setForeground(fg);
            bar.setForeground(fg);
            bar.setLineWidth(2);         
		} else {
		    connection = null;
		}
        return connection;
       
	}
}
