package arro.diagram.types;

import org.eclipse.graphiti.dt.AbstractDiagramTypeProvider;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;

public class SfcLeafTypeProvider extends AbstractDiagramTypeProvider
		implements IDiagramTypeProvider {

	public SfcLeafTypeProvider() {
		super();
		setFeatureProvider(new SfcLeafFeatureProvider(this));
	}

//    private IToolBehaviorProvider[] toolBehaviorProviders;
//   
//    @Override
//    public IToolBehaviorProvider[] getAvailableToolBehaviorProviders() {
//        if (toolBehaviorProviders == null) {
//            toolBehaviorProviders =
//                new IToolBehaviorProvider[] { new StateDiagramToolBehaviorProvider(
//                    this) };
//        }
//        return toolBehaviorProviders;
//    }
}
