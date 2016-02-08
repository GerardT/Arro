package arro.domain;

import java.util.ArrayList;
import java.util.Collection;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class represents a node as shown in a diagram, the diagram
 * representing a node definition itself, so a little confusing.
 * 
 */
public class ArroDevice extends NonEmfDomainObject {
		
	private String url;
	private DomainModule parent = null;
	private ArrayList<ArroParameter> parameterList = new ArrayList<ArroParameter>();
	
	public ArroDevice() {
		super();
	}
	
	
	public void xmlWrite(Document doc, Element elt) {
		Attr attr = null;
		
		attr = doc.createAttribute("id");
		attr.setValue(getId());
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("url");
		attr.setValue(getUrl());
		elt.setAttributeNode(attr);
		
		int count = parameterList.size();
		for(int i = 0; i < count; i++) {
			ArroParameter parm = parameterList.get(i);
			
			Element sub = doc.createElement("param");
			elt.appendChild(sub);
			
			attr = doc.createAttribute("key");
			attr.setValue(parm.getFormalKey());
			sub.setAttributeNode(attr);
			
			attr = doc.createAttribute("subst");
			attr.setValue(parm.getSubstitute());
			sub.setAttributeNode(attr);
			
			attr = doc.createAttribute("value");
			attr.setValue(parm.getValue());
			sub.setAttributeNode(attr);
		}

	}
	
	public void xmlRead(Node nNode) {
		Element eElement = (Element) nNode;
    	setId(eElement.getAttribute("id"));
    	setUrl(eElement.getAttribute("url"));
    	
    	NodeList nList = nNode.getChildNodes();
    	for (int temp = 0; temp < nList.getLength(); temp++) {
    		Node sub = nList.item(temp);
    		
			if(sub.getNodeName().equals("param")) {
	    		Element eSubElement = (Element) sub;
	    		
	    		parameterList.add(new ArroParameter(
	    				eSubElement.getAttribute("key"),
	    				eSubElement.getAttribute("subst"),
	    				eSubElement.getAttribute("value"),
	    				""));
			}
    	}

   	}

	public void setUrl(String url) {
		this.url = url;		
	}

	public String getUrl() {
		return url;		
	}


	/**
	 * Get the list of parameters defined by this ArroNode. Those are defined
	 * in the DomainModule that this ArroNode is an instantiation of.
	 * 
	 * @return
	 */
	public ArrayList<ArroParameter> getParameterList() {
		return parameterList;
	}
	
	public void setParameterList(ArrayList<ArroParameter> params) {
	    parameterList = params;
	}

	public void setParent(DomainModule domainModule) {
		this.parent = domainModule;
	}
	public DomainModule getParent() {
		return parent;
	}

}
