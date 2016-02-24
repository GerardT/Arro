package arro.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArroStateDiagram extends NonEmfDomainObject {

	private String nodeType;
	private HashMap<String, ArroState> states = new HashMap<String, ArroState>();
	private HashMap<String, ArroTransition> transitions = new HashMap<String, ArroTransition>();
	private ArroModule parent;
	
	@SuppressWarnings("unchecked")
	public ArroStateDiagram clone() {
		ArroStateDiagram diag = new ArroStateDiagram();
		diag.states = (HashMap<String, ArroState>) states.clone();
		diag.transitions = (HashMap<String, ArroTransition>) transitions.clone();
		diag.nodeType = nodeType;
		diag.parent = parent;
		return diag;		
	}
	
	public void addState(ArroState newState) {
		states.put(newState.getName(), newState);
		newState.setParent(this);
	}
	
	public void addTransition(ArroTransition newTransition) {
		transitions.put(newTransition.getName(), newTransition);
		newTransition.setParent(this);
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
		
		Collection<ArroState> st = states.values();
		for(ArroState state: st) {
			
			Element sub = doc.createElement("state");
			elt.appendChild(sub);
			
			state.xmlWrite(doc, sub);
		}
		Collection<ArroTransition> ts = transitions.values();
		for(ArroTransition transition: ts) {
			
			Element sub = doc.createElement("transition");
			elt.appendChild(sub);
			
			transition.xmlWrite(doc, sub);
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
	    		
	    		state.xmlRead(eSubElement);
	    		
	    		addState(state);
			}
			if(sub.getNodeName().equals("transition")) {
	    		Element eSubElement = (Element) sub;
	    		ArroTransition transition = new ArroTransition();
	    		
	    		transition.xmlRead(eSubElement);
	    		
	    		addTransition(transition);
			}
    	}
   	}

	public ArroState getStateByName(String instanceName) {
		return states.get(instanceName);
	}

	public void removeState(ArroState obj) {
		states.remove(obj.getName());
	}

	public ArroTransition getTransitionByName(String instanceName) {
		return transitions.get(instanceName);
	}

	public void removeTransition(ArroTransition obj) {
		transitions.remove(obj.getName());
	}

	public void setParent(ArroModule module) {
		parent = module;
	}
	public ArroModule getParent() {
		return parent;
	}

	public ArrayList<String> getStateNames() {
		ArrayList<String> list = new ArrayList<String>();
		
		Collection<ArroState> values = states.values();
		for(ArroState state: values){
			list.add(state.getName());
		}
		return list;
	}

}
