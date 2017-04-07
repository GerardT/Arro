package arro.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.Logger;
import workspace.ResourceCache;

/**
 * This class represents a node as shown in a diagram, the diagram
 * representing a node definition itself, so a little confusing.
 * 
 */
public class ArroNode extends NonEmfDomainObject {
		
	private String nodeType;
	private ArroModule parent = null;
	private ArrayList<ArroParameter> parameterList = new ArrayList<ArroParameter>();
	private boolean needsUpdate;
	
	public boolean needsUpdate() {
		return needsUpdate;
	}

	public void setNeedsUpdate(boolean needsUpdate) {
		this.needsUpdate = needsUpdate;
	}

	public ArroNode() {
		super();
	}
	
	public String getType() {
		return nodeType;
	}
	
	public void setType(String name) {
		this.nodeType = name;
	}
	
	public ArroPad getPadByName(String name) {
		// return the Pad info based on the name.
		ArroModule domainObject = (ArroModule)ResourceCache.getInstance().getZip(getType()).getDomainDiagram();		
		
		return domainObject.getPadByName(name);
	}
	
	
	public void xmlWrite(Document doc, Element elt) {
		Attr attr = null;
		
		attr = doc.createAttribute("id");
		attr.setValue(getId());
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("name");
		attr.setValue(getName());
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("type");
		attr.setValue(getType());
		elt.setAttributeNode(attr);
		
		int count = parameterList.size();
		for(int i = 0; i < count; i++) {
			ArroParameter parm = parameterList.get(i);
			
			Element sub = doc.createElement("param");
			elt.appendChild(sub);
			
			attr = doc.createAttribute("key");
			attr.setValue(parm.getFormalKey());
			sub.setAttributeNode(attr);
			
			attr = doc.createAttribute("subst");
			attr.setValue(parm.getSubstitute());
			sub.setAttributeNode(attr);
			
			attr = doc.createAttribute("value");
			attr.setValue(parm.getValue());
			sub.setAttributeNode(attr);
		}

		
	}
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
    	setName(eElement.getAttribute("name"));
    	setType(eElement.getAttribute("type"));
    	
    	NodeList nList = nNode.getChildNodes();
    	for (int temp = 0; temp < nList.getLength(); temp++) {
    		Node sub = nList.item(temp);
    		
			if(sub.getNodeName().equals("param")) {
	    		Element eSubElement = (Element) sub;
	    		
	    		parameterList.add(new ArroParameter(
	    				eSubElement.getAttribute("key"),
	    				eSubElement.getAttribute("subst"),
	    				eSubElement.getAttribute("value"),
	    				""));
			}
    	}

   	}

	/**
	 * Get module that defines this node. This method will load
	 * the info from disk if needed.
	 * 
	 * @return
	 */
	public ArroModule getAssociatedModule() {
        return (ArroModule)ResourceCache.getInstance().getZip(getType()).getDomainDiagram();
	}
	
	/**
	 * Get the list of parameters defined by this ArroNode. Those are defined
	 * in the DomainModule that this ArroNode is an instantiation of.
	 * 
	 * @return
	 */
	public ArrayList<ArroParameter> getParameterList() {
		updateParameters();
		
		return parameterList;
	}
	
	public void setParameterList(ArrayList<ArroParameter> params) {
	    parameterList = params;
	}
	
	/**
	 * Get the list of parameters that is defined by this ArroNode. Those
	 * are defined in the DomainModule that this ArroNode is an instantiation
	 * of.
	 * 
	 * @return
	 */
//	public ArrayList<ArroParameter> getParameterDefinitions() {
//		readNodeInfo();		
//
//		return domainObject.getAllParameterDefinitions();
//	}
	


	public void setParent(ArroModule domainModule) {
		this.parent = domainModule;
	}
	public ArroModule getParent() {
		return parent;
	}
	
	public void updateParameters() {
		ArrayList<ArroParameter> allParameters = new ArrayList<ArroParameter>();
		
		ArroModule domainObject = (ArroModule)ResourceCache.getInstance().getZip(getType()).getDomainDiagram();
		
		HashMap<String, ArroParameter> paramMap = domainObject.getParameterExports();
		
		Collection<ArroParameter> params = paramMap.values();

		// collect all parameters
		for(ArroParameter p: params) {
			Logger.out.trace(Logger.STD, "From " + getName() + " get parameter " + p.getSubstitute());
			if(!(p.getSubstitute().equals(""))) {
				allParameters.add(new ArroParameter(
						p.getSubstitute(),  // now Key
						"",
						p.getValue(),
						getName()));
			}
		}

		// merge with current list
		for(ArroParameter p: allParameters) {
			ArroParameter match = findParameter(p.getFormalKey());
			if(match != null) {
				p.setKey(match.getFormalKey());
				p.setValue(match.getValue());
				p.setSubstitute(match.getSubstitute());
			}
		}
		parameterList = allParameters;
	}
	private ArroParameter findParameter(String subst) {
		for(ArroParameter p: parameterList) {
			if((!subst.equals("")) && p.getFormalKey().equals(subst)) {
				return p;
			}
		}
		return null;
	}



}
