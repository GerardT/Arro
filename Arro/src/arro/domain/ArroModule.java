package arro.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.Logger;
import workspace.ArroModuleContainer;

/**
 * Represents a function diagram, consisting of Nodes (=composite), Pads,
 * Connections and Devices.
 * TODO Device is called Process in Runtime.
 * 
 */
public class ArroModule extends NonEmfDomainObject  {
	private HashMap<String, ArroNode> nodes = new HashMap<String, ArroNode>();
	private ArroDevice device = null; // Note: either device is specified or one or more nodes. Not both.
	private HashMap<String, ArroPad> pads = new HashMap<String, ArroPad>();
	private HashMap<String, ArroConnection> connections = new HashMap<String, ArroConnection>();
	private String type;
	private ArroSequenceChart stateDiagram;
	
	public ArroModule() {
		super();
		// should only be one state diagram.
		stateDiagram = null;
	}
	
	@Override
    public ArroModuleContainer update() {
	    // update all nodes
	    Collection<ArroNode> v = nodes.values();
	    for(ArroNode n : v) {
	        n.update();
	    }
	    return null;
	}
	
	/*
	 * Node functions: get / add / remove.
	 */
	
	public ArroNode getNode(String id) {
		return nodes.get(id);
	}

	/**
	 * Search node by its given name (not ID).
	 * 
	 * @param name
	 * @return
	 */
	public ArroNode getNodeByName(String name) throws RuntimeException {
		ArroNode ret = findNodeByName(name);
		if(ret == null) {
			throw new RuntimeException("Node not found");
		}
		return ret;
	}

	/**
	 * Search node by its given name (not ID).
	 * 
	 * @param name
	 * @return
	 */
	public ArroNode findNodeByName(String name) throws RuntimeException {
		Collection<ArroNode> n = nodes.values();
		
		for(ArroNode ref: n) {
			if(ref.getName().equals(name)) {
				return ref;
			}
		}
		return null;
	}

	public void addNode(ArroNode node) {
		nodes.put(node.getId(), node);
		node.setParent(this);
	}
	
	public void removeNode(ArroNode node) {
		nodes.remove(node.getId());
	}

	/**
	 * 
	 * Get a map with an entry for each node in this module containing
	 * an array with possible (external) state names for this node.
	 */
	public ArrayList<String> getNodeNames() {
		ArrayList<String> ret = new ArrayList<String>();
		
		Collection<ArroNode> p = nodes.values();
		
		for(ArroNode ref: p) {
			ret.add(ref.getName());
		}
		return ret;
	}
	
	public ArrayList<String> getStateNames() {
	    if(stateDiagram == null) {
	        return new ArrayList<String>(); // empty
	    } else {
    		return stateDiagram.getStateNames(); 
	    }
	}

    public ArrayList<ArroRequest> getPublishedActions() {
        return stateDiagram.getPublishedActions();
    }


	/*
	 * Pad functions: get / add / remove.
	 */
	
	public ArroPad getPad(String id) {
		return pads.get(id);
	}
	
	/**
	 * Search pad by its given name (not ID).
	 * 
	 * @param name
	 * @return
	 */
	public ArroPad getPadByName(String name) {
		Collection<ArroPad> p = pads.values();
		
		for(ArroPad ref: p) {
			if(ref.getName().equals(name)) {
				return ref;
			}
		}
		return null;
	}

	public void addPad(ArroPad pad) {
		pads.put(pad.getId(), pad);
		pad.setParent(this);
	}

	public void removePad(ArroPad pad) {
		pads.remove(pad.getId());
	}

	/*
	 * Device functions: add.
	 */
	
	public void addDevice(ArroDevice device) {
		this.device = device;
		device.setParent(this);
	}
	

	
	/*
	 * Pad functions: get / add / remove.
	 */
	
	public ArroConnection getConnection(String id) {
		return connections.get(id);
	}
	
	public void addConnection(ArroConnection connection) {
		connections.put(connection.getId(), connection);
		connection.setParent(this);
	}

