package arro.domain;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



public class ArroState extends NonEmfDomainObject{
	private ArroStateDiagram parent;
	private String description;
	
	public ArroStateDiagram getParent() {
		return parent;
	}
	public void setParent(ArroStateDiagram parent) {
		this.parent = parent;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void xmlWrite(Document sub, Element elt) {
		Attr attr = null;
		
		attr = sub.createAttribute("id");
		attr.setValue(getId());
		elt.setAttributeNode(attr);
		
		attr = sub.createAttribute("name");
		attr.setValue(getName());
		elt.setAttributeNode(attr);
	}
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
    	setName(eElement.getAttribute("name"));
 	}

}
