package arro.domain;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



public class ArroCondition extends NonEmfDomainObject {
	private String state;
	@SuppressWarnings("unused")
	private ArroTransition parent;
	
	/**
	 * @param name is the name of the node that is referred.
	 * @param state
	 */
	public ArroCondition(String name, String state) {
		setName(name);
		this.state = state;
	}
	
	public ArroCondition() {
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ArroCondition) {
			ArroCondition cond = (ArroCondition)obj;
			
			return getName().equals(cond.getName()) & state.equals(cond.state);
		} else {
			return super.equals(obj);
		}
	}
	
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	public void xmlWrite(Document doc, Element elt) {
		Attr attr = null;
		
		attr = doc.createAttribute("id");
		attr.setValue(getId());
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("name");
		attr.setValue(getName());
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("state");
		attr.setValue(getName());
		elt.setAttributeNode(attr);
	}
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
    	setName(eElement.getAttribute("name"));
    	setState(eElement.getAttribute("state"));
	}

	public void setParent(ArroTransition ts) {
		parent = ts;
	}
	
}
