package arro;

import org.eclipse.graphiti.util.ColorConstant;
import org.eclipse.graphiti.util.IColorConstant;

public class Constants {
	public static final int LABEL_WIDTH = 100;
	public static final int PAD_SIZE = 14;
	public static final int HALF_PAD_SIZE = PAD_SIZE / 2;

	public final static String HIDDEN_RESOURCE = ".";
	public final static String NODE_EXT = "anod";
	public final static String MESSAGE_EXT = "amsg";
	
	// temp file name after unzip
	public final static String SFC_FILE_NAME = Constants.HIDDEN_RESOURCE + Constants.STATE_NODE_DIAGRAM_TYPE;
	
	// Editor types - Diagram.getDiagramTypeId() will return:
    public final static String FUNCTION_NODE_DIAGRAM_TYPE = "Arro"; // must match with plugin.xml
    public final static String FUNCTION_NODE_DIAGRAM_TYPE_PROVIDER = "Arro.FunctionNodeDiagramTypeProvider"; // must match with plugin.xml

    public final static String FUNCTION_LEAF_DIAGRAM_TYPE = "ArroFunctionLeafDiagram"; // must match with plugin.xml
    public final static String FUNCTION_LEAF_DIAGRAM_TYPE_PROVIDER = "Arro.FunctionLeafDiagramTypeProvider"; // must match with plugin.xml
	
    public final static String STATE_NODE_DIAGRAM_TYPE = "ArroStateDiagram"; // must match with plugin.xml
    public final static String STATE_NODE_DIAGRAM_TYPE_PROVIDER = "Arro.StateDiagramTypeProvider"; // must match with plugin.xml

    public final static String STATE_LEAF_DIAGRAM_TYPE = "ArroSfcLeafDiagram"; // must match with plugin.xml
    public final static String STATE_LEAF_DIAGRAM_TYPE_PROVIDER = "Arro.SfcLeafTypeProvider"; // must match with plugin.xml
    // Provider = Handler object that handles e.g. doc type.
	
	// Document types - defined in META file
	public final static String FUNCTION_BLOCK = "Function";
	public final static String CODE_BLOCK = "Code";
	
	// Document types - derived from META
	public static final int FunctionBlock = 1;
	public static final int CodeBlockPython = 2;
	public static final int CodeBlockNative = 3;
	
	// Document types - messages
	public static final int MessageDiagram = 4;
	public static final int PrimitiveMessageDiagram = 5;
	

	// Properties
	public final static String PROP_PAD_INPUT_KEY = "pad_input";
	
	// Pictogram types - Key
	public final static String PROP_PICT_KEY = "pictogram";
	
	// Pictogram types - Values
	public final static String PROP_PICT_NODE = "node";
	public final static String PROP_PICT_STEP = "state_block";
	public final static String PROP_PICT_TRANSITION = "transition";
	public final static String PROP_PICT_NULL_TRANSITION = "null_transition";
	public final static String PROP_PICT_SYNCHRONIZATION_IN = "synchronization_in";
	public final static String PROP_PICT_SYNCHRONIZATION_OUT = "synchronization_out";
	public final static String PROP_PICT_BOX = "box";
	public final static String PROP_PICT_PAD = "pad";
	public final static String PROP_PICT_CONNECTION = "connection";
	public final static String PROP_PICT_PASSIVE = "passive";
	
	public final static String PROP_TRUE_VALUE = "true";
	public final static String PROP_FALSE_VALUE = "false";
	
	// Undo / redo property keys for domain module
	public static final String PROP_DOMAIN_MODULE_KEY = "domain_node";  // domain diagram ID
	
	public static final String PROP_UNDO_NODE_KEY = "undo_node";
	public static final String PROP_REDO_NODE_KEY = "redo_node";
	public static final String PROP_UNDO_PAD_KEY = "undo_pad";
	public static final String PROP_REDO_PAD_KEY = "redo_pad";
	public static final String PROP_UNDO_CONNECTION_KEY = "undo_connection";
	public static final String PROP_REDO_CONNECTION_KEY = "redo_connection";
	
	public static final String PROP_PAD_NAME_KEY = "pad_name";
	public static final Object PROP_SOURCE_PAD_KEY = "source_pad";
	public static final Object PROP_TARGET_PAD_KEY = "target_pad";

	// Undo / redo property keys for domain sfc
	public static final String PROP_DOMAIN_SFC_KEY = "domain_sfc";  // domain diagram ID
	
	public static final String PROP_UNDO_SFC_KEY = "undo_sfc";
	public static final String PROP_REDO_SFC_KEY = "redo_sfc";

	
	
	// Pre-defined pad names used in SFCs
	public static final String PROP_PAD_NAME_STEP_IN = "Step-in";
	public static final String PROP_PAD_NAME_STEP_OUT = "Step-out";
//	public static final String PROP_PAD_NAME_SYNC_START_IN = "Sync-start-in";
//	public static final String PROP_PAD_NAME_SYNC_START_OUT = "Sync-start-out";
//	public static final String PROP_PAD_NAME_SYNC_STOP_IN = "Sync-stop-in";
//	public static final String PROP_PAD_NAME_SYNC_STOP_OUT = "Sync-stop-out";

	
	public static final IColorConstant PAD_TEXT_FOREGROUND = new ColorConstant(51, 51, 153);
         
	public static final IColorConstant PAD_FOREGROUND_INPUT = new ColorConstant(255, 50, 0);
     
	public static final IColorConstant PAD_FOREGROUND_OUTPUT = new ColorConstant(50, 255, 0);
     
	public static final IColorConstant PAD_BACKGROUND_INPUT = new ColorConstant(255, 153, 153);

	public static final IColorConstant PAD_BACKGROUND_OUTPUT = new ColorConstant(153, 255, 153);

	public static final IColorConstant ANCHOR_FG = new ColorConstant(0, 0, 0);

	public static final IColorConstant ANCHOR_BG = new ColorConstant(200, 200, 200);

    public static final IColorConstant MODULE_GONE = new ColorConstant(200, 0, 0);

    public static final IColorConstant CLASS_TEXT_FOREGROUND = new ColorConstant(51, 51, 153);
         
	public static final IColorConstant CLASS_FOREGROUND = new ColorConstant(255, 102, 0);
     
	public static final IColorConstant CLASS_BACKGROUND = new ColorConstant(255, 204, 153);

	public static final String PLUGIN_ID = "arro";

	public static final String ATTR_LAUNCH_IP_ADDRESS = PLUGIN_ID + ".launch_ip_address";
	public static final String CONSOLE_NAME = "Target";
	public static final String ATTR_LAUNCH_PROJECT = PLUGIN_ID + ".launch_project";

	public static String FOLDER_DEVICES = "diagrams";
	public static String FOLDER_DIAGRAMS = "diagrams";
	public static String FOLDER_MESSAGES = "messages";

	
    public static String NODE_PYTHON = "Python";
    public static String NODE_NATIVE = "Native";



}
