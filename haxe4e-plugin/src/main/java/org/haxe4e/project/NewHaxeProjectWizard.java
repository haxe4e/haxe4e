/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.haxe4e.localization.Messages;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.util.LOG;
import org.haxe4e.util.StatusUtils;
import org.haxe4e.util.ui.Dialogs;
import org.haxe4e.util.ui.UI;

import net.sf.jstuff.core.concurrent.Threads;

/**
 * @author Sebastian Thomschke
 */
public final class NewHaxeProjectWizard extends Wizard implements INewWizard {

   private NewHaxeProjectPage newHaxeProjectPage;
   private IProject newProject;

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

               newProject = (IProject) create.getAffectedObjects()[0];
               HaxeProjectNature.addToProject(newProject);
               final var prefs = new HaxeProjectPreference(newProject);
               prefs.setAlternateHaxeSDK(newHaxeProjectPage.selectedAltSDK.get());
               prefs.save();
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
         if (exUnwrapped.getCause() instanceof CoreException) {
            final var exCore = (CoreException) exUnwrapped.getCause();
            if (exCore.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
               status = StatusUtils.createError(exCore, Messages.NewHaxeProject_CaseVariantExistsError, projHandle.getName());
            } else {
               status = StatusUtils.createStatus(exCore.getStatus().getSeverity(), exCore, Messages.NewHaxeProject_UnexpectedError, exCore
                  .getMessage());
               LOG.log(status);
            }
         } else {
            status = StatusUtils.createError(exUnwrapped, Messages.NewHaxeProject_UnexpectedError, exUnwrapped.getMessage());
            LOG.log(status);
         }

         Dialogs.showStatus(Messages.NewHaxeProject_ErrorTitle, status, false);
         return false;
      }

      BasicNewResourceWizard.selectAndReveal(newProject, UI.getActiveWorkbenchWindow());

      return true;
   }
}