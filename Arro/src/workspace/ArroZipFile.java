package workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
import util.PathUtil;

/**
 * Class supporting zip files. By creating an instance it will
 * upzip the contents of the zip file and maintain those temp files
 * (starting with "." to make them hidden).
 * 
 */
public class ArroZipFile {
	private Map<String, IFile> files = new HashMap<String, IFile>();
	private Map<String, String> meta = new HashMap<String, String>();
	private IFile zipFile;
	private DocumentBuilderFactory builderFactory;
    private IFolder tempFolder;

	/**
	 * Constructor.
	 * Open a zipfile with the specified names in it. If the
	 * zip file exists, unzip only the specified names.
	 * 
	 * @param names
	 * @param zipFile
	 */
	public ArroZipFile(IFile zipFile) {
		this.zipFile = zipFile;
		
		builderFactory = DocumentBuilderFactory.newInstance();

    	if(zipFile.exists()) {
            try {
        		// Create a hidden temp folder where we unzip to.
            	tempFolder = (IFolder)zipFile.getParent();
            	tempFolder = tempFolder.getFolder("." + PathUtil.truncExtension(zipFile.getName()));
            	if(!tempFolder.exists()) {
            		tempFolder.create(true, true, null);
            	}
            	
                InputStream source = zipFile.getContents(true);
                ZipInputStream in = new ZipInputStream(source);
                
                ZipEntry entry = in.getNextEntry();
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
                        	file.create(bais, IResource.NONE, null);
                        } else {
                        	// don't touch. FIXME: do touch
                        	file.setContents(bais, true, false, null);
                        }
                    }
                	entry = in.getNextEntry();        			
                }
            } catch (CoreException e) {
                throw new RuntimeException(e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
    		}
            readMETA();
    	}
    }
	
	private void readMETA() {
	    try {
	    	
	    	Logger.out.trace(Logger.STD, "Reading META");
		    
	    	InputStream fXmlFile = files.get("META").getContents();

	    	DocumentBuilder dBuilder = builderFactory.newDocumentBuilder();
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
	            	meta.put(key, value);
	    		}
	    	}
	    	
	    	if(!meta.containsKey("UUID")) {
                String uuid = UUID.randomUUID().toString();
                meta.put("UUID", uuid);
	    	}
	    } catch (Exception e) {
	    	// no file
        }
	}
	
	public String getMETA(String key) {
		return meta.get(key);
	}
	
	public void setMETA(String key, String value) {
		meta.put(key, value);
	}
	
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

		    Logger.out.trace(Logger.STD, "Saving META");
			
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
	 
			Logger.out.trace(Logger.STD, "File saved!");
	 
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	 * Save the zip file. Leave unzipped files open.
	 */
    public void save() {
		
        byte[] b = new byte[1024];
        int count;

		InputStream source;
		
		saveMETA();

        try {
			
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
					
			ZipOutputStream out = new ZipOutputStream(bao);
			
			Set<Entry<String, IFile>> set = files.entrySet();
			for(Entry<String, IFile> entry: set) {
	        	// read from temp file
	            source = entry.getValue().getContents(true);

	            // name the file inside the zip file  - use same name as if unzipped
		        out.putNextEntry(new ZipEntry(entry.getKey() /*Constants.HIDDEN_RESOURCE + fileName*/));
		        
		        while ((count = source.read(b)) > 0) {
		            out.write(b, 0, count);
		        }
				source.close();
			}
	        
	        // close ZIP here in order to flush..
	        out.close();
			
			// Now read the new file again into a resource...
			if (zipFile.exists()) {
				zipFile.setContents(new ByteArrayInputStream(bao.toByteArray()), true, true, null /*monitor*/);
			} else {
				zipFile.create(new ByteArrayInputStream(bao.toByteArray()), true, null /*monitor*/);
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
	 * Return filename.
	 * 
	 * @return
	 */
	public String getName() {
		return zipFile.getName();
	}

}
