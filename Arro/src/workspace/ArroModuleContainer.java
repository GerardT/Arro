package workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import arro.Constants;
import arro.domain.ArroModule;
import arro.domain.NonEmfDomainObject;
import arro.editors.MultiPageEditor;
import util.Logger;
import util.PathUtil;

public class ArroModuleContainer extends  ArroZipFile {
    private NonEmfDomainObject domainDiagram = null;
    private MultiPageEditor editor = null;
    
    /* for XML load / store */
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();


    public ArroModuleContainer(IFile zipFile)
    {
        super(zipFile);
        
    }
    
    /**
     * Just for convenience, store the parsed diagram info with the
     * zip file data.
     * 
     * @param diagram
     */
    public void setDomainDiagram(NonEmfDomainObject diagram) {
        domainDiagram = diagram;
    }

    /**
     * See setDomainDiagram.
     * 
     * @return
     */
    public NonEmfDomainObject getDomainDiagram() {
        if(domainDiagram == null) {
            ArroModule module = loadModule(this);
            setDomainDiagram(module);
        }
        return domainDiagram;
    }
    
    /**
     * Save specified zip file, leaving unzipped files open
     * for further editing.
     * 
     * @param zip
     */
    public void storeDomainDiagram() {
        ArroModule dnd = (ArroModule)getDomainDiagram();
        
        storeModule(dnd, this, getName());
    }

    public void updateDependencies() {
        //getDomainDiagram();
        if(domainDiagram != null) {
            domainDiagram.update();
        }
    }

    public String getUuid() {
        return super.getMETA("UUID");
    }

    public MultiPageEditor getEditor() {
        return editor;
    }

    public void setEditor(MultiPageEditor editor) {
        this.editor = editor;
    }


    /**
     * Used during build process. FIXME: might have some improvement.
     * 
     * @param folder
     * @param zipFile
     * @param entryName
     * @param file
     */
    public static void unzipAndConcatenateBody(IFolder folder, IFile zipFile, String entryName, IFile file, boolean skipFirstLine) {
        try {
            InputStream source = zipFile.getContents(true);
            ZipInputStream in = new ZipInputStream(source);
            
            ZipEntry entry = in.getNextEntry();
            while(entry != null) {
                if(entry.getName().equals(entryName)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }
                    String tmp = baos.toString();
                    int firstPosition = 0;
                    if(skipFirstLine) {
                        firstPosition = tmp.indexOf('\n') + 1;
                    }

                    file.appendContents(new ByteArrayInputStream(tmp.substring(firstPosition,  tmp.length()).getBytes()), true, true, null);
                }
                entry = in.getNextEntry();                  
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            // never mind, if temp file existed then just overwrite.
            // e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Used during build process. FIXME: might have some improvement.
     * 
     * @param folder
     * @param zipFile
     * @param entryName
     * @param file
     */
    public static boolean unzipAndStage(IFolder folder, IFile zipFile, String entryName) {
        boolean retval = false;
        try {
            InputStream source = zipFile.getContents(true);
            ZipInputStream in = new ZipInputStream(source);
            IFile pythonFile = folder.getFile(PathUtil.truncExtension(zipFile.getName()) + ".py");
            
            ZipEntry entry = in.getNextEntry();
            while(entry != null) {
                if(entry.getName().equals(entryName)) {
                    retval = true;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }
                    pythonFile.create(new ByteArrayInputStream(baos.toByteArray()), true, null);
                }
                entry = in.getNextEntry();                  
            }
        } catch (CoreException e) {
            retval = false;
        } catch (IOException e) {
            retval = false;
        }
        return retval;
    }

    
    
    private synchronized ArroModule loadModule(ArroModuleContainer zip) {
        // TODO: handle error if zip file removed.
        String fileName = zip.getName();
        String typeName = fileName.substring(0, fileName.indexOf(".anod"));

        ArroModule n = null;
        
        try {
            
            Logger.out.trace(Logger.WS, "Loading for " + zip.getName());
            
            n = new ArroModule();
            // FIXME compare name in file with name passed as parameter.
            n.setType(typeName);


            InputStream fXmlFile = zip.getFile(Constants.MODULE_FILE_NAME).getContents();
            if(fXmlFile == null) {
                // file doesn't exist
                return null;
            }
            DocumentBuilder dBuilder = builderFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
         
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
         
            Logger.out.trace(Logger.WS, "Root element :" + doc.getDocumentElement().getNodeName());
            
            NodeList nList = doc.getElementsByTagName("module");
            for (int temp = 0; temp < nList.getLength(); temp++) {
         
                Node nNode = nList.item(temp);
                
                // FIXME we expect only one node!
                n.xmlRead(nNode);
            }
        } catch (Exception e) {
            /* no file */;
            throw new RuntimeException("Error " + e.getMessage() + " for type "  + typeName);
        }
        return n;
    }

    private void storeModule(ArroModule domainModule, ArroModuleContainer zip, String fileName) {
        DocumentBuilder builder = null;
        
        try {
            builder = builderFactory.newDocumentBuilder();
                 
            // root elements
            Document doc = builder.newDocument();

            Element elt = doc.createElement("module");
            doc.appendChild(elt);
            
            domainModule.xmlWrite(doc, elt);
     
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);

            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            Logger.out.trace(Logger.WS, "Saving to " + fileName);
            
            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream fXmlFile = new ByteArrayOutputStream();

            StreamResult result = new StreamResult(fXmlFile /*new File(fullPath)*/);
            transformer.transform(source, result);
     
            // Output to console for testing
            StreamResult result2 = new StreamResult(System.out);
            transformer.transform(source, result2);

            IFile f = zip.getFile(Constants.MODULE_FILE_NAME);
            f.setContents(new ByteArrayInputStream(fXmlFile.toByteArray()), true, true, null);
     
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
