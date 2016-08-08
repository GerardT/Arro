package arro.diagram.types;

import org.eclipse.graphiti.dt.AbstractDiagramTypeProvider;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.tb.IToolBehaviorProvider;

public class SfcNodeTypeProvider extends AbstractDiagramTypeProvider
		implements IDiagramTypeProvider {

	public SfcNodeTypeProvider() {
		super();
		setFeatureProvider(new SfcNodeFeatureProvider(this));
	}

    private IToolBehaviorProvider[] toolBehaviorProviders;
   
    @Override
    public IToolBehaviorProvider[] getAvailableToolBehaviorProviders() {
        if (toolBehaviorProviders == null) {
            toolBehaviorProviders =
                new IToolBehaviorProvider[] { new StateDiagramToolBehaviorProvider(
                    this) };
        }
        return toolBehaviorProviders;
    }
}
