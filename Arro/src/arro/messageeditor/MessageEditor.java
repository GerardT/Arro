package arro.messageeditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.w3c.dom.Attr;
//public class MessageEditor extends TextEditor {
//
//	// private ColorManager colorManager;
//
//	public MessageEditor() {
//		super();
//		// colorManager = new ColorManager();
//		// setSourceViewerConfiguration(new XMLConfiguration(colorManager));
//		setDocumentProvider(new MsgDocumentProvider());
//	}
//	public void dispose() {
//		// colorManager.dispose();
//		super.dispose();
//	}
//
//}
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import arro.Constants;
import util.ImageUtils;
import util.Logger;
import util.PathUtil;
import util.PbScalarTypes;



public class MessageEditor extends EditorPart {

	public static final String ID = "editor.messageEditor";
	private FileEditorInput fileInput;
	private TableViewer viewer;
	private boolean dirty = false;
	
	private Action doubleClickAction;
	
	private UndoActionHandler undoAction;
	private RedoActionHandler redoAction;
	private IUndoContext undoContext;

	private final Image CHECKED = ImageUtils.getImage("checked.png", MessageEditor.class);
	private final Image UNCHECKED = ImageUtils.getImage("unchecked.png", MessageEditor.class);
	
   	private int documentType = Constants.MessageDiagram;


	public class Message {
		private MessageEditor me;
		private String type;
		private String name;
		private boolean required;
		private String description;

		public Message(MessageEditor me, String type, String name, boolean required, String description) {
			this.me = me;
			this.type = type;
			this.name = name;
			this.required = required;
			this.description = description;
		}
		
