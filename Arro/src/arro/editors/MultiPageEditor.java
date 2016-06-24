package arro.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

import arro.Constants;
import arro.domain.ResourceCache;
import util.ArroZipFile;
import util.PathUtil;

/**
 * An example showing how to create a multi-page editor. This example has 3
 * pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
public class MultiPageEditor extends MultiPageEditorPart implements
        IResourceChangeListener {

    /** The text editor used in page 0. */
    // private TextEditor editor;
    private FunctionDiagramEditor functionEditor;
    private StateDiagramEditor stateEditor;
    private TextEditor pythonEditor;
    
    private ArroZipFile zip = null;

	private int documentType = Constants.FunctionBlock;

    /** The font chosen in page 1. */
    private Font font;

    /** The text widget used in page 2. */
    private StyledText text;

    /**
     * Creates a multi-page editor example.
     */
    public MultiPageEditor() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }
    
    /**
     * Creates page 0 of the multi-page editor, which contains the function diagram.
     */
    void createPage0(String fileName, ArroZipFile zip) {

        IFile file = zip.getFile(Constants.HIDDEN_RESOURCE + fileName);
        if(file != null) {
        	FileEditorInput fei2 = new FileEditorInput(file);
        	

            try {
            	// pass the file so later it knows where to store the .xml file.
                functionEditor = new FunctionDiagramEditor(zip, documentType);
                
                int index = addPage(functionEditor, fei2);
                setPageText(index, "Function Diagram");
            } catch (PartInitException e) {
                ErrorDialog.openError(getSite().getShell(),
                        "Error creating nested text editor", null, e.getStatus());
            }
        }
    }

    /**
     * Creates page 1 of the multi-page editor, which shows the Python code.
     */
    void createPage1(String fileName, ArroZipFile zip) {
        IFile file = zip.getFile(Constants.HIDDEN_RESOURCE + fileName + ".py");
        if(file != null) {
        	FileEditorInput fei2 = new FileEditorInput(file);
        	
    		if(zip.getMETA("type").equals(Constants.CODE_BLOCK)) {
    			
                try {
                	// pass the file so later it knows where to store the .py file.
                    pythonEditor = new TextEditor();
                    
                    int index = addPage(pythonEditor, fei2);
                    setPageText(index, "Python Code");
                } catch (PartInitException e) {
                    ErrorDialog.openError(getSite().getShell(),
                            "Error creating nested text editor", null, e.getStatus());
                }
    		}
        }
    }

    /**
     * Creates page 2 of the multi-page editor, which contains the sequence diagram.
     */
    void createPage2(String fileName, ArroZipFile zip) {

        //IFile file = zip.getFile(Constants.HIDDEN_RESOURCE + fileName);
        IFile file = zip.getFile(Constants.HIDDEN_RESOURCE + Constants.STATE_DIAGRAM_TYPE);
        if(file != null) {
        	FileEditorInput fei2 = new FileEditorInput(file);
        	

            try {
            	// pass the file so later it knows where to store the .xml file.
                stateEditor = new StateDiagramEditor(zip);
                
                int index = addPage(stateEditor, fei2);
                setPageText(index, "Sequence Diagram");
            } catch (PartInitException e) {
                ErrorDialog.openError(getSite().getShell(),
                        "Error creating nested state editor", null, e.getStatus());
            }
        }
    }

    /**
     * Creates page 3 of the multi-page editor, which allows you to change the
     * font used in page 3.
     */
    void createPage3() {

        Composite composite = new Composite(getContainer(), SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setLayout(layout);
        layout.numColumns = 2;

        Button fontButton = new Button(composite, SWT.NONE);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        fontButton.setLayoutData(gd);
        fontButton.setText("Change Font...");

        fontButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                setFont();
            }
        });

        int index = addPage(composite);
        setPageText(index, "Properties");
    }

    /**
     * Creates the pages of the multi-page editor.
     */
    protected void createPages() {
    	// Open the ZIP file and create unzipped files
    	
        // let's here unzip the file and create a FileEditorInput for each of the subfiles.
        FileEditorInput fei = (FileEditorInput) getEditorInput();
        
        // Make this call to do some init inside ResourceCache. It's a bit ugly..
        ResourceCache.getInstance().getDiagramFolder(fei);
        
        String fileName = fei.getFile().getName();
        
        // Unzip Function Diagram file and load domain data in cache.
        zip = ResourceCache.getInstance().getZip(PathUtil.truncExtension(fileName));
        
        if(zip.getMETA("type").equals(Constants.FUNCTION_BLOCK)) {
			documentType = Constants.FunctionBlock;
		} else if(zip.getMETA("type").equals(Constants.CODE_BLOCK)){
	        if(zip.getMETA("language").equals(Constants.NODE_PYTHON)) {
	        	documentType = Constants.CodeBlockPython;
	        } else if(zip.getMETA("language").equals(Constants.NODE_NATIVE)) {
		        documentType = Constants.CodeBlockNative;
	        }
        }

        // Create page 0 containing Graphiti editor. File was just unzipped in ResourceCache.
        createPage0(fei.getName(), zip);
        if(documentType == Constants.FunctionBlock) {
            createPage2(fei.getName(), zip);
        }
        if(documentType == Constants.CodeBlockPython) {
            createPage1(fei.getName(), zip);
        }

        //createPage2();
    }

    
    /**
     * The <code>MultiPageEditorPart</code> implementation of this
     * <code>IWorkbenchPart</code> method disposes all nested editors.
     * Subclasses may extend.
     */
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        
        super.dispose();
    }

    /**
     * Saves the multi-page editor's document.
     */
    public void doSave(IProgressMonitor monitor) {
        int i = 0;
        getEditor(i++).doSave(monitor);
        if(documentType == Constants.FunctionBlock) {
            getEditor(i++).doSave(monitor);
        }
        if(documentType == Constants.CodeBlockPython) {
            getEditor(i++).doSave(monitor);
        }
        
		ResourceCache.getInstance().storeDomainDiagram(zip);
        
        // then zip the whole thing again
        //FileEditorInput fei = (FileEditorInput) getEditorInput();
		
        zip.save();
    }

    /**
     * Saves the multi-page editor's document as another file. Also updates the
     * text for page 0's tab, and updates this multi-page editor's input to
     * correspond to the nested editor's.
     */
    public void doSaveAs() {
		MessageDialog.openInformation(null, "Arro", "SaveAs is not supported");
    	
//        IEditorPart editor = getEditor(0);
//        editor.doSaveAs();
//        setPageText(0, editor.getTitle());
//        setInput(editor.getEditorInput());
    }

    /*
     * (non-Javadoc) Method declared on IEditorPart
     */
    public void gotoMarker(IMarker marker) {
        setActivePage(0);
        IDE.gotoMarker(getEditor(0), marker);
    }

    /**
     * The <code>MultiPageEditor</code> implementation of this method
     * checks that the input is an instance of <code>IFileEditorInput</code>.
     */
    public void init(IEditorSite site, IEditorInput editorInput)
            throws PartInitException {
        if (!(editorInput instanceof IFileEditorInput)
                && !(editorInput instanceof DiagramEditorInput))
            throw new PartInitException("Invalid Input: Must be Arro Diagram");

        super.init(site, editorInput);
        
		setPartName(editorInput.getName());

    }

    /*
     * (non-Javadoc) Method declared on IEditorPart.
     */
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Calculates the contents of page 2 when the it is activated.
     */
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        // if (newPageIndex == 2) {
        // sortWords();
        // }
    }

    /**
     * Closes all project files on project close.
     */
    public void resourceChanged(final IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchPage[] pages = getSite().getWorkbenchWindow()
                            .getPages();
                    for (int i = 0; i < pages.length; i++) {
                        if (((FileEditorInput) functionEditor.getEditorInput())
                                .getFile().getProject()
                                .equals(event.getResource())) {
                            IEditorPart editorPart = pages[i].findEditor(functionEditor
                                    .getEditorInput());
                            pages[i].closeEditor(editorPart, true);
                        }
                    }
                }
            });
        }
    }

    /**
     * Sets the font related data to be applied to the text in page 2.
     */
    void setFont() {
        FontDialog fontDialog = new FontDialog(getSite().getShell());
        fontDialog.setFontList(text.getFont().getFontData());
        FontData fontData = fontDialog.open();
        if (fontData != null) {
            if (font != null)
                font.dispose();
            font = new Font(text.getDisplay(), fontData);
            text.setFont(font);
        }
    }
    
    


}
