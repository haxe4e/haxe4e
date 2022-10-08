/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.formatting;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.haxe4e.Haxe4EPlugin;

import de.sebthom.eclipse.commons.ui.Editors;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import formatter.CodeOrigin;
import formatter.Formatter;
import formatter.FormatterInput;
import formatter.Result;
import formatter.codedata.FormatterInputRange;
import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class FormatEditorCommand extends AbstractHandler {

   @Override
   public @Nullable Object execute(final ExecutionEvent event) throws ExecutionException {
      final var editor = Editors.getActiveTextEditor();
      if (editor == null)
         return null;

      final var doc = Editors.getDocument(editor);
      if (doc == null)
         return null;

      try {
         final var sourceCode = doc.get();

         final var formatterConfig = Format.getFormatterConfig(editor.getEditorInput().getAdapter(IFile.class));

         final var selProvider = editor.getSelectionProvider();
         final var sel = (ITextSelection) selProvider.getSelection();
         final var isSelection = sel.getLength() > 0 && sel.getLength() < doc.getLength();

         if (isSelection) {
            /*
             * format selection
             */
            final var range = new FormatterInputRange( //
               sel.getOffset() + sel.getLength(), // end pos
               doc.getLineOffset(sel.getStartLine()) // start pos = beginning of the first selected line
            );

            final var result = Formatter.format( //
               new FormatterInput.Code(sourceCode, CodeOrigin.Snippet), //
               formatterConfig, //
               Strings.getNewLineSeparator(sourceCode), //
               null, //
               range //
            );

            if (result instanceof final Result.Success successResult) {
               final var formatted = successResult.formattedCode;
               if (!sel.getText().equals(formatted)) {
                  final var sel_startLineOffset = doc.getLineOffset(sel.getStartLine());
                  final var sel_offset = sel.getOffset();
                  doc.replace(sel_startLineOffset, sel.getLength() + sel.getOffset() - sel_startLineOffset, formatted);
                  selProvider.setSelection(new TextSelection(sel_offset, formatted.length()));
               }
            } else {
               new NotificationPopup(((Result.Failure) result).errorMessage).open();
            }

         } else {
            /*
             * format all
             */
            final var result = Formatter.format( //
               new FormatterInput.Code(sourceCode, CodeOrigin.Snippet), //
               formatterConfig, //
               Strings.getNewLineSeparator(sourceCode), //
               null, //
               null //
            );

            if (result instanceof final Result.Success successResult) {
               final var formatted = successResult.formattedCode;
               if (!sourceCode.equals(formatted)) {
                  final var selStartLine = sel.getStartLine();
                  doc.set(formatted);
                  selProvider.setSelection(new TextSelection(doc.getLineOffset(Math.min(selStartLine, doc.getNumberOfLines() - 1)), 0));
               }
            } else {
               new NotificationPopup(((Result.Failure) result).errorMessage).open();
            }
         }

      } catch (final BadLocationException ex) {
         Haxe4EPlugin.log().error(ex);
         new NotificationPopup(ex.getClass().getSimpleName() + ": " + ex.getMessage()).open();
      }

      return null;
   }
}
