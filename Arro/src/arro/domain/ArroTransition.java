package arro.domain;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class ArroTransition extends NonEmfDomainObject {
	private ArroSequenceChart parent;
	private String condition = new String();
	
	
	public ArroSequenceChart getParent() {
		return parent;
	}
	public void setParent(ArroSequenceChart parent) {
		this.parent = parent;
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	
	public void xmlWrite(Document doc, Element elt) {
		Attr attr = null;
		
		attr = doc.createAttribute("id");
		attr.setValue(getId());
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("name");
		attr.setValue(getName());
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("condition");
		attr.setValue(getCondition());
		elt.setAttributeNode(attr);
	}
	
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
    	setName(eElement.getAttribute("name"));
    	setCondition(eElement.getAttribute("condition"));
	}
	
}


