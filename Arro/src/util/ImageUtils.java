package util;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Since for deployed app the icons have to be part of the bundle, this
 * class helps getting icons out of the bundle at runtime.
 * 
 * Make sure build.properties include icons:
   bin.includes = plugin.xml,\
               META-INF/,\
               icons/,\
               .,\
               src/arro/wizards/messages.properties
 * 
 * @see ArroImageProvider.
 */
public class ImageUtils {
	public static Image getImage(String file, Class<?> cl) {
	    Bundle bundle = FrameworkUtil.getBundle(cl);
	    Path p = new Path("icons/" + file);
	    URL url = FileLocator.find(bundle, p, null);
	    try {
	        url = FileLocator.resolve(url);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    ImageDescriptor imageDcr = ImageDescriptor.createFromURL(url);
	    return imageDcr.createImage();
	}
}