		public Message(Message origin) {
			this.me = origin.me;
			this.type = origin.type;
			this.name = origin.name;
			this.required = origin.required;
			this.description = origin.description;
		}

		
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
			me.updateAll();
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
			me.updateAll();
		}
		public boolean getRequired() {
			return required;
		}
		public void setRequired(boolean optional) {
			this.required = optional;
			me.updateAll();
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
			me.updateAll();
		}
	}
	
	private ArrayList<Message> messageList = new ArrayList<Message>();
	
	public Object cloneMessageList() {
		return messageList.clone();
	}

	public void setMessageList(Object messageList) {
		
		this.messageList.clear();
		this.messageList.addAll((ArrayList<Message>)messageList);

		updateAll();
	}

	public void addRow(String newType) {
		messageList.add(new Message(this, newType, "noname", false, "nodescription"));

		updateAll();
	}

	public void delRow(StructuredSelection s) {
		Message m = (Message)s.getFirstElement();
		Logger.out.trace(Logger.EDITOR, "Message for deletion " + m.toString());
		List<Message> lm = s.toList();
		messageList.removeAll(lm);
		
		updateAll();
	}

	public void updateAll() {
		viewer.refresh();
		dirty = true;
		firePropertyChange(PROP_DIRTY);
	    setUndoRedoActionHandlers();
	}


	// Will be called before createPartControl
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
 		//    if (!(input instanceof MessageEditorInput)) {
		//      throw new RuntimeException("Wrong input");
		//    }
		if (!(input instanceof FileEditorInput)) {
			throw new RuntimeException("Wrong input");
		}
		fileInput = (FileEditorInput) input;
		
		if(PbScalarTypes.getInstance().contains(PathUtil.truncExtension(input.getName()))) {
			documentType = Constants.PrimitiveMessageDiagram;
		}

		// read current data from domain
		loadMessages(fileInput.getFile());

		setSite(site);
		setInput(input);
		setPartName(fileInput.getName());

	    undoContext = new ObjectUndoContext(this);
	    undoAction = new UndoActionHandler(getSite(), undoContext);
	    redoAction = new RedoActionHandler(getSite(), undoContext);
	    
		getOperationHistory().setLimit(undoContext, 20);
	}
	
	public int getDocumentType() {
		return documentType;
	}
	


	@Override
	public void createPartControl(Composite parent) {

		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
	    int operations = DND.DROP_COPY | DND.DROP_MOVE ;
	    Transfer[] transferTypes = new Transfer[]{FileTransfer.getInstance()};
	    viewer.addDropSupport(operations, transferTypes, new MessageEditorDropListener(this, viewer));
		
		createColumns(viewer);
		
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setContentProvider(new ArrayContentProvider());
		// get the content for the viewer, setInput will call getElements in the
		// contentProvider
		viewer.setInput(messageList);
		// make the selection available to other views
		getSite().setSelectionProvider(viewer);
		// set the sorter for the table

		// define layout for the viewer
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "Arro.viewer");
////		makeActions();
//		hookContextMenu();
////		hookDoubleClickAction();
//		contributeToActionBars();
	}
	
	// create the columns for the table
	private void createColumns(final TableViewer viewer) {
		String[] titles = { "Type", "Name", "Required", "Description" };
		int[] bounds = { 100, 100, 100, 100 };

		// first column is for the type
		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Message p = (Message) element;
				return p.type;
			}
		});
	    // col.setEditingSupport(new EditingSupportForType(viewer, this));


		// second column is for the last name
		col = createTableViewerColumn(titles[1], bounds[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Message p = (Message) element;
				return p.name;
			}
		});
	    col.setEditingSupport(new EditingSupportForName(viewer, this));

		// now the optional
		col = createTableViewerColumn(titles[2], bounds[2], 2);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}

		    @Override
		    public Image getImage(Object element) {
		      if (((Message) element).required == true) {
		    	  return CHECKED;
		      } else {
		          return UNCHECKED;
		      }
		   }
		});
	    col.setEditingSupport(new EditingSupportForOptional(viewer, this));

	    // now the description
		col = createTableViewerColumn(titles[3], bounds[3], 3);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Message p = (Message) element;
				return p.description;
			}
		});
	    col.setEditingSupport(new EditingSupportForDesc(viewer, this));

	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
				SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}


	
	public IOperationHistory getOperationHistory() {

	  // The workbench provides its own undo/redo manager
	  //return PlatformUI.getWorkbench()
	  //   .getOperationSupport().getOperationHistory();

	  // which, in this case, is the same as the default undo manager
	  return OperationHistoryFactory.getOperationHistory();
	}

	public IUndoContext getUndoContext() {

	  // For workbench-wide operations, we should return
	  //return PlatformUI.getWorkbench()
	  //   .getOperationSupport().getUndoContext();

	  // but our operations are all local, so return our own content
	  return undoContext;
	}

	
	

	
	public void setUndoRedoActionHandlers() {

	    final IActionBars actionBars = getEditorSite().getActionBars();
	    actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
	    actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
	    actionBars.updateActionBars();
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
		
	    setUndoRedoActionHandlers();
	}

	public TableViewer getViewer() {
		return viewer;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		storeMessages(fileInput.getFile(), monitor);
		
		dirty = false;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public void setDirty() {
		dirty = true;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void storeMessages(IFile file, IProgressMonitor monitor) {
		// Build XML document tree..
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = null;

		try {
			builder = builderFactory.newDocumentBuilder();

			// root elements
			Document doc = builder.newDocument();
			Element rootElement = doc.createElement("message");
			doc.appendChild(rootElement);

			for(Message m: messageList) {
				Element elt = doc.createElement("attribute");
				rootElement.appendChild(elt);

				Attr attr = null;

				attr = doc.createAttribute("type");
				attr.setValue(m.type);
				elt.setAttributeNode(attr);
				attr = doc.createAttribute("name");
				attr.setValue(m.name);
				elt.setAttributeNode(attr);
				attr = doc.createAttribute("required");
				attr.setValue(String.valueOf(m.required));
				elt.setAttributeNode(attr);
				attr = doc.createAttribute("description");
				attr.setValue(m.description);
				elt.setAttributeNode(attr);

			}

			// Write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 4);

			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			// String fpath = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(file.getFullPath().toString()).toString();

			Logger.out.trace(Logger.EDITOR, "Saving to " + file.getLocation().toString());

			DOMSource source = new DOMSource(doc);
			
			// temp buffer to store in memory first..
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			// Send output to StreamResult
			StreamResult result = new StreamResult( baos);
			transformer.transform(source, result);
			
			ByteArrayInputStream stream = new ByteArrayInputStream(baos.toByteArray());
			file.setContents(stream, true, false, monitor);

			// Output to console for testing
			StreamResult result2 = new StreamResult(System.out);
			transformer.transform(source, result2);
			

			Logger.out.trace(Logger.EDITOR, "File saved!");

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean loadMessages(IFile file) {
		
//		String fpath = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(file.getFullPath().toString()).toString();

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

	    try {
	    	
	    	Logger.out.trace(Logger.EDITOR, "Loading from " + file.getLocation().toString());

//	    	File fXmlFile = new File(fpath);
	    	DocumentBuilder dBuilder = builderFactory.newDocumentBuilder();
//	    	Document doc = dBuilder.parse(fXmlFile);
	    	Document doc = dBuilder.parse(file.getContents());
	     
	    	//optional, but recommended
	    	//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
	    	doc.getDocumentElement().normalize();
	     
	    	Logger.out.trace(Logger.EDITOR, "Root element :" + doc.getDocumentElement().getNodeName());
	    	
	    	NodeList nList = doc.getElementsByTagName("attribute");
	    	for (int temp = 0; temp < nList.getLength(); temp++) {
	    		Node sub = nList.item(temp);
	    		
	    		if(sub.getNodeName() == "attribute") {
	    			Element eElement = (Element) sub;
	    			
	    			boolean b = eElement.getAttribute("required").equals("true");

	    			Message m = new Message(
	    					this,
	    					eElement.getAttribute("type"),
	    					eElement.getAttribute("name"),
	    					b,
	    					eElement.getAttribute("description"));
	    			
	    			messageList.add(m);
	    		}
	    	}
			
			return true;

	     
	    } catch (Exception e) {
	        return false /* no file */;
        }
	}
} 
//private void hookContextMenu() {
//MenuManager menuMgr = new MenuManager("#PopupMenu");
//menuMgr.setRemoveAllWhenShown(true);
//menuMgr.addMenuListener(new IMenuListener() {
//	public void menuAboutToShow(IMenuManager manager) {
//		MessageEditor.this.fillContextMenu(manager);
//	}
//});
//Menu menu = menuMgr.createContextMenu(viewer.getControl());
//viewer.getControl().setMenu(menu);
//getSite().registerContextMenu(menuMgr, viewer);
//}

///*
//* Seems like we should do this in setFocus!!
//*/
//private void contributeToActionBars() {
//IActionBars bars = getEditorSite().getActionBars();
//fillLocalPullDown(bars.getMenuManager());
//fillLocalToolBar(bars.getToolBarManager());
//}
//
//private void fillLocalPullDown(IMenuManager manager) {
////manager.add(undoAction);
////manager.add(redoAction);
//}
//
//private void fillContextMenu(IMenuManager manager) {
//manager.add(undoAction);
//manager.add(redoAction);
//manager.add(new Separator());
//
//// Other plug-ins can contribute there actions here
//manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
//}
//
//private void fillLocalToolBar(IToolBarManager manager) {
//IContributionItem ci = manager.find("undo");
//manager.add(undoAction);
//manager.add(redoAction);
//}

