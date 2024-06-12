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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;

import de.sebthom.eclipse.commons.ui.DialogPages;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.concurrent.Threads;

/**
 * @author Sebastian Thomschke
 */
public final class NewHaxeFileWizard extends Wizard implements INewWizard {

   private NewHaxeFilePage haxeFilePage = lateNonNull();
   private IStructuredSelection selection = lateNonNull();

   @Override
   public void addPages() {
      haxeFilePage = new NewHaxeFilePage(NewHaxeFilePage.class.getSimpleName(), selection);
      haxeFilePage.setTitle(Messages.Label_Haxe_File);
      haxeFilePage.setDescription(Messages.NewHaxeFile_Descr);
      haxeFilePage.setFileExtension("hx");
      addPage(haxeFilePage);
   }

   @Override
   public void init(final IWorkbench workbench, final IStructuredSelection selection) {
      this.selection = selection;
      setWindowTitle(Messages.NewHaxeFile);
      setNeedsProgressMonitor(true);
   }

   @Override
   public boolean performFinish() {
      final var folderName = haxeFilePage.getContainerFullPath().toOSString();
      final var fileName = asNonNull(haxeFilePage.getFileName());

      try {
         getContainer().run(true, false, progress -> {
            try {
               progress.beginTask(NLS.bind(Messages.NewHaxeFile_Creating, fileName), 2);

               final var folder = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(folderName));
               if (!folder.exists() || !(folder instanceof IContainer))
                  throw new CoreException(Haxe4EPlugin.status().createError(Messages.NewHaxeFile_DirectoryDoesNotExist, folderName));

               final var newHaxeFile = ((IContainer) folder).getFile(new Path(fileName));
               final var newHaxeFileContent = "";
               try (InputStream stream = new ByteArrayInputStream(newHaxeFileContent.getBytes())) {
                  if (newHaxeFile.exists()) {
                     newHaxeFile.setContents(stream, true, true, progress);
                  } else {
                     newHaxeFile.create(stream, true, progress);
                  }
               } catch (final IOException ex) {
                  Haxe4EPlugin.log().error(ex, ex.getMessage());
               }
               progress.worked(1);
               progress.setTaskName(Messages.NewHaxeFile_OpeningInEditor);
               UI.run(() -> {
                  final var page = UI.getActiveWorkbenchPage();
                  try {
                     IDE.openEditor(page, newHaxeFile, true);
                  } catch (final PartInitException ex) {
                     Haxe4EPlugin.log().error(ex, ex.getMessage());
                  }
               });
               progress.worked(1);
            } catch (final CoreException ex) {
               throw new InvocationTargetException(ex);
            } finally {
               progress.done();
            }
         });
      } catch (final InterruptedException ex) {
         Threads.handleInterruptedException(ex);
         return false;
      } catch (final InvocationTargetException ex) {
         Haxe4EPlugin.log().error(ex.getTargetException(), ex.getMessage());
         DialogPages.setMessage(haxeFilePage, Haxe4EPlugin.status().createError(ex.getTargetException()));
         return false;
      }
      return true;
   }
}
