package property;

import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.platform.AbstractPropertySectionFilter;

import arro.Constants;

public class ArroPublishedActionsFilter extends AbstractPropertySectionFilter {

    @Override
    protected boolean accept(PictogramElement pe) {
		if(pe instanceof Diagram) {
		    Diagram cs = (Diagram)pe;
		    
		    String type = cs.getDiagramTypeId();
			
			if(type.equals(Constants.STATE_NODE_DIAGRAM_TYPE) || type.equals(Constants.STATE_LEAF_DIAGRAM_TYPE)) {
				return true;
			}
		}

        return false;
    }

}
