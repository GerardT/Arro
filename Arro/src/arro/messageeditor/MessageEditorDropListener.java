package arro.messageeditor;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;

import arro.Constants;
import arro.messageeditor.MessageEditor.Message;
import util.Logger;

public class MessageEditorDropListener extends ViewerDropAdapter {

	private final Viewer viewer;
	private final MessageEditor me;

	public MessageEditorDropListener(MessageEditor me, Viewer viewer) {
		super(viewer);
		this.viewer = viewer;
		this.me = me;
	}

	@Override
	public void drop(DropTargetEvent event) {
		int location = this.determineLocation(event);
		Logger.out.trace(Logger.EDITOR, "Event " + event.toString());
		Object target = determineTarget(event);
		if (target instanceof String) {
			String targetStr = (String) target;
			Logger.out.trace(Logger.EDITOR, "The drop was done on the element: " + targetStr);
		} else if (target instanceof Message) {
			Message targetMsg = (Message) target;
			Logger.out.trace(Logger.EDITOR, "The drop was done on the element: "
					+ targetMsg.toString());
		}
		
		if(me.getDocumentType() == Constants.PrimitiveMessageDiagram) {
        	// cannot add anything in device diagram.

        	IStatus status = new Status(IStatus.ERROR, "Arro", /*reason*/"Not allowed to modify protocol buffer scalars types");
			ErrorDialog.openError(null, "Adding element", "Cannot add to diagram", status);

        	return;
		}
		
		String translatedLocation = "";
		switch (location) {
		case 1:
			translatedLocation = "Dropped before the target ";
			break;
		case 2:
			translatedLocation = "Dropped after the target ";
			break;
		case 3:
			translatedLocation = "Dropped on the target ";
			break;
		case 4:
			translatedLocation = "Dropped into nothing ";
			break;
		}
		Logger.out.trace(Logger.EDITOR, translatedLocation);
		super.drop(event);
	}

	// This method performs the actual drop
	// We simply add the String we receive to the domain and trigger a refresh of
	// the
	// viewer by calling its setInput method.
	@Override
	public boolean performDrop(Object data) {
		Logger.out.trace(Logger.EDITOR, "Dropping " + data.toString());

		if (data instanceof String[]) {
			String[] strings = (String[]) data;
			Path p = new Path(strings[0]);
			Logger.out.trace(Logger.EDITOR, "Dropping with success "
					+ p.removeFileExtension().lastSegment());
			IUndoableOperation operation = new DeltaInfoOperation(me, p.removeFileExtension().lastSegment());
			operation.addContext(me.getUndoContext());
			try {
				me.getOperationHistory().execute(operation, null, null);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return true;
		}
		return false;
	}

	@Override
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {
		if (FileTransfer.getInstance().isSupportedType(transferType)) {
			return true;
		} else {
			return false;
		}
	}

	class DeltaInfoOperation extends AbstractOperation {
		MessageEditor me;
		Object undoList, redoList;
		String newType;

		public DeltaInfoOperation(MessageEditor me, String newType) {
			super("Add Messag");
			this.me = me;
			this.newType = newType;
		}

		public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
			undoList = me.cloneMessageList();
			me.addRow(newType);
			return Status.OK_STATUS;
		}

		public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
			redoList = me.cloneMessageList();
			me.setMessageList(undoList);

			return Status.OK_STATUS;
		}

		public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			undoList = me.cloneMessageList();
			me.setMessageList(redoList);

			return Status.OK_STATUS;
		}
	}

}
