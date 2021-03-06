package arro.diagram.types;

import java.util.ArrayList;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.palette.IPaletteCompartmentEntry;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.tb.DefaultToolBehaviorProvider;
import org.eclipse.graphiti.tb.IDecorator;
import org.eclipse.graphiti.tb.ImageDecorator;

import arro.ArroImageProvider;
import arro.Constants;
import arro.domain.ArroNode;

public class StateDiagramToolBehaviorProvider extends DefaultToolBehaviorProvider{

    public StateDiagramToolBehaviorProvider(IDiagramTypeProvider dtp) {
        super(dtp);
    }
    
    @Override
    public boolean isShowFlyoutPalette() {
    	return true;
    }
    
    /**
     * Remove unneeded entries from Palette.
     */
    @Override
    public IPaletteCompartmentEntry[] getPalette() {
    	ArrayList<IPaletteCompartmentEntry> ret = new ArrayList<IPaletteCompartmentEntry>();
    	
        // add compartments from super class
        IPaletteCompartmentEntry[] superCompartments = super.getPalette();
        for (int i = 0; i < superCompartments.length; i++) {
        	//IPaletteCompartmentEntry comp = superCompartments[i];
        	if(true /*comp.getLabel().equals("Step")*/) {
                ret.add(superCompartments[i]);
        	}
        }
     
        return ret.toArray(new IPaletteCompartmentEntry[ret.size()]);
    }
    
    /**
     * If the node needs update then return a decorator that shows an icon on top
     * of the node.
     */
    @Override
    public IDecorator[] getDecorators(PictogramElement pe) {
        IFeatureProvider featureProvider = getFeatureProvider();
        Object bo = featureProvider.getBusinessObjectForPictogramElement(pe);
        
        if(bo instanceof ArroNode) {
            if (pe instanceof ContainerShape) {
                ContainerShape cs = (ContainerShape) pe;

    			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
    			
    			if(pict != null && pict.equals(Constants.PROP_PICT_NODE) /*&& ((ArroNode)bo).needsUpdate()*/) {
    	            ImageDecorator peDecorator = new ImageDecorator(ArroImageProvider.IMG_NEED_UPDATE);
    	            peDecorator.setX(10);
    	                	            
    	            return new IDecorator[] { peDecorator };
    			}
            }
        }
     
        return super.getDecorators(pe);
    }
}