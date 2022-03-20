/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.formatting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.haxe4e.Haxe4EPlugin;

import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import formatter.CodeOrigin;
import formatter.Formatter;
import formatter.FormatterInput;
import formatter.Result;
import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class FormatFileCommand extends AbstractHandler {

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {
      final var window = UI.getActiveWorkbenchWindow();
      if (window != null) {
         final var selection = (IStructuredSelection) window.getSelectionService().getSelection();
         final var job = Job.create("Formatting Haxe source files...", monitor -> {
            final var haxeFiles = getSelectedHaxeFiles(selection);
            if (haxeFiles.isEmpty()) {
               UI.run(() -> {
                  final var popup = new NotificationPopup("[WARNING] No formattable source files found!");
                  popup.setDelayClose(1000);
                  popup.open();
               });
               return;
            }
            final var subMonitor = SubMonitor.convert(monitor, selection.size());
            for (final var haxeFile : haxeFiles) {
               formatFile(haxeFile, subMonitor);
               subMonitor.worked(1);
            }
            subMonitor.done();
            UI.run(() -> {
               final var popup = new NotificationPopup("Formatting " + haxeFiles.size() + " source file(s) done.");
               popup.setDelayClose(1000);
               popup.open();
            });
         });
         job.setPriority(Job.BUILD);
         job.schedule();
      }
      return null;
   }

   private void formatFile(final IFile file, final IProgressMonitor monitor) {
      if (!file.exists())
         return;

      monitor.setTaskName("Formatting " + file.getLocation() + "...");
      try (var in = file.getContents()) {
         final var unformatted = IOUtils.toString(in, file.getCharset());
         final var formatterConfig = Format.getFormatterConfig(file);

         final var result = Formatter.format( //
            new FormatterInput.Code(unformatted, CodeOrigin.SourceFile(file.getFullPath().toOSString())), //
            formatterConfig, //
            Strings.getNewLineSeparator(unformatted), //
            null, //
            null //
         );
         if (result instanceof Result.Success) {
            final var formatted = ((Result.Success) result).formattedCode;
            if (!unformatted.equals(formatted)) {
               file.setContents(new ByteArrayInputStream(formatted.getBytes(file.getCharset())), true, true, monitor);
            }
         } else {
            new NotificationPopup(((Result.Failure) result).errorMessage).open();
         }
      } catch (IOException | CoreException ex) {
         Haxe4EPlugin.log().error(ex);
         new NotificationPopup(ex.getMessage()).open();
      }
   }

   private List<IFile> getSelectedHaxeFiles(final IStructuredSelection selection) throws CoreException {
      final var haxeFiles = new ArrayList<IFile>();
      for (final Object item : selection) {
         if (item instanceof IFile) {
            haxeFiles.add((IFile) item);
         } else if (item instanceof IFolder) {
            final var folder = (IFolder) item;
            folder.accept(res -> {
               if (res.isLinked() || res.isVirtual())
                  return false;

               if (res instanceof IFile) {
                  final var file = (IFile) res;
                  if ("org.haxe4e.content.haxe".equals(file.getContentDescription().getContentType().getId())) {
                     haxeFiles.add(file);
                  }
               }
               return true;
            });
         }
      }
      return haxeFiles;
   }
}
