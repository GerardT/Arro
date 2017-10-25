package arro.diagram.features;

import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.util.IColorConstant;

import arro.Constants;
import arro.domain.ArroConnection;
import arro.domain.ArroModule;
import arro.editors.FunctionDiagramEditor;
import util.Logger;


public class ArroConnectionAddFeature extends AbstractAddFeature implements
		IAddFeature {

	public ArroConnectionAddFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
    public boolean canAdd(IAddContext context) {
		return context instanceof IAddConnectionContext /* && context.getNewObject() instanceof DomainObjectConnection */;
	}

	@Override
    public PictogramElement add(IAddContext context) {
        IDiagramContainer dc = getDiagramBehavior().getDiagramContainer();
        if(!(dc instanceof FunctionDiagramEditor)) {
        	Logger.out.trace(Logger.EDITOR, "not an editor");
        	return null;
        }
        ArroModule domainModule =  ((FunctionDiagramEditor)dc).getDomainModule();

		ArroConnection newConnection = new ArroConnection();
		newConnection.setSource((String) context.getProperty(Constants.PROP_SOURCE_PAD_KEY));
		newConnection.setTarget((String) context.getProperty(Constants.PROP_TARGET_PAD_KEY));
        
		IAddConnectionContext addConContext = (IAddConnectionContext) context;
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();
		
		Connection connection = peCreateService.createFreeFormConnection(getDiagram());
		connection.setStart(addConContext.getSourceAnchor());
		connection.setEnd(addConContext.getTargetAnchor());

		Polyline polyline = gaService.createPlainPolyline(connection);
		polyline.setForeground(manageColor(Constants.CONNECTOR));

        Graphiti.getPeService().setPropertyValue(connection, Constants.PROP_PICT_KEY, Constants.PROP_PICT_CONNECTION);
		
        domainModule.addConnection(newConnection);

		link(connection, newConnection);

		return connection;
	}

}
