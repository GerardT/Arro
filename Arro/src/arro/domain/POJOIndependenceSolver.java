package arro.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.impl.IIndependenceSolver;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import util.Logger;


/**
 * This class is there to allow Graphiti to look up domain objects (NonEmfDomainObject)
 * using getBusinessObjectForKey:
 * - Node objects
 * - Pad objects
 * In order to allow cleanup up when a diagram is closed, we also store the diagram ID
 * with the domain object.
 * 
 */
public class POJOIndependenceSolver implements IIndependenceSolver {
		
	/* Make it a singleton, used for all PictogramElements in all diagrams. */
	private static POJOIndependenceSolver pojoIndependenceSolver = null;
	
	/* for XML load / store */
	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	
	//private ArroNodeInternal intern;
	private HashMap<String, NonEmfDomainObject> domainObjects;
	
	// Build a very simple cache. Increase usage count on every access.
	// A garbage collector cleans entries with count != 0 and resets all counts to 0.

	
	
	public POJOIndependenceSolver() {
		domainObjects = new HashMap<String, NonEmfDomainObject>();
	}
	
	public static POJOIndependenceSolver getInstance() {
		if(pojoIndependenceSolver == null)
			pojoIndependenceSolver = new POJOIndependenceSolver();

		return pojoIndependenceSolver;
	}

	/**
	 * This function provides the key for the given business object.
	 * Key being the UUID string used in Graphiti diagrams.
	 * 
	 * @see getBusinessObjectForKey
	 * @see org.eclipse.graphiti.features.impl.IIndependenceSolver#getKeyForBusinessObject(java.lang.Object)
	 */
	public String getKeyForBusinessObject(Object bo) {
		String result = null;
		if(bo != null && bo instanceof NonEmfDomainObject ) {
			result = ((NonEmfDomainObject) bo).getId();
		}
		return result;
	}

	/**
	 * This function provides the business object for the given key.
	 * Key being the UUID string used in Graphiti diagrams.
	 * 
	 * @see getKeyForBusinessObject
	 * @see org.eclipse.graphiti.features.impl.IIndependenceSolver#getBusinessObjectForKey(java.lang.String)
	 */
	public Object getBusinessObjectForKey(String key) {
		return domainObjects.get(key);
	}
	
	/**
	 * Do not remove BOs from the list. When an undo is done on a Graphiti
	 * element referring such object, the link would be broken.
	 * 
	 * @param bo
	 */
	public void removeBusinessObject(Object bo) {
	}

	public void RemovePOJOObjects(String fullName) {
		// TODO Auto-generated method stub
        String name = fullName.  substring(0, fullName.indexOf('.'));
		Logger.out.trace(Logger.STD, "Remove POJO file " + name);
		
		ResourceCache.getInstance().removeFromCache(name);
		
	}

	public void RegisterPOJOObject(NonEmfDomainObject nonEmfDomainObject) {
		Collection<NonEmfDomainObject> list = domainObjects.values();
		for(NonEmfDomainObject obj: list) {
			if(obj == nonEmfDomainObject) {
				// remove it
				domainObjects.remove(obj.getId());
				break;
			}
		}
		domainObjects.put(nonEmfDomainObject.getId(), nonEmfDomainObject);
	}
	
    public NonEmfDomainObject findPOJOObjectByName(String name, Class<?> class1) {
	    Iterator<Entry<String, NonEmfDomainObject>> it = domainObjects.entrySet().iterator();
	    
	    NonEmfDomainObject found = null;
	    
	    // search for business object that is a ArroNode with name nodeName
	    Logger.out.trace(Logger.STD, "printing POJOIndependenceSolver list ");
	    
	    while (it.hasNext()) {
	    	Map.Entry<String, NonEmfDomainObject> pair = it.next();
	    	Logger.out.trace(Logger.STD, "id: " + pair.getKey() + " name: " + pair.getValue().getName());
	    	if(pair.getValue().getName().equals(name) /*&& pair.getValue().getClass().equals(class1)*/) {
	    		found = pair.getValue();
	    	}
	    }
	    return found;
    
    }
    
    // Helper method to get POJO from PE
    public NonEmfDomainObject findPOJOObjectByPictureElement(PictogramElement pe, IFeatureProvider afp) {
		Object[] obj = afp.getAllBusinessObjectsForPictogramElement(pe);
		if(obj == null || obj.length == 0) {
			return null;
		} else {
			return ((NonEmfDomainObject) (obj[0]));
		}
    }
    
	    

}


