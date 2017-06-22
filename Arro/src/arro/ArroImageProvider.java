package arro;

import org.eclipse.graphiti.ui.platform.AbstractImageProvider;

import util.ImageUtils;


/**
 * Utility class to provide a icon location to Graphiti.
 * @see ImageUtils
 * not sure when to use ImageUtils and when this.
 */
public class ArroImageProvider extends AbstractImageProvider {

    // The prefix for all identifiers of this image provider
    protected static final String PREFIX = "arro.";
 
    // The image identifier for an EReference.
    public static final String IMG_NEED_UPDATE= PREFIX + "need_update";
    public static final String IMG_CHECKED    = PREFIX + "checked.png";
 
    @Override
    protected void addAvailableImages() {
        // register the path for each image identifier
    	//int resolution = Toolkit.getDefaultToolkit().getScreenResolution();

        addImageFilePath(IMG_NEED_UPDATE, "icons/sample.gif");
    }


}
