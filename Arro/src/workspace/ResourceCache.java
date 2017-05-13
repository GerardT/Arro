package workspace;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

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
        removeTempFiles();
        lock = new ReentrantLock();
        cache = new ConcurrentHashMap<String, ArroModuleContainer>();
        
        loadResourcesFromWorkspace();
    }
    
    /**
     *  Load zipped modules in workspace, unzip and read META file.
     * - <typeName>.anod into .<typeName>.anod and .<typeName>.anod.xml
     * @return 
     */ 
    private void loadResourcesFromWorkspace() {
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
                        if(!typeName.startsWith(".")) {
                            int ix = typeName.indexOf(".anod");
                            if(ix > 0) {
                                typeName = typeName.substring(0, ix);
                                addToCache(typeName, r);
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
    }
    
    private void removeTempFiles() {
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
                        if(typeName.startsWith(".")) {
                            r.delete(true, null);
                        }
                        return true;
                    }
                });
            } catch (CoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public synchronized void addToCache(String typeName, IResource res) throws RuntimeException {
        if(lock.tryLock()) {
            Map<String, String> meta = new HashMap<String, String>();
            
            ArroZipFile.getMeta((IFile) res, meta );
            String uuid = meta.get("UUID");
            
            // now check if this entry is already in list
            ArroModuleContainer zip = getZipByUuid(uuid);
            if(zip == null) {
                zip = new ArroModuleContainer((IFile) res);
                cache.put(PathUtil.truncExtension(typeName), zip);  
                Logger.out.trace(Logger.WS, "New " + typeName + " was added.");
                updateDependents();
            } else if(!PathUtil.truncExtension(zip.getName()).equals(typeName)) {
                // If renamed, first remove old name
                cache.remove(PathUtil.truncExtension(zip.getName()));
                Logger.out.trace(Logger.WS, "Existing " + PathUtil.truncExtension(zip.getName()) + " was removed.");
                
                // Change zip file in ArroZip
                zip.changeFile(res);
                zip.setMETA("name", PathUtil.truncExtension(zip.getName()));
                cache.put(PathUtil.truncExtension(typeName), zip);  
                Logger.out.trace(Logger.WS, "Existing " + typeName + " was added.");
                if(zip.getEditor() != null) {
                    zip.getEditor().changeInput(res);
                }
                updateDependents();
            }
            lock.unlock();
        }
    }
    
    public synchronized void removeFromCache(String typeName, IResource res) throws RuntimeException {
        if(cache.containsKey(typeName)) {
            // remove unzipped files
//            ArroModuleContainer c = cache.get(typeName);
//            c.cleanup();
//            
//            cache.remove(typeName);
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
             //getZip(zip.getName());
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
        //loadResourcesFromWorkspace();
        return cache.get(typeName);
    }
    
    public ArroModuleContainer getZipByFile(IFile zipFile) {
        Map<String, String> meta = new HashMap<String, String>();
        
        String fileName = zipFile.getName();
        ArroModuleContainer zip = new ArroModuleContainer(zipFile);
        cache.put(PathUtil.truncExtension(fileName), zip);  
        Logger.out.trace(Logger.WS, "New " + fileName + " was added.");
        return zip;
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

    public ArroModuleContainer getZipByUuid(String uuid) {
        Collection<ArroModuleContainer> zips = cache.values();
        for(ArroModuleContainer zip: zips) {
            String value = zip.getMETA("UUID");
            if(value != null && value.equals(uuid)) {
                return zip;
            }
        }
        return null;
    }
}

