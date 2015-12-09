package arro.domain;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ArroConnection extends NonEmfDomainObject {
	
	private String type;
	private DomainNodeDiagram parent;
	private String source, target;
	
	public ArroConnection() {
		super();
		super.setName("aConnection");
	}
	
	public String getType() {
		return type;
	}

	public void setType(String name) {
		this.type = name;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	public String getSource() {
		return this.source;
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	
	public String getTarget() {
		return this.target;
	}
	
	public void xmlWrite(Document doc, Element elt) {
		Attr attr = null;
		
		attr = doc.createAttribute("id");
		attr.setValue(getId());
		elt.setAttributeNode(attr);
		
//		attr = doc.createAttribute("name");
//		attr.setValue(getName());
//		elt.setAttributeNode(attr);
//		
//		attr = doc.createAttribute("type");
//		attr.setValue(getType());
//		elt.setAttributeNode(attr);
//		
		attr = doc.createAttribute("source");
		attr.setValue(source);
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("target");
		attr.setValue(target);
		elt.setAttributeNode(attr);
	}
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
//    	setName(eElement.getAttribute("name"));
//    	setType(eElement.getAttribute("type"));
    	setSource(eElement.getAttribute("source"));
    	setTarget(eElement.getAttribute("target"));
	}

	public void setParent(DomainNodeDiagram domainNodeDiagram) {
		this.parent = domainNodeDiagram;
	}

	public DomainNodeDiagram getParent() {
		return parent;
	}


}
