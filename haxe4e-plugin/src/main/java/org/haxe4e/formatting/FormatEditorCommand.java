/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.formatting;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;

import de.sebthom.eclipse.commons.ui.Editors;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import formatter.Formatter;
import formatter.FormatterInput;
import formatter.Result;

/**
 * @author Sebastian Thomschke
 */
public final class FormatEditorCommand extends AbstractHandler {

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {
      final var editor = Editors.getActiveTextEditor();
      if (editor == null)
         return null;

      final var doc = Editors.getDocument(editor);
      if (doc == null)
         return null;

      final var file = editor.getEditorInput().getAdapter(IFile.class);

      final var unformatted = doc.get();
      final var result = Formatter.format(new FormatterInput.Code(unformatted), Format.getFormatterConfig(file), null, null, null);
      if (result instanceof Result.Success) {
         final var formatted = ((Result.Success) result).formattedCode;
         if (!unformatted.equals(formatted)) {
            doc.set(formatted);
         }
      } else {
         new NotificationPopup(((Result.Failure) result).errorMessage).open();
      }

      return null;
   }
}
