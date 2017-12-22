package arro.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import arro.Constants;
import util.Logger;
import util.Misc;
import util.PathUtil;
import util.PbScalarTypes;
import workspace.ArroModuleContainer;

public class ArroBuilder extends IncrementalProjectBuilder {

	/**
	 * For Delta build. Not used right now since always Full build is done.
	 * 
	 */
	class DeltaVisitor implements IResourceDeltaVisitor {
		private BuildInfo buildInfo;
		public DeltaVisitor(BuildInfo buildInfo) {
			this.buildInfo = buildInfo;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		@Override
        public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				//checkXML(resource);
				processResource(resource, buildInfo);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				//checkXML(resource);
				processResource(resource, buildInfo);
				break;
			}
			//return true to continue visiting children.
			return true;
		}
	}

	/**
	 * Resource visitor that is used for Full build. This visitor
	 * will visit all resources in the project.
	 * 
	 */
	class ResourceVisitor implements IResourceVisitor {
		private BuildInfo buildInfo;
		public ResourceVisitor(BuildInfo buildInfo) {
			this.buildInfo = buildInfo;
		}

		@Override
        public boolean visit(IResource resource) {
			Logger.out.trace(Logger.BUILDER, "Try checking resource " + resource.getName());
			//checkXML(resource);
			processResource(resource, buildInfo);
			//return true to continue visiting children.
			return true;
		}
	}

	/**
	 * Special visitor that is used to collect all Message types used in
	 * the project. This is the first pass in the Full build so the second
	 * pass can check Message types used in Function Diagrams. This visitor
	 * will visit all resources in the project.
	 * 
	 */
	class TypeCollectVisitor implements IResourceVisitor {
		private BuildInfo buildInfo;
		public TypeCollectVisitor(BuildInfo buildInfo) {
			this.buildInfo = buildInfo;
		}

		@Override
        public boolean visit(IResource resource) {
			Logger.out.trace(Logger.BUILDER, "Try adding resource " + resource.getName());
			if (resource instanceof IFile && resource.getName().endsWith("." + Constants.MESSAGE_EXT)) {
				buildInfo.add(resource.getName());
			}
			//return true to continue visiting children.
			return true;
		}
	}

	/**
	 * Sample code, should be removed.
	 * 
	 */
	class XMLErrorHandler extends DefaultHandler {
		
		private IFile file;

		public XMLErrorHandler(IFile file) {
			this.file = file;
		}

		private void addMarker(SAXParseException e, int severity) {
			ArroBuilder.this.addMarker(file, e.getMessage(), e
					.getLineNumber(), severity);
		}

		@Override
        public void error(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		@Override
        public void fatalError(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		@Override
        public void warning(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_WARNING);
		}
	}

	public static final String BUILDER_ID = "Arro.sampleBuilder";

	private static final String MARKER_TYPE = "Arro.xmlProblem";

	private SAXParserFactory parserFactory;

	private void addMarker(IFile file, String message, int lineNumber, int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}
	
	/**
	 * BuildInfo is used to pass into visitors; all info used and/or collected
	 * in visitors is kept in BuildInfo.
	 * 
	 */
	class BuildInfo {
		Set<String> types;
		IFolder folder;
		IFile resultFileMessages;
		IFile resultFileNodes;
		IFile resultPython;
		
		BuildInfo(IFolder folder, IFile resultFileMessages, IFile resultFileNodes, IFile resultPython) {
			types = new HashSet<String>();
			this.folder = folder;
			this.resultFileMessages = resultFileMessages;
			this.resultFileNodes = resultFileNodes;
			this.resultPython = resultPython;
		}
		void add(String string) {
			types.add(string);
		}
		boolean has(String string) {
			return types.contains(string);
		}	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 *      
	 * Entry point for the build process. It should depend on kind if full or incremental
	 * build is done. We always do full build.
	 * 
	 * First create 4 files with skeleton info:
	 * arro.proto - for messages
	 * arro.xml- for function diagrams.
	 * arro.html - for webcomponents UI
	 * arro_pgm.py - for.. 
	 * 
	 * Then run sort of a fake build, one that just collects file names in the project. These are
	 * collected in BuildInfo.
	 * 
	 */
	@Override
    @SuppressWarnings("unused")
	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
			throws CoreException {
		
		IFolder folder = Misc.createFolder(getProject(), "build");

		// First collect all types
		// Then do full or incremental build, which checks that referenced types are indeed defined.
	    try {
	        // Write prolog for arro.proto
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(("syntax = \"proto3\";\n" +
						"package arro;\n").getBytes());

			IFile resultFileMessages = folder.getFile("arro.proto");

			if (resultFileMessages.exists()) {
				resultFileMessages.setContents(new ByteArrayInputStream(baos.toByteArray()), true, true, null);
			} else {
				resultFileMessages.create(new ByteArrayInputStream(baos.toByteArray()), true, null);
			}
			baos.close();
			
            // Write prolog for arro.xml
            baos = new ByteArrayOutputStream();
            baos.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                        "<modules>\n").getBytes());
            
            IFile resultFileNodes = folder.getFile("arro.xml");

            if (resultFileNodes.exists()) {
                resultFileNodes.setContents(new ByteArrayInputStream(baos.toByteArray()), true, true, null);
            } else {
                resultFileNodes.create(new ByteArrayInputStream(baos.toByteArray()), true, null);
            }
            baos.close();

            // Write prolog for arro.html
            baos = new ByteArrayOutputStream();
            baos.write((
"<!DOCTYPE html>\n" +
"<meta charset=\"UTF-8\"> \n" +
"<html lang=\"en\">\n" +
"  <head>\n" +
"    <script src=\"https://polygit.org/components/webcomponentsjs/webcomponents-loader.js\"></script>\n" +
"    <link rel=\"import\" href=\"bower_components/paper-button/paper-button.html\">\n" +
"    <link rel=\"import\" href=\"bower_components/paper-checkbox/paper-checkbox.html\">\n" +
"    <link rel=\"import\" href=\"bower_components/paper-slider/paper-slider.html\">\n" +
"    <link rel=\"import\" href=\"bower_components/paper-progress/paper-progress.html\">\n" +
"    <!-- Import all elements here -->\n" +
"    <link rel=\"import\" href=\"arro-slider.html\">\n" +
"    <link rel=\"import\" href=\"arro-toggle-button.html\">\n" +
"    <link rel=\"import\" href=\"arro-radio-button.html\">\n" +
"    <link rel=\"import\" href=\"arro-check-box.html\">\n" +
"    <link rel=\"import\" href=\"arro-progress.html\">\n" +
"    <style>\n" +
"    </style>\n" +
"  </head>\n" +
"  <body>\n" +
"      <!-- Instantiate all elements here -->\n\n").getBytes());
            
            IFile resultFileHtml = folder.getFile("arro.html");

            if (resultFileHtml.exists()) {
                resultFileHtml.setContents(new ByteArrayInputStream(baos.toByteArray()), true, true, null);
            } else {
                resultFileHtml.create(new ByteArrayInputStream(baos.toByteArray()), true, null);
            }
            baos.close();

            // Write prolog for arro_pgm.py
			baos = new ByteArrayOutputStream();
			baos.write(("import sys\n" +
                        "sys.path.append('.')\n\n").getBytes());
	    	
			IFile resultPython = folder.getFile("arro_pgm.py");

			if (resultPython.exists()) {
				resultPython.setContents(new ByteArrayInputStream(baos.toByteArray()), true, true, null);
			} else {
				resultPython.create(new ByteArrayInputStream(baos.toByteArray()), true, null);
			}
			baos.close();

			
            // Write content for arro.proto, arro.xml, arro_pgm.py
			BuildInfo buildInfo = new BuildInfo(folder, resultFileMessages, resultFileNodes, resultPython);
			fullCollect(buildInfo, monitor);
			// for now we always do a full build...
			if (true /* kind == FULL_BUILD */) {
				fullBuild(buildInfo, monitor);
			} else {
				IResourceDelta delta = getDelta(getProject());
				if (delta == null) {
					fullBuild(buildInfo, monitor);
				} else {
					incrementalBuild(buildInfo, delta, monitor);
				}
			}
			
            // Write epilog for arro.xml
			resultFileNodes.appendContents(new ByteArrayInputStream("</modules>\n".getBytes()), true, true, null);

            // Write epilog for arro.html
            resultFileHtml.appendContents(new ByteArrayInputStream((
"    <script type=\"text/javascript\">\n" +
"    // use vanilla JS because why not\n" +
"    mySocket = 0; \n" +
"    window.addEventListener(\"load\", function() {\n" +
"        // create websocket instance\n" +
"        mySocket = new WebSocket(\"ws://\" + location.host + \"/ws\");\n" +
"        // Display output\n" +
"        // add event listener reacting when message is received\n" +
"        mySocket.onmessage = function (event) {\n" +
"            json = JSON.parse(event.data);\n" +
"            address = json.address;\n" +
"            var web_component = document.getElementById(address);\n" +
"            web_component.value = json.data.value;\n" +
"        };\n" +
"    });\n" +
"    </script>\n" +
"  </body>\n" +
"</html>\n").getBytes()), true, true, null);

	    } catch (Exception e) {
	    	e.printStackTrace();
	        //return false /* no file */;
        }

		return null;
	}

	@Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
		// delete markers set and files created
		getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
	}

	void checkXML(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".xml")) {
			IFile file = (IFile) resource;
			deleteMarkers(file);
			XMLErrorHandler reporter = new XMLErrorHandler(file);
			try {
				getParser().parse(file.getContents(true), reporter);
			} catch (Exception e1) {
			}
		}
	}

	/**
	 * Process one resource:
	 * - if a Message then convert XML content of Message file into Protocol buffers
	 *   syntax and append to arro.proto. Sample contents
	 *			message Value {
	 *			   required double value = 1;
	 *		    }
	 *   
	 * - if a Function Diagram then concatenate contents to arro.xml.
	 * 
	 * @param resource
	 * @param buildInfo 
	 */
	private void processResource(IResource resource, BuildInfo buildInfo) {
		if (resource instanceof IFile && resource.getName().endsWith("." + Constants.MESSAGE_EXT)) {
			String filename = PathUtil.truncExtension(resource.getName());
			
			if(!(PbScalarTypes.getInstance().contains(filename))) {
				
				IFile file = (IFile) resource;
				
				deleteMarkers(file);
				DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

			    try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					baos.write(("message " + filename + " {\n").getBytes());		    	

					Logger.out.trace(Logger.BUILDER, "Loading from " + file.getLocation().toString());

			    	DocumentBuilder dBuilder = builderFactory.newDocumentBuilder();
			    	Document doc = dBuilder.parse(file.getContents(true));
			     
			    	//optional, but recommended
			    	//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			    	doc.getDocumentElement().normalize();
			     
			    	NodeList nList = doc.getElementsByTagName("attribute");
			    	int num = 1;
			    	for (int temp = 0; temp < nList.getLength(); temp++) {
			    		Node sub = nList.item(temp);
			    		
			    		if(sub.getNodeName() == "attribute") {
			    			Element eElement = (Element) sub;
			    			
			    			String name = eElement.getAttribute("name");
			    			String type = eElement.getAttribute("type");
			    			// Explicit 'optional' labels are disallowed in the Proto3 syntax.
			    			// String required = eElement.getAttribute("required").equals("true")? "required": "";
			    			String required = "";
			    			if(!(type != null && buildInfo.has(type + "." + Constants.MESSAGE_EXT))) {
			    				addMarker(file, "type not found " + type, 0 /* lineNumber */, IMarker.SEVERITY_ERROR);
			    			} else {
			    				baos.write(("   " + required + " " + type + " " + name + " = " + num++ + ";\n").getBytes());
			    			}
			    		}
			    	}
					baos.write(("}\n\n").getBytes());		    	
			    	
					buildInfo.resultFileMessages.appendContents(new ByteArrayInputStream(baos.toByteArray()), true, true, null);
					baos.close();
			    } catch (Exception e) {
			    	e.printStackTrace();
			        //return false /* no file */;
		        }
			}
		} else if (resource instanceof IFile && 
				(resource.getName().endsWith("." + Constants.NODE_EXT))) {
			// Just concatenate XML files
			IFile file = (IFile) resource;
			
			if(!(file.getName().startsWith(Constants.HIDDEN_RESOURCE))) {
				Logger.out.trace(Logger.BUILDER, "Node to check " + file.getName());
				ArroModuleContainer.unzipAndConcatenateBody(buildInfo.folder, file, Constants.MODULE_FILE_NAME, buildInfo.resultFileNodes, true);
			}

			// Stage .py files
			if(ArroModuleContainer.unzipAndStage(buildInfo.folder, file, Constants.PYTHON_FILE_NAME)) {
				Logger.out.trace(Logger.BUILDER, "Node to check " + file.getName());

				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					String name = PathUtil.truncExtension(file.getName());
					baos.write(("from " + name + " import " + name + "\n").getBytes());
					buildInfo.resultPython.appendContents(new ByteArrayInputStream(baos.toByteArray()), true, true, null);
					baos.close();
				} catch (IOException e) {
					Logger.out.trace(Logger.BUILDER, e.getMessage());
				} catch (CoreException e) {
                    Logger.out.trace(Logger.BUILDER, e.getMessage());
				}
			}
		}
		
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	/**
	 * Run TypeCollectVisitor on all Message resources in order to collect
     * their filenames in BuildInfo.
	 * 
	 * @param buildInfo
	 * @param monitor
	 * @throws CoreException
	 */
	private void fullCollect(BuildInfo buildInfo, final IProgressMonitor monitor) throws CoreException {
		try {
			getProject().accept(new TypeCollectVisitor(buildInfo));
		} catch (CoreException e) {
		}
	}

	/**
	 * Run ResourceVisitor on all resources.
	 * 
	 * @param buildInfo
	 * @param monitor
	 * @throws CoreException
	 */
	private void fullBuild(BuildInfo buildInfo, final IProgressMonitor monitor) throws CoreException {
		try {
			getProject().accept(new ResourceVisitor(buildInfo));
		} catch (CoreException e) {
		}
	}

	/**
	 * Run DeltaVisitor on changed resources.
	 * 
	 * @param buildInfo
	 * @param delta
	 * @param monitor
	 * @throws CoreException
	 */
	private void incrementalBuild(BuildInfo buildInfo, IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new DeltaVisitor(buildInfo));
	}

	private SAXParser getParser() throws ParserConfigurationException,
			SAXException {
		if (parserFactory == null) {
			parserFactory = SAXParserFactory.newInstance();
		}
		return parserFactory.newSAXParser();
	}

}
