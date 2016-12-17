package arro.domain;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ArroRequest {
    private String request = "";

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }
    

    
    public void xmlWrite(Document doc, Element elt) {
        Attr attr = null;
        
        attr = doc.createAttribute("request");
        attr.setValue(request);
        elt.setAttributeNode(attr);
    }

    public void xmlRead(Node nNode) {
        Element eElement = (Element) nNode;
        setRequest(eElement.getAttribute("request"));
    }

    public void setValue(String value) {
        request = value;
        
    }

    public String getValue() {
        return request;
    }

}
