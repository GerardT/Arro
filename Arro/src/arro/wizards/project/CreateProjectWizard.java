package arro.wizards.project;

/*******************************************************************************
 * <copyright>
 *
 * Copyright (c) 2005, 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API, implementation and documentation
 *
 * </copyright>
 *
 *******************************************************************************/

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import arro.Constants;
import arro.builder.ProjectNature;
import util.Misc;
import util.PbScalarTypes;

public class CreateProjectWizard extends BasicNewProjectResourceWizard {

	@Override
	public boolean performFinish() {
		if (!super.performFinish())
			return false;

		IProject newProject = getNewProject();
		try {
			IProjectDescription description = newProject.getDescription();
			description.setNatureIds(new String[] { ProjectNature.NATURE_ID });
			newProject.setDescription(description, null);
			createPredefinedContent(newProject);
		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	private void createPredefinedContent(IProject newProject) throws CoreException {
		IFolder messages = Misc.createFolder(newProject, "messages");
		Misc.createFolder(newProject, Constants.FOLDER_DIAGRAMS);
		Misc.createFolder(newProject, Constants.FOLDER_DEVICES);
		
		String contents = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><message></message>";
		
		String scalars[] = PbScalarTypes.getInstance().getList();
		for(String s: scalars) {			
			InputStream emptyContents =  new ByteArrayInputStream(contents.getBytes());			
			messages.getFile(s + ".amsg").create(emptyContents, true, null);
		}
	}
}
