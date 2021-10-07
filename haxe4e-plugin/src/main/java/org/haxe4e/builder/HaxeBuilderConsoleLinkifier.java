/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.builder;

import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.haxe4e.editor.HaxeEditor;
import org.haxe4e.util.LOG;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public class HaxeBuilderConsoleLinkifier implements IPatternMatchListenerDelegate {

   private HaxeBuilderConsole console;

   @Override
   public void connect(final TextConsole console) {
      this.console = (HaxeBuilderConsole) console;
   }

   @Override
   public void disconnect() {
      console = null;
   }

   protected HaxeBuilderConsole getConsole() {
      return console;
   }

   @Override
   public void matchFound(final PatternMatchEvent event) {
      final var offset = event.getOffset();
      final var length = event.getLength();

      try {
         final var doc = console.getDocument();
         final var sourceLoc = doc.get(offset, length); // e.g. src/mypackage/Game.hx:123:
         final var chunks = Strings.split(sourceLoc, ':');
         final var file = console.project.getFile(chunks[0]);
         if (file.exists()) {
            final var link = new FileLink(file, HaxeEditor.class.getName(), -1, -1, Integer.parseInt(chunks[1]));
            console.addHyperlink(link, offset, length - 1);
         }
      } catch (final BadLocationException ex) {
         LOG.debug(ex);
      }
   }

}
