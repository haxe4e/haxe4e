/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.builder;

import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.editor.HaxeEditor;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeBuilderConsoleLinkifier implements IPatternMatchListenerDelegate {

   private @Nullable HaxeBuilderConsole console;

   @Override
   public void connect(final TextConsole console) {
      this.console = (HaxeBuilderConsole) console;
   }

   @Override
   public void disconnect() {
      console = null;
   }

   @Override
   public void matchFound(final PatternMatchEvent event) {
      final var console = this.console;
      if (console == null)
         return;

      final var offset = event.getOffset();
      final var length = event.getLength();
      final var doc = console.getDocument();
      try {
         final var sourceLoc = doc.get(offset, length); // e.g. src/mypackage/Game.hx:123:
         final var chunks = Strings.split(sourceLoc, ':');
         final var file = console.buildContext.project.getFile(chunks[0]);
         if (file.exists()) {
            final var link = new FileLink(file, HaxeEditor.ID, -1, -1, Integer.parseInt(chunks[1]));
            console.addHyperlink(link, offset, length - 1);
         }
      } catch (final BadLocationException ex) {
         Haxe4EPlugin.log().debug(ex);
      }
   }
}
