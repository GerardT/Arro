package property;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ColumnLabelProvider;

/**
 * Use this interface together with ILabelProvider that defines
 * getText API.
 *
 */
public class ColumnLabelStrategy extends ColumnLabelProvider {
	/**
	 * Implementer must provide specific implementation that updates the
	 * proper field in of a structure (or class).
	 * 
	 * @param value
	 * @param element
	 */
	public void setText(String value, Object element) {
	}
	
	/**
	 * If this function returns non-null then the user is presented with a pull-down listbox to choose from.
	 * 
	 * @return
	 */
	public String[] getAcceptedValues(Object element) {
		return null;
	}

	/**
	 * This function is called if a table value could not be found in getAcceptedValues return. So
	 * apparently this value was removed somehow. We add it to accepted values as being obsolete.
	 * 
	 * @param element
	 * @return
	 */
    public String obsolete(Object element) {
        return "Obsolete";
    }

}
