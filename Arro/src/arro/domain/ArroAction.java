package arro.domain;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ArroAction {
	String name;
	String state;
	ArroTransition parent;
	
	public ArroAction(String name, String state) {
		this.name = name;
		this.state = state;
	}
	public ArroAction(ArroTransition arroTransition) {
		this.parent = arroTransition;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public void xmlWrite(Document doc, Element elt) {
		Attr attr = null;
		
		attr = doc.createAttribute("node");
		attr.setValue(name);
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("action");
		attr.setValue(state);
		elt.setAttributeNode(attr);
	}
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	name = (eElement.getAttribute("node"));
    	state = (eElement.getAttribute("action"));
	}

}
