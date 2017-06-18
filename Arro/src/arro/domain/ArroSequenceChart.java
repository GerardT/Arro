package arro.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.core.commands.ExecutionException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArroSequenceChart extends NonEmfDomainObject {

	private String nodeType;
	private HashMap<String, ArroStep> steps = new HashMap<String, ArroStep>();
	private HashMap<String, ArroTransition> transitions = new HashMap<String, ArroTransition>();
    private ArrayList<ArroRequest> publishedActions = new ArrayList<ArroRequest>();;
	private ArroModule parent;
	
	@SuppressWarnings("unchecked")
	public ArroSequenceChart clone() {
		ArroSequenceChart diag = new ArroSequenceChart();
		diag.steps = (HashMap<String, ArroStep>) steps.clone();
		diag.transitions = (HashMap<String, ArroTransition>) transitions.clone();
		diag.nodeType = nodeType;
		diag.parent = parent;
		return diag;		
	}
	
	public void addState(ArroStep newState) throws ExecutionException {
        if(steps.containsKey(newState.getName())) {
            throw new ExecutionException("Non-unique step name", null);
        }
		steps.put(newState.getName(), newState);
		newState.setParent(this);
	}
	
	public void addTransition(ArroTransition newTransition) throws ExecutionException {
	    if(transitions.containsKey(newTransition.getName())) {
            throw new ExecutionException("Non-unique transition name", null);
	    }
		transitions.put(newTransition.getName(), newTransition);
		newTransition.setParent(this);
	}
	
	public String getType() {
		return nodeType;
	}
	
	public void setType(String name) {
		this.nodeType = name;
	}

    public void setPublishedActions(ArrayList<ArroRequest> requests) {
        publishedActions = requests;
    }

    public ArrayList<ArroRequest> getPublishedActions() {
        publishedActions.add(new ArroRequest());
        return publishedActions;
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
		
		Collection<ArroStep> st = steps.values();
		for(ArroStep state: st) {
			
			Element sub = doc.createElement("step");
			elt.appendChild(sub);
			
			state.xmlWrite(doc, sub);
		}
		Collection<ArroTransition> ts = transitions.values();
		for(ArroTransition transition: ts) {
			
			Element sub = doc.createElement("transition");
			elt.appendChild(sub);
			
			transition.xmlWrite(doc, sub);
		}
//        for(ArroRequest action: publishedActions) {
//            
//            Element sub = doc.createElement("published-action");
//            elt.appendChild(sub);
//        }
	}
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
    	setName(eElement.getAttribute("name"));
    	setType(eElement.getAttribute("type"));
    	
    	NodeList nList = nNode.getChildNodes();
    	for (int temp = 0; temp < nList.getLength(); temp++) {
    		Node sub = nList.item(temp);
    		
			if(sub.getNodeName().equals("step")) {
	    		Element eSubElement = (Element) sub;
	    		ArroStep state = new ArroStep();
	    		
	    		state.xmlRead(eSubElement);
	    		
	    		try {
                    addState(state);
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
			}
            if(sub.getNodeName().equals("transition")) {
                Element eSubElement = (Element) sub;
                ArroTransition transition = new ArroTransition();
                
                transition.xmlRead(eSubElement);
                
                try {
                    addTransition(transition);
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(sub.getNodeName().equals("published-actions")) {
                Element eSubElement = (Element) sub;
                ArroRequest req = new ArroRequest();
                
                req.xmlRead(eSubElement);
                
                publishedActions.add(req);
            }
    	}
   	}

	public ArroStep getStepByName(String instanceName) {
		return steps.get(instanceName);
	}

	public void removeStep(ArroStep obj) {
		steps.remove(obj.getName());
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
		
		Collection<ArroStep> values = steps.values();
		for(ArroStep state: values){
			list.add(state.getName());
		}
		return list;
	}

	public void addSynchronization(ArroSynchronization addedDomainObject) {
		// TODO Auto-generated method stub
		
	}

}
