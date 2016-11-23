package arro.domain;

import java.util.ArrayList;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ArroTransition extends NonEmfDomainObject {
	private ArroSequenceChart parent;
    private String condition = new String();
    private String from = new String();
    private String to = new String();
    private ArrayList<ArroAction> entryActions = new ArrayList<ArroAction>();
	
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
	
    public ArrayList<ArroAction> getEntryActions() {
        // Make sure there is a least one empty action ("", "")
        int i = 0;
        for(ArroAction entry : entryActions) {
            if(entry.getName().equals("") && entry.getState().equals("")) {
                i++;
            }
        }
        if(i == 0) {
            entryActions.add(new ArroAction("", ""));
        }
        return entryActions;
    }
    
    // Allow client code to update individual elements, use address
    // of object to find it.
    public void updateEntry(ArroAction action, ArroAction newValue) {
        int index = entryActions.indexOf(action);
        if(index != -1) {
            entryActions.get(index).name = newValue.name;
            entryActions.get(index).state = newValue.state;
        }
        
        // Remove superfluous empty entries, add one empty entry at end.
        ArrayList<ArroAction> removals = new ArrayList<ArroAction>();
        for(ArroAction entry : entryActions) {
            if(entry.getName().equals("") && entry.getState().equals("")) {
                removals.add(entry);
            }
        }
        for(ArroAction entry : removals) {
            entryActions.remove(entry);
        }
    }

    public String getTarget() {
        return to;
    }
    public String getSource() {
        return from;
    }
    public void setTarget(String attribute) {
        to = attribute;   
    }
    
    public void setSource(String attribute) {
        from = attribute;
    }

    public void xmlWrite(Document doc, Element elt) {
		Attr attr = null;
		
		attr = doc.createAttribute("id");
		attr.setValue(getId());
		elt.setAttributeNode(attr);
		
        attr = doc.createAttribute("name");
        attr.setValue(getName());
        elt.setAttributeNode(attr);
        
        attr = doc.createAttribute("source");
        attr.setValue(getSource());
        elt.setAttributeNode(attr);
        
        attr = doc.createAttribute("target");
        attr.setValue(getTarget());
        elt.setAttributeNode(attr);
        
		attr = doc.createAttribute("condition");
		attr.setValue(getCondition());
		elt.setAttributeNode(attr);
		
        for(ArroAction action: entryActions) {
            
            Element sub = doc.createElement("action");
            elt.appendChild(sub);
            
            action.xmlWrite(doc, sub);
        }
	}
	
    public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
        setName(eElement.getAttribute("name"));
        setSource(eElement.getAttribute("source"));
        setTarget(eElement.getAttribute("target"));
    	setCondition(eElement.getAttribute("condition"));
        
        NodeList nList = nNode.getChildNodes();
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node sub = nList.item(temp);
            
            if(sub.getNodeName().equals("action")) {
                Element eSubElement = (Element) sub;
                ArroAction action = new ArroAction(this);

                action.xmlRead(eSubElement);
                
                entryActions.add(action);
            }
        }
	}
	
}


