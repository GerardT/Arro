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
		
		connection = peCreateService.createFreeFormConnection(targetDiagram);
		connection.setStart(addConContext.getSourceAnchor());
		connection.setEnd(addConContext.getTargetAnchor());
		
		Anchor start = connection.getStart();
		Anchor end = connection.getEnd();
		
        String startType = Graphiti.getPeService().getPropertyValue(start, Constants.PROP_PAD_NAME_KEY);
        String endType = Graphiti.getPeService().getPropertyValue(end, Constants.PROP_PAD_NAME_KEY);
        
        assert(startType != null);
        assert(endType != null);
        
		// The following combinations are allowed:
//        if((
//        		(startType.equals(Constants.PROP_PAD_NAME_SYNC_START_OUT) && endType.equals(Constants.PROP_PAD_NAME_STEP_IN)) ||
//        		(startType.equals(Constants.PROP_PAD_NAME_STEP_OUT) && endType.equals(Constants.PROP_PAD_NAME_SYNC_STOP_IN))
//        		
//        		)) {
//            Graphiti.getPeService().setPropertyValue(connection, Constants.PROP_PICT_KEY, Constants.PROP_PICT_NULL_TRANSITION);
//    		polyline = gaService.createPlainPolyline(connection);
//			
//			polyline.setForeground(fg);
//        } else {
            Graphiti.getPeService().setPropertyValue(connection, Constants.PROP_PICT_KEY, Constants.PROP_PICT_TRANSITION);
    		polyline = gaService.createPlainPolyline(connection);
    		
   	        // add static graphical decorator (composition and navigable)
   	        cd = peCreateService.createConnectionDecorator(connection, false, 0.5, true);
   	        // Coordinates relative to line!
   	        bar = gaService.createPolyline(cd, new int[] { 0, -20, 0, 20 });
			
			polyline.setForeground(fg);
			bar.setForeground(fg);
	        bar.setLineWidth(2);	     
//        }
        return connection;
       
	}
}
