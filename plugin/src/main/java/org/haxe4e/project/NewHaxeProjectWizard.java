/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.prefs.HaxeProjectPreference;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.concurrent.Threads;

/**
 * @author Sebastian Thomschke
 */
public final class NewHaxeProjectWizard extends Wizard implements INewWizard {

   private NewHaxeProjectPage newHaxeProjectPage = lateNonNull();
   private @Nullable IProject newProject;

   @Override
   public void addPages() {
      newHaxeProjectPage = new NewHaxeProjectPage(NewHaxeProjectPage.class.getSimpleName());
      newHaxeProjectPage.setTitle(Messages.Label_Haxe_Project);
      newHaxeProjectPage.setDescription(Messages.NewHaxeProject_Descr);
      addPage(newHaxeProjectPage);
   }

   @Override
   public void init(final IWorkbench workbench, final IStructuredSelection selection) {
      setWindowTitle(Messages.NewHaxeProject);
      setNeedsProgressMonitor(true);
   }

   @Override
   public synchronized boolean performFinish() {
      if (newProject != null)
         return true;

      final var projHandle = newHaxeProjectPage.getProjectHandle();
      final var projConfig = ResourcesPlugin.getWorkspace().newProjectDescription(projHandle.getName());

      // configure target root folder
      if (!newHaxeProjectPage.useDefaults()) {
         projConfig.setLocationURI(newHaxeProjectPage.getLocationURI());
      }

      try {
         getContainer().run(true, true, monitor -> {
            try {
               final var create = new CreateProjectOperation(projConfig, Messages.NewHaxeProject);
               create.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));

               final var newProject = this.newProject = (IProject) asNonNullUnsafe(create.getAffectedObjects())[0];
               HaxeProjectNature.addToProject(newProject);
               final var prefs = HaxeProjectPreference.get(newProject);
               prefs.setAlternateHaxeSDK(newHaxeProjectPage.selectedAltSDK.get());
               prefs.save();

               // may want system to create different types of projects, for now this is better than empty
               createFileFromResource("templates/new-project/default/build.hxml", newProject, "build.hxml", monitor);
               createFileFromResource("templates/new-project/default/src/Main.hx", newProject, "src/Main.hx", monitor);

               newProject.open(monitor);
            } catch (final Exception ex) {
               throw new InvocationTargetException(ex);
            }
         });
      } catch (final InterruptedException ex) {
         Threads.handleInterruptedException(ex);
         return false;
      } catch (final InvocationTargetException ex) {
         final var exUnwrapped = ex.getTargetException();
         final IStatus status;
         if (exUnwrapped.getCause() instanceof final CoreException exCore) {
            if (exCore.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
               status = Haxe4EPlugin.status().createError(exCore, Messages.NewHaxeProject_CaseVariantExistsError, projHandle.getName());
            } else {
               status = Haxe4EPlugin.status().createStatus(exCore.getStatus().getSeverity(), exCore,
                  Messages.NewHaxeProject_UnexpectedError, exCore.getMessage());
               Haxe4EPlugin.log().log(status);
            }
         } else {
            status = Haxe4EPlugin.status().createError(exUnwrapped, Messages.NewHaxeProject_UnexpectedError, exUnwrapped.getMessage());
            Haxe4EPlugin.log().log(status);
         }

         Dialogs.showStatus(Messages.NewHaxeProject_ErrorTitle, status, false);
         return false;
      }

      BasicNewResourceWizard.selectAndReveal(newProject, UI.getActiveWorkbenchWindow());
      return true;
   }

   private void createFileFromResource(final String resourceName, final IProject project, final String to,
      final @Nullable IProgressMonitor monitor) throws CoreException, IOException {
      createFileFromResource(resourceName, project, to, false, monitor);
   }

   private void createFileFromResource(final String resourceName, final IProject project, final String to, final boolean isBinary,
      final @Nullable IProgressMonitor monitor) throws CoreException, IOException {
      final var f = project.getFile(to);
      createParents(f, monitor);

      if (isBinary) {
         try (var is = Haxe4EPlugin.resources().getAsStream(resourceName)) {
            f.create(is, true, monitor);
         }
      } else {
         f.create(new ByteArrayInputStream(Haxe4EPlugin.resources().getAsString(resourceName).getBytes()), true, monitor);
      }
   }

   private void createParents(final IResource res, final @Nullable IProgressMonitor monitor) throws CoreException {
      if (!res.exists()) {
         if (res.getParent() instanceof @NonNull final IFolder folder) {
            createParents(folder, monitor);
            folder.create(true, true, monitor);
         }
      }
   }
}
