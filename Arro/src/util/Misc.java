package util;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Text;

public class Misc {
	public static IFolder createFolder(IProject project, String name) {
		IFolder f = project.getFolder(name);
		try {
			if(f.exists()) {
				IResource[] members = f.members();
				for(IResource r: members) {
					r.delete(true, null);
				}
			} else {
				f.create(false, true, null);
			}
		} catch (CoreException e) {
		}
		return f;
	}
	
	public static String checkString(Text textBox) {
		String text = textBox.getText();
		if(!(text.equals("") || text.matches("[a-z][A-Za-z0-9]*"))) {
		    MessageDialog.openError(textBox.getShell(),
		            "Wrong input", "Alphanumeric (a-z, A-Z, 0-9) only, starting with lowercase character");
		    textBox.setText("");
		    int end = text.length() - 1;
		    if(end < 0) end = 0;
		    text = text.substring(0, end);
		    textBox.append(text);
		}
		return text;
	}
}