	public void removeConnection(ArroConnection connection) {
		connections.remove(connection.getId());
	}
	
	/*
	 * StateDiagram functions: get / set
	 */
	public ArroSequenceChart getStateDiagram() {
		return stateDiagram;
	}

	public void setStateDiagram(ArroSequenceChart diag) {
		stateDiagram = diag;
		stateDiagram.setParent(this);
	}
	
	
	/**
	 * Collect all parameter definitions from ArroNode instances in this
	 * diagram. Only parameters that have 'substitute' name are exported.
	 * 
	 */
	public HashMap<String, ArroParameter> getParameterExports() {
		HashMap<String, ArroParameter> all = new HashMap<String, ArroParameter>();
		
		Collection<ArroNode> allNodes = nodes.values();
		
		// collect all parameters
		for(ArroNode n: allNodes) {
			Logger.out.trace(Logger.STD, "Inside " + getName() + " hecking for parameters in node " + n.getName());
			ArrayList<ArroParameter> params = n.getParameterList();
			
			for(ArroParameter p: params) {
				Logger.out.trace(Logger.STD, "From " + n.getName() + " get parameter " + p.getSubstitute());
				if(p.getSubstitute().equals("") == false && all.containsKey(p.getSubstitute()) == false) {
					all.put(p.getSubstitute(), p);
				}
			}
		}
		if(device != null) {
			Logger.out.trace(Logger.STD, "Inside " + getType() + " Checking for parameters in device ");
			ArrayList<ArroParameter> params = device.getParameterList();
			
			for(ArroParameter p: params) {
				Logger.out.trace(Logger.STD, "Get parameter " + p.getSubstitute());
				if(p.getSubstitute().equals("") == false && all.containsKey(p.getSubstitute()) == false) {
					all.put(p.getSubstitute(), p);
				}
			}
		}
		return all;
	}
	
	/**
	 * Write this DomainModule into XML.
	 * 
	 * @param doc
	 * @param elt
	 */
	public void xmlWrite(Document doc, Element elt) {
		Attr attr = null;
		
		attr = doc.createAttribute("id");
		attr.setValue(getId());
		elt.setAttributeNode(attr);
		
		attr = doc.createAttribute("type");
		attr.setValue(getType());
		elt.setAttributeNode(attr);
		
		Collection<ArroNode> col = nodes.values();
		for(ArroNode ref: col) {
			Element sub = doc.createElement("node");
			elt.appendChild(sub);

			ref.xmlWrite(doc, sub);
		}
		
		if(device != null) {
			Element sub = doc.createElement("device");
			elt.appendChild(sub);
			
			device.xmlWrite(doc, sub);
		}
		
		if(stateDiagram != null) {
			Element sub = doc.createElement("sfc");
			elt.appendChild(sub);
			
			stateDiagram.xmlWrite(doc, sub);
		}
		
		Collection<ArroPad> p = pads.values();
		for(ArroPad ref: p) {
			Element sub = doc.createElement("pad");
			elt.appendChild(sub);

			ref.xmlWrite(doc, sub);
		}
		
		Collection<ArroConnection> c = connections.values();
		for(ArroConnection ref: c) {
			Element sub = doc.createElement("connection");
			elt.appendChild(sub);

			ref.xmlWrite(doc, sub);
		}

	}
	
