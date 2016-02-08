package arro.domain;

import java.util.Collection;
import java.util.HashMap;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import arro.domain.ArroState;

public class ArroStateDiagram extends NonEmfDomainObject {

	private String nodeType;
	private HashMap<String, ArroState> states = new HashMap<String, ArroState>();
	
	@SuppressWarnings("unchecked")
	public ArroStateDiagram clone() {
		ArroStateDiagram diag = new ArroStateDiagram();
		diag.states = (HashMap<String, ArroState>) states.clone();
		diag.nodeType = nodeType;
		return diag;		
	}
	
	public void addState(ArroState newState) {
		states.put(newState.getName(), newState);
		newState.setParent(this);
	}
	
	public String getType() {
		return nodeType;
	}
	
	public void setType(String name) {
		this.nodeType = name;
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
		
		Collection<ArroState> col = states.values();
		for(ArroState state: col) {
			
			Element sub = doc.createElement("state");
			elt.appendChild(sub);
			
			attr = doc.createAttribute("name");
			attr.setValue(state.getName());
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
    		
			if(sub.getNodeName().equals("state")) {
	    		Element eSubElement = (Element) sub;
	    		ArroState state = new ArroState();
	    		
	    		String name = eSubElement.getAttribute("name");
	    		state.setName(name);
	    		addState(state);
			}
    	}
   	}

	public ArroState getStateByName(String instanceName) {
		return states.get(instanceName);
	}

	public void removeState(ArroState obj) {
		states.remove(obj.getName());
	}

}
