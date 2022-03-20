/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public class HaxeProjectSelectionDialog {
   private final ElementListSelectionDialog dialog;

   public HaxeProjectSelectionDialog(final Shell parent) {
      final var haxeProjectIcon = Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_NAVIGATOR_HAXE_PROJECT);
      dialog = new ElementListSelectionDialog(parent, new LabelProvider() {
         @Override
         public Image getImage(final Object element) {
            return haxeProjectIcon;
         }

         @Override
         public String getText(final Object item) {
            return " " + ((IProject) item).getName();
         }
      });
      dialog.setImage(haxeProjectIcon);
      dialog.setTitle("Select a Haxe project");
      dialog.setMessage("Enter a string to filter the project list:");
      dialog.setEmptyListMessage("No Haxe projects found in workspace.");
      setProjects(Projects.getOpenProjectsWithNature(HaxeProjectNature.NATURE_ID));
   }

   public IProject getSelectedProject() {
      return (IProject) dialog.getFirstResult();
   }

   public HaxeProjectSelectionDialog setProjects(final IProject... projects) {
      dialog.setElements(projects);
      return this;
   }

   public HaxeProjectSelectionDialog setProjects(final List<IProject> projects) {
      dialog.setElements(projects.toArray(new IProject[projects.size()]));
      return this;
   }

   public HaxeProjectSelectionDialog setSelectedProject(final IProject project) {
      dialog.setInitialSelections(project);
      return this;
   }

   /**
    * @return null if cancelled
    */
   public IProject show() {
      if (dialog.open() == Window.OK)
         return getSelectedProject();
      return null;
   }
}
