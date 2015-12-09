package util;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.graphiti.mm.pictograms.Diagram;


public class FileUtil {
	public IFile getFile(EObject object) {
		IFile result = null;
		final Resource resource = object.eResource();
		if (resource != null) {
			final ResourceSet resourceSet = resource.getResourceSet();
			if (resourceSet != null) {
				result = getFile(resource.getURI());
			}
		}
		return result;
	}
	

	static public IFile getFile(URI uri) {
		if (uri == null) {
			return null;
		}

		final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

		// File URIs
		final String filePath = getWorkspaceFilePath(uri.trimFragment());
		if (filePath == null) {
			final IPath location = Path.fromOSString(uri.toString());
			final IFile file = workspaceRoot.getFileForLocation(location);
			if (file != null) {
				return file;
			}
			return null;
		}

		// Platform resource URIs
		else {
			final IResource workspaceResource = workspaceRoot.findMember(filePath);
			return (IFile) workspaceResource;
		}
	}

	static public String getWorkspaceFilePath(URI uri) {
		if (uri.isPlatform()) {
			return uri.toPlatformString(true);
		}
		return null;
	}

//	static public void getSomething(IDiagramBehavior diagramBehavior) {
//			EList<Resource> r = diagramBehavior.getEditingDomain().getResourceSet().getResources();
//		
//		URI uri = r.get(0).getURI();
//		
//		uri = uri.trimFileExtension();
//		uri = uri.appendFileExtension("node");
//		String path = uri.path();
//		IFile f = FileUtil.getFile(uri);
//		if (uri.isPlatform()) {
//			String path2 =  uri.toPlatformString(true);
//			Logger.out.trace(Logger.STD, ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
//		}
//		String path3 = FileUtil.getWorkspaceFilePath(uri);
//		if(path3 != null) {
//			Logger.out.trace(Logger.STD, f.getFullPath().toString());
//		}
//	}
//
//
//	private Map<EAttribute, Object> getAttributesWithValues(EObject refObject, boolean b) {
//		final EClass metaObject = refObject.eClass();
//		final EList<EAttribute> attrs = metaObject.getEAllAttributes();
//		final HashMap<EAttribute, Object> result = new HashMap<EAttribute, Object>(attrs.size());
//		for (final EAttribute attr : attrs) {
//			final Object value = refObject.eGet(attr);
//			result.put(attr, value);
//		}
//		return result;
//	}

	public Diagram getDiagramFromFile(IFile file, ResourceSet resourceSet) {
		// Get the URI of the domain file.
		URI resourceURI = getFileURI(file);
		// Demand load the resource for this file.
		Resource resource;
		try {
			resource = resourceSet.getResource(resourceURI, true);
			if (resource != null) {
				// does resource contain a diagram as root object?
				URI diagramUri = mapDiagramFileUriToDiagramUri(resourceURI);
				EObject eObject = resource.getEObject(diagramUri.fragment());
				if (eObject instanceof Diagram)
					return (Diagram) eObject;
			}
		} catch (WrappedException e) {
		}
		return null;
	}


	public URI getFileURI(IFile file) {
		String pathName = file.getFullPath().toString();
		URI resourceURI = URI.createPlatformResourceURI(pathName, true);
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceURI = resourceSet.getURIConverter().normalize(resourceURI);
		return resourceURI;
	}

	public URI mapDiagramFileUriToDiagramUri(URI diagramFileUri) {
		return diagramFileUri.appendFragment("/0"); //$NON-NLS-1$
	}
	
//	public TransactionalEditingDomain createResourceSetAndEditingDomain() {
//		final ResourceSet resourceSet = new ResourceSetImpl();
//		final IWorkspaceCommandStack workspaceCommandStack = new GFWorkspaceCommandStackImpl(new DefaultOperationHistory());
//	
//		final TransactionalEditingDomainImpl editingDomain = new TransactionalEditingDomainImpl(new ComposedAdapterFactory(
//				ComposedAdapterFactory.Descriptor.Registry.INSTANCE), workspaceCommandStack, resourceSet);
//		WorkspaceEditingDomainFactory.INSTANCE.mapResourceSet(editingDomain);
//		return editingDomain;
//	}

}
