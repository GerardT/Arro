package arro.domain;

import java.util.ArrayList;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ArroTransition extends NonEmfDomainObject {
	private ArroStateDiagram parent;
	private ArrayList<ArroCondition> conditions = new ArrayList<ArroCondition>();
	
	
	public ArroStateDiagram getParent() {
		return parent;
	}
	public void setParent(ArroStateDiagram parent) {
		this.parent = parent;
	}
	public ArrayList<ArroCondition> getConditions() {
		return conditions;
	}
	
	public void xmlWrite(Document doc, Element elt) {
		Attr attr = null;
		
		attr = doc.createAttribute("id");
		attr.setValue(getId());
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("name");
		attr.setValue(getName());
		elt.setAttributeNode(attr);
		
		for(ArroCondition condition: conditions) {
			
			Element sub = doc.createElement("condition");
			elt.appendChild(sub);
			
			condition.xmlWrite(doc, sub);
		}
	}
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
    	setName(eElement.getAttribute("name"));
    	
    	NodeList nList = nNode.getChildNodes();
    	for (int temp = 0; temp < nList.getLength(); temp++) {
    		Node sub = nList.item(temp);
    		
			if(sub.getNodeName().equals("condition")) {
	    		Element eSubElement = (Element) sub;
	    		ArroCondition condition = new ArroCondition();
	    		
	    		condition.xmlRead(eSubElement);
	    		
	    		addCondition(condition);
			}
    	}
	}
	
	private void addCondition(ArroCondition condition) {
		conditions.add(condition);
		condition.setParent(this);
	}
}


