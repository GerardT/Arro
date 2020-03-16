package arro.workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.Logger;

/**
 * Class supporting container files (such as zip). By creating an instance it will
 * unpack the contents of the container file and maintain those temp files
 * (starting with "." to make them hidden).
 * 
 */
public class ContainerManager {
	private Map<String, IFile> files = new HashMap<String, IFile>();
	private Map<String, String> meta = new HashMap<String, String>();
	private IFile container;
	private DocumentBuilderFactory builderFactory;
    private IFolder tempFolder;

	/**
	 * Constructor.
	 * Open a container with the specified names in it. If the
	 * container file exists, unpack the names as stated in META file.
	 * 
	 * @param containerFile - zip file or other container format.
	 */
	public ContainerManager(IFile containerFile) {
		container = containerFile;
		
		getMeta(container, meta);
		
		builderFactory = DocumentBuilderFactory.newInstance();

    	if(container.exists()) {
            try {
                tempFolder = getTempFolder();
                if(tempFolder.exists()) {
                    Logger.out.trace(Logger.WS, "Existing temp folder " + tempFolder.getName());
                } else {
                    Logger.out.trace(Logger.WS, "New temp folder " + tempFolder.getName());
                }

                // Open rest
                InputStream source = container.getContents(true);
                InputStreamWrapper in = new InputStreamWrapper(source);
                
                FileEntry entry = in.getNextEntry();
                while(entry != null) {
            		String fileName = entry.getName();
                    IFile file = tempFolder.getFile(fileName);
                    if(true/*files.containsKey(fileName)*/) {
                    	files.put(fileName, file);
                  		
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
            	        byte[] buffer = new byte[1024];
            	        int count;
            	        while ((count = in.read(buffer)) != -1) {
            	            baos.write(buffer, 0, count);
            	        }
            	        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                       
                        // Copy initial contents..
                        if (!file.exists()) {
                            // TODO why not file.create(in, IResource.NONE, null);
                            file.create(bais, IResource.NONE, null);
                        } else {
                        	// don't touch. FIXME: do touch
                        	file.setContents(bais, true, false, null);
                        }
                    }
                	entry = in.getNextEntry();        			
                }
                parseMETA(files.get("META").getContents(), meta);
                
                in.close();
            } catch (CoreException e) {
                throw new RuntimeException(e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
    		}
    	}
    }	


	/**
	 * Change the resource that should be used for zipping / unzipping
	 * from now on.
	 * 
	 * @param res
	 */
    public void changeFile(IResource res) {
        container = (IFile) res;
    }
    
    /**
     * Get Meta data from container file without creating ArroContainerFile object and
     * (thus) unpacking all files from container.
     * 
     * @param container
     * @param metaDb
     */
    public static void getMeta(IFile container, Map<String, String> metaDb) {
        
        if(container.exists()) {
            try {
                InputStream sourceMeta = container.getContents(true);
                
                // Search for META
                InputStreamWrapper inMeta = new InputStreamWrapper(sourceMeta);
                
                FileEntry entryMeta = inMeta.getNextEntry();
                while(entryMeta != null) {
                    if(entryMeta.getName().equals("META")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = inMeta.read(buffer)) != -1) {
                            baos.write(buffer, 0, count);
                        }
                        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                        parseMETA(bais, metaDb);
                    }
                    entryMeta = inMeta.getNextEntry();                  
                }
                inMeta.close();
                
            } catch (CoreException e) {
                throw new RuntimeException(e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }
    
    /**
     * Return Meta property as contained in ArroZipFile object.
     * 
     * @param key
     * @return
     */
    public String getMETA(String key) {
        return meta.get(key);
    }

    /**
     * Set Meta property in ArroZipFile object.
     * 
     * @param key
     * @param value
     */
    public void setMETA(String key, String value) {
        meta.put(key, value);
    }
    


    /**
     * Return the temp folder that contains unzipped content.
     * 
     * @return
     */
	public IFolder getTempFolder() {
	    IFolder folder = (IFolder)container.getParent();
        folder = folder.getFolder("." + meta.get("UUID"));
	    if(!folder.exists()) {
	        try {
	            folder.create(true, true, null);
            } catch (CoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
	    }
	    return folder;
	}
	
    
	/**
	 * Returns specified file that is zipped inside zip file. We
	 * don't 'export' META as a file. Use API instead.
	 * 
	 * @param name
	 * @return
	 */
	public IFile getFile(String name) {	
		if((!name.equals("META")) && files.containsKey(name)) {
			//return folder.getFile(name);
			return files.get(name);
		} else {
			return null;
		}
	}
	
    /**
	 * Save the container file. Leave unpacked files open.
	 */
    public void save() {
		
        byte[] b = new byte[1024];
        int count;

		InputStream source;
		
		saveMETA();

        try {
			
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
					
			OutputStreamWrapper out = new OutputStreamWrapper(bao);
			
			Set<Entry<String, IFile>> set = files.entrySet();
			for(Entry<String, IFile> entry: set) {
	        	// read from temp file
	            source = entry.getValue().getContents(true);

	            // name the file inside the zip file  - use same name as if unzipped
		        out.putNextEntry(new FileEntry(entry.getKey()));
		        
		        while ((count = source.read(b)) > 0) {
		            out.write(b, 0, count);
		        }
				source.close();
			}
	        
	        // close ZIP here in order to flush..
	        out.close();
			
			// Now read the new file again into a resource...
			if (container.exists()) {
				container.setContents(new ByteArrayInputStream(bao.toByteArray()), true, true, null /*monitor*/);
			} else {
				container.create(new ByteArrayInputStream(bao.toByteArray()), true, null /*monitor*/);
			}
			
			bao.close();

			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


	/**
	 * Return filename of zipfile.
	 * 
	 * @return
	 */
	public String getName() {
		return container.getName();
	}


    private static void parseMETA(InputStream fXmlFile, Map<String, String> metaDb) {
        try {
            
            Logger.out.trace(Logger.WS, "Reading META");
            
            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();

            DocumentBuilder dBuilder = bf.newDocumentBuilder();
            // For some reason, DocumentBuilder.parse closes the stream..
            Document doc = dBuilder.parse(fXmlFile);
         
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
         
            NodeList nList = doc.getElementsByTagName("entry");
            for (int temp = 0; temp < nList.getLength(); temp++) {
         
                Node nNode = nList.item(temp);
                
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                     
                    Element eElement = (Element) nNode;
                    String key = eElement.getAttribute("key");
                    String value = eElement.getAttribute("value");
                    metaDb.put(key, value);
                }
            }
        } catch (Exception e) {
            // no file
        }
    }
    
    /**
     * Save key/values in META container entry.
     */
    private void saveMETA() {
        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
                 
            // root elements
            Document doc = builder.newDocument();

            Element elt = doc.createElement("metadata");
            doc.appendChild(elt);
            
            Attr attr = null;
            
            Set<Entry<String, String>> set = meta.entrySet();
            for(Entry<String, String> entry: set) {
                Element sub = doc.createElement("entry");
                elt.appendChild(sub);
                
                attr = doc.createAttribute("key");
                attr.setValue(entry.getKey());
                sub.setAttributeNode(attr);
                
                attr = doc.createAttribute("value");
                attr.setValue(entry.getValue());
                sub.setAttributeNode(attr);
            }
     
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);

            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            Logger.out.trace(Logger.WS, "Saving META");
            
            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream fXmlFile = new ByteArrayOutputStream();

            StreamResult result = new StreamResult(fXmlFile /*new File(fullPath)*/);
            transformer.transform(source, result);
     
            // Output to console for testing
            StreamResult result2 = new StreamResult(System.out);
            transformer.transform(source, result2);
            
            IFile file = files.get("META");

            if (file.exists()) {
                file.setContents(new ByteArrayInputStream(fXmlFile.toByteArray()), true, true, null);
            } else {
                file.create(new ByteArrayInputStream(fXmlFile.toByteArray()), true, null /*monitor*/);
            }
     
            Logger.out.trace(Logger.WS, "File saved!");
     
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}
