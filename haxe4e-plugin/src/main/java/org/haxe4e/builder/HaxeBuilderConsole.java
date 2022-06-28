/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.builder;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.MessageConsole;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.builder.HaxeBuilder.Context;

import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.concurrent.Threads;
import net.sf.jstuff.core.io.Processes;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class HaxeBuilderConsole extends MessageConsole {

   public static class Factory implements IConsoleFactory {
      @Override
      public void openConsole() {
         final var consoleManager = ConsolePlugin.getDefault().getConsoleManager();

         for (final var console : consoleManager.getConsoles()) {
            if (HaxeBuilderConsole.class.getName().equals(console.getType())) {
               consoleManager.showConsoleView(console);
               return;
            }
         }
      }
   }

   public static HaxeBuilderConsole openConsole(final HaxeBuilder.Context buildContext) {
      final var consoleManager = ConsolePlugin.getDefault().getConsoleManager();

      for (final var console : consoleManager.getConsoles()) {
         if (Factory.class.getName().equals(console.getType())) {
            consoleManager.removeConsoles(new IConsole[] {console});
         }
      }

      final var console = new HaxeBuilderConsole(buildContext);
      consoleManager.addConsoles(new IConsole[] {console});
      consoleManager.showConsoleView(console);
      return console;
   }

   public static void runWithConsole(final IProject project, final Processes.Builder processBuilder, final IProgressMonitor monitor)
      throws CoreException {

      monitor.setTaskName("Building project '" + project.getName() + "'");

      final var onTerminated = new CompletableFuture<Void>();
      final var console = HaxeBuilderConsole.openConsole(new Context(project, monitor, onTerminated));

      try (var out = console.newMessageStream();
           var err = console.newMessageStream()) {

         UI.run(() -> {
            out.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR));
            err.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR));
         });

         final var startAt = LocalTime.now();
         final var startAtStr = startAt.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_TIME);

         out.println("Building project '" + project.getName() + "'...");
         out.println();

         final var hasOutput = new AtomicBoolean(false);
         final var proc = processBuilder //
            .withWorkingDirectory(project.getLocation().toFile()) //
            .withEnvironment(env -> {
               if (Platform.getBundle("net.mihai-nita.ansicon.plugin") != null) {
                  env.put("ANSICON", "1");
               }
            }) //
            .withRedirectOutput(line -> {
               out.println(line);
               hasOutput.set(true);
            }) //
            .withRedirectError(line -> {
               err.println(line);
               hasOutput.set(true);
            }) //
            .start();

         final var exe = proc.getProcess().info().command().orElse("<unknown>");

         console.setTitle("<running> " + exe + " (" + startAtStr + ")");

         while (proc.isAlive()) {
            /*
             * kill process if job was aborted by user
             */
            if (monitor.isCanceled()) {
               proc.terminate() //
                  .waitForExit(2, TimeUnit.SECONDS) //
                  .kill();
               proc.getProcess().descendants().forEach(ProcessHandle::destroy);
               Threads.sleep(1000);
               proc.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
               err.println("Aborted on user request.");
               break;
            }
            Threads.sleep(500);
         }

         final var endAt = LocalTime.now();
         final var endAtStr = endAt.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_TIME);
         console.setTitle("<terminated> " + exe + " (" + startAtStr + " - " + endAtStr + ")");
         if (monitor.isCanceled())
            return;

         if (hasOutput.get()) {
            out.println();
         }
         if (proc.exitStatus() == 0) {
            out.write("Build successful in ");
         } else {
            out.write("Build");
            out.flush();
            err.write(" failed ");
            out.write("in ");
         }

         var elapsed = ChronoUnit.MILLIS.between(startAt, endAt);
         if (elapsed < 1_000) { // prevent 'Build successful in 0 seconds'
            elapsed = 1_000;
         }
         out.write(DurationFormatUtils.formatDurationWords(elapsed, true, true));
         if (proc.exitStatus() != 0) {
            out.write(" (exit code: " + proc.exitStatus() + ")");
         }
         out.println();

      } catch (final IOException ex) {
         throw new CoreException(Haxe4EPlugin.status().createError(ex, "Failed to run Haxe Builder."));
      } catch (final InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new CoreException(Haxe4EPlugin.status().createError(ex, "Aborted."));
      } finally {
         onTerminated.complete(null);
      }
   }

   public final HaxeBuilder.Context buildContext;

   private HaxeBuilderConsole(final HaxeBuilder.Context buildContext) {
      super("Haxe Builder", HaxeBuilderConsole.class.getName(), null, true);
      this.buildContext = buildContext;
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
