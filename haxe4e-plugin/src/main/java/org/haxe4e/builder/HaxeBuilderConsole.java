/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.MessageConsole;
import org.haxe4e.util.ui.UI;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeBuilderConsole extends MessageConsole {

   public static class Factory implements IConsoleFactory {
      @Override
      public void openConsole() {
         final var consoleManager = ConsolePlugin.getDefault().getConsoleManager();

         for (final IConsole console : consoleManager.getConsoles()) {
            if (HaxeBuilderConsole.class.getName().equals(console.getType())) {
               consoleManager.showConsoleView(console);
               return;
            }
         }
      }
   }

   public static HaxeBuilderConsole openConsole(final IProject project) {
      final var consoleManager = ConsolePlugin.getDefault().getConsoleManager();

      for (final IConsole console : consoleManager.getConsoles()) {
         if (Factory.class.getName().equals(console.getType())) {
            consoleManager.removeConsoles(new IConsole[] {console});
         }
      }

      final var console = new HaxeBuilderConsole(project);
      consoleManager.addConsoles(new IConsole[] {console});
      consoleManager.showConsoleView(console);
      return console;
   }

   public final IProject project;

   private HaxeBuilderConsole(final IProject project) {
      super("Haxe Builder", HaxeBuilderConsole.class.getName(), null, true);
      this.project = project;
   }

   public void setTitle(final String title) {
      UI.run(() -> {
         if (Strings.isEmpty(title)) {
            setName("Haxe Builder");
         } else {
            setName("Haxe Builder: " + title);
         }
      });
   }
}
