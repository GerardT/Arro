package workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import arro.Constants;
import arro.domain.ArroModule;
import util.Logger;
import util.PathUtil;

/**
 * Simple cache for ModuleContainer instances. Such instances will keep
 * their zipped files unzipped and editable as long as the instance
 * exists. Cleanup is done when??
 * 
 */
public class ResourceCache {
    private static ResourceCache myCache = null;
    private ConcurrentHashMap<String, ArroModuleContainer> cache;
    private Lock lock;
    
    /* for XML load / store */
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    /**
     * Constructor, init the cache.
     */
    public ResourceCache() {
        lock = new ReentrantLock();
        cache = new ConcurrentHashMap<String, ArroModuleContainer>();
        
        loadResourcesFromWorkspace();
    }
    
    /**
     *  Load zipped modules in workspace, unzip and read META file.
     * - <typeName>.anod into .<typeName>.anod and .<typeName>.anod.xml
     * @return 
     */ 
    public void loadResourcesFromWorkspace() {
        if(lock.tryLock()) {
            // build a map of all files in workspace
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            IProject[] projects = workspaceRoot.getProjects();
            
            for(IProject p: projects) {
                IResource r = p.getFolder("diagrams");
                try {
                    r.accept(new IResourceVisitor() {

                        @Override
                        public boolean visit(IResource r) throws CoreException {
                            String typeName = r.getName();
                            int ix = typeName.indexOf(".anod");
                            if(!typeName.startsWith(".") && ix > 0) {
                                typeName = typeName.substring(0, ix);
                                if(!cache.containsKey(typeName)) {
                                    ArroModuleContainer zip = new ArroModuleContainer((IFile) r);
                                    cache.put(PathUtil.truncExtension(typeName), zip);  
                                    Logger.out.trace(Logger.WS, typeName + " was added.");
                                }
                            }
                            return true;
                        }
                    });
                } catch (CoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            lock.unlock();
        }
    }
    
    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }


    /**
     * Note, by updating and saving into the resource, the update will trigger an
     * update in the Graphiti diagram as well.
     */
     public void updateDependents() {
         Collection<ArroModuleContainer> zips = cache.values();
         for(ArroModuleContainer zip: zips) {
             getZip(zip.getName());
             zip.updateDependencies();
         }
             
     }


    /**
     * Then read the domain file (.<typeName>.anod) into a DomainModule
     * instance and register this instance in the cache.
     * 
     * FIXME: must search all resources in the open project.
     */
    public ArroModuleContainer getZip(String typeName) throws RuntimeException {
        loadResourcesFromWorkspace();
        return cache.get(typeName);
    }
    
    /**
     * Get singleton object.
     * 
     * @return
     */
    public static ResourceCache getInstance() {
        if(myCache == null) {
            myCache = new ResourceCache();
        }
        return myCache;
    }

    /**
     * Open domain file <typeName> by unzipping it:
     * - <typeName>.anod into .<typeName>.anod and .<typeName>.anod.xml
     * Zip file also contains other stuff, but we only need module here.
     * 
     * Then read the domain file (.<typeName>.anod) into a DomainModule
     * instance and register this instance in the cache.
     * 
     * FIXME: must search all resources in the open project.
     * 
     * META contains UUID.
     * If a module (zip file) is added, then all other modules are checked:
     * - if they reference the UUID inside the added module (so the added module was renamed / moved)
     * - otherwise, if they reference the module name. If so then the referred UUID is used as reference from now on.
     */
    public void removeFromCache(String typeName) throws RuntimeException {

        if(cache.containsKey(typeName)) {
            cache.remove(typeName);
            // and remove unzipped files
        }
    }

    public ArroModuleContainer getZipByUuid(String uuid) {
        Collection<ArroModuleContainer> zips = cache.values();
        for(ArroModuleContainer zip: zips) {
            if(zip.getMETA("UUID") == uuid) {
                return zip;
            }
        }
        return null;
    }
}

