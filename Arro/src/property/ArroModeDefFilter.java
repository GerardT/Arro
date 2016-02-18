package property;

import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.platform.AbstractPropertySectionFilter;

import arro.Constants;

public class ArroModeDefFilter extends AbstractPropertySectionFilter {

    @Override
    protected boolean accept(PictogramElement pe) {
		if(pe instanceof Diagram) {
			Diagram cs = (Diagram)pe;
			
			if(cs.getDiagramTypeId().equals(Constants.FUNCTION_DIAGRAM_TYPE)) {
			
			//String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.FUNCTION_BLOCK);
			
			//if(pict != null && pict.equals(Constants.PROP_PICT_STATE)) {
				return true;
			}
		} else if(pe instanceof ContainerShape) {
			ContainerShape cs = (ContainerShape)pe;
			
			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_STATE)) {
				return true;
			}
		}

        return false;
    }

}
