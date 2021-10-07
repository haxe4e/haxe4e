/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.builder;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IOConsole;

/**
 * @author Sebastian Thomschke
 */
public class HaxeBuilderConsole extends IOConsole {

   public static class Factory implements IConsoleFactory {
      @Override
      public void openConsole() {
         final var consoleManager = ConsolePlugin.getDefault().getConsoleManager();

         for (final IConsole console : consoleManager.getConsoles()) {
            if (Factory.class.getName().equals(console.getType())) {
               consoleManager.showConsoleView(console);
               return;
            }
         }

         HaxeBuilderConsole.openConsole();
      }
   }

   public static HaxeBuilderConsole openConsole() {
      final var consoleManager = ConsolePlugin.getDefault().getConsoleManager();

      for (final IConsole console : consoleManager.getConsoles()) {
         if (Factory.class.getName().equals(console.getType())) {
            consoleManager.removeConsoles(new IConsole[] {console});
         }
      }

      final var console = new HaxeBuilderConsole("Haxe Builder", Factory.class.getName(), null);
      consoleManager.addConsoles(new IConsole[] {console});
      consoleManager.showConsoleView(console);
      return console;
   }

   public HaxeBuilderConsole(final String name, final String consoleType, final ImageDescriptor imageDescriptor) {
      super(name, consoleType, imageDescriptor);
   }
}