	/**
	 * Read this DomainModule from XML.
	 * 
	 * @param nNode
	 */
	public void xmlRead(Node nNode) {
		Logger.out.trace(Logger.STD, "\nCurrent Element :" + nNode.getNodeName());
 
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
 
			Element eElement = (Element) nNode;
        	setId(eElement.getAttribute("id"));
        	setType(eElement.getAttribute("type"));
        	
    		//POJOIndependenceSolver.getInstance().RegisterPOJOObject(this, diagram);
        	        	
	    	NodeList nList = nNode.getChildNodes();
	    	for (int temp = 0; temp < nList.getLength(); temp++) {
	    		Node sub = nList.item(temp);
	    		
	    		if(sub.getNodeName().equals("node")) {
	    			ArroNode newNode = new ArroNode();

		    		newNode.xmlRead(sub);
		    		// let xmlRead first read the id so the right key is used in nodes.
		    		addNode(newNode);
	    		}
	    		if(sub.getNodeName().equals("sfc")) {
	    		    stateDiagram = new ArroSequenceChart();

	    			stateDiagram.xmlRead(sub);
	    			
	    			setStateDiagram(stateDiagram);
	    		}
	    		if(sub.getNodeName().equals("device")) {
	    			ArroDevice newDevice = new ArroDevice();

	    			newDevice.xmlRead(sub);
		    		// let xmlRead first read the id so the right key is used in nodes.
		    		addDevice(newDevice);
	    		}
	    		if(sub.getNodeName().equals("pad")) {
	    			ArroPad newPad = new ArroPad();

	    			newPad.xmlRead(sub);
		    		// let xmlRead first read the id so the right key is used in nodes.
	    			addPad(newPad);
	    		}
	    		if(sub.getNodeName().equals("connection")) {
	    			ArroConnection newConnection = new ArroConnection();

	    			newConnection.xmlRead(sub);
		    		// let xmlRead first read the id so the right key is used in nodes.
	    			addConnection(newConnection);
	    		}
	    	}
		}
	}

	/**
	 * Find a node in the diagram.
	 * 
	 * @param name
	 * @param class1
	 * @return
	 */
	public NonEmfDomainObject findPOJOObjectByName(String name, Class<?> class1) {
		
//		if(getName().equals(name) /*&& this instanceof class1*/) {
//			return this;			
//		}
//		
		Collection<ArroNode> col = nodes.values();
		
		for(ArroNode ref: col) {
			if(ref.getName().equals(name)) {
				return ref;
			}
		}
		return null;    
	}

	public Object cloneNodeList() {
		return nodes.clone();
	}

	@SuppressWarnings("unchecked")
	public void setNodeList(Object undoList) {
		nodes = (HashMap<String, ArroNode>) undoList;		
	}

	public Object clonePadList() {
		return pads.clone();
	}

	@SuppressWarnings("unchecked")
	public void setPadList(Object undoList) {
		pads = (HashMap<String, ArroPad>) undoList;		
	}

	public final Collection<ArroPad> getPads() {
		return pads.values();
	}

	public Object cloneConnectionList() {
		return connections.clone();
	}
	@SuppressWarnings("unchecked")
	public void setConnectionList(Object undoList) {
		connections = (HashMap<String, ArroConnection>) undoList;		
	}

	public void purgeConnections() {		
		HashSet<String> allPadNames = new HashSet<String>();

		Collection<ArroNode> n = nodes.values();
		for(ArroNode node: n) {
			try {
                Collection<ArroPad> p = node.getAssociatedModule().getPads();
                for(ArroPad pad: p) {
                	allPadNames.add(node.getName() + "#" + pad.getName());
                	Logger.out.trace(Logger.STD, "pad list " + node.getName() + "#" + pad.getName());
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}
		Collection<ArroPad> p = pads.values();
		for(ArroPad pad: p) {
			allPadNames.add(pad.getName());
			Logger.out.trace(Logger.STD, "pad list " + pad.getName());
		}
		
		Collection<ArroConnection> c = connections.values();
		for(ArroConnection connection: c) {
			// if the connection is not connected on both sides, remove it from diagram
			// Note: upon undo the old connections will be reinstated.
			if(!(allPadNames.contains(connection.getSource()) && allPadNames.contains(connection.getTarget()))) {
				connections.remove(connection.getId());
			}
		}
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}

    public boolean hasConnection(ArroPad arroPad) {
        Collection<ArroConnection> c = connections.values();
        for(ArroConnection connection: c) {
            if(connection.getSource().equals(arroPad.getName())) {
                return true;
            }
            if(connection.getTarget().equals(arroPad.getName())) {
                return true;
            }
        }
        return false;
    }

}
