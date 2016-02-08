package arro.domain;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ArroPad extends NonEmfDomainObject {
	
	private String nodeClass;
	private DomainModule parent;
	private boolean input = true;
	private boolean run = true;
	
	public ArroPad() {
		super();
	}
	
	public String getType() {
		return nodeClass;
	}

	public void setType(String name) {
		this.nodeClass = name;
	}
	
	public boolean getRun() {
		return run;
	}

	public void setRun(boolean run) {
		this.run = run;
		
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
		
		attr = doc.createAttribute("input");
		attr.setValue(getInput()? "true": "false");
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("run");
		attr.setValue(getRun()? "true": "false");
		elt.setAttributeNode(attr);
	}
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
    	setName(eElement.getAttribute("name"));
    	setType(eElement.getAttribute("type"));
    	String in = eElement.getAttribute("input");
    	setInput(in.equals("true")? true: false);
    	String run = eElement.getAttribute("run");
    	setRun(run.equals("true")? true: false);
	}

	public void setParent(DomainModule domainModule) {
		this.parent = domainModule;
	}

	public DomainModule getParent() {
		return parent;
	}

	public void setInput(boolean input) {
		this.input = input;
	}
	public boolean getInput() {
		return input;
	}

}
