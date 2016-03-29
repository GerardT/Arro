package property;

import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.platform.AbstractPropertySectionFilter;

import arro.Constants;

public class ArroStateFilter extends AbstractPropertySectionFilter {

    @Override
    protected boolean accept(PictogramElement pe) {
		if(pe instanceof ContainerShape) {
			ContainerShape cs = (ContainerShape)pe;
			
			String pict = Graphiti.getPeService().getPropertyValue(cs, Constants.PROP_PICT_KEY);
			
			if(pict != null && pict.equals(Constants.PROP_PICT_STEP)) {
				return true;
			}
		}

        return false;
    }

}
