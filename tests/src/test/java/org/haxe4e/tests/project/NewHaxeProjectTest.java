/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.tests.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit5.SWTBotJunit5Extension;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.haxe4e.localization.Messages;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.tests.project.NewHaxeProjectTest.RunTestsOnNonUiThreadExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import net.sf.jstuff.core.exception.Exceptions;
import net.sf.jstuff.core.functional.ThrowingRunnable;

/**
 * @author Sebastian Thomschke
 */
@ExtendWith({SWTBotJunit5Extension.class, RunTestsOnNonUiThreadExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // so @BeforeAll can be non-static
class NewHaxeProjectTest {

   private SWTWorkbenchBot bot;

   @BeforeAll
   void initWorkbench() {
      try {
         SWTBotPreferences.MAX_ERROR_SCREENSHOT_COUNT = 0;
         bot = new SWTWorkbenchBot();
         // close intro view
         bot.viewByTitle("Welcome").close();
      } catch (final Exception ignored) {
         /* ignore, view absent */
      }
   }

   @BeforeEach
   @AfterEach
   void cleanWorkspace() throws CoreException {
      for (final IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects())
         if (p.exists()) {
            p.delete(true, true, null);
         }
   }

   @Test
   void testMenuEntriesAvailable() {
      assertThat(bot.menu("File").menu("New").menu(Messages.Label_Haxe_Project).isEnabled()).isTrue();
      assertThat(bot.menu("File").menu("New").menu(Messages.Label_Haxe_File).isEnabled()).isTrue();
   }

   @Test
   void testNewHaxeProject() throws Exception {
      // launch File -> New -> Haxe Project
      bot.menu("File").menu("New").menu(Messages.Label_Haxe_Project).click();

      // dismiss Haxe not found error dialog
      try { // CHECKSTYLE:IGNORE RequireFailForTryCatchInJunitCheck
         bot.waitUntil(Conditions.shellIsActive(Messages.Prefs_NoSDKRegistered_Title), 3_000);
         bot.button("OK").click();
      } catch (final TimeoutException ex) {
         // ignore
      }

      // dismiss preference dialog
      try { // CHECKSTYLE:IGNORE RequireFailForTryCatchInJunitCheck
         bot.waitUntil(Conditions.shellIsActive("Preferences (Filtered)"), 3_000);
         bot.button("Cancel").click();
      } catch (final TimeoutException ex) {
         // ignore
      }

      // fill in the wizard
      final SWTBotShell wiz = bot.shell(Messages.NewHaxeProject).activate();
      wiz.bot().textWithLabel("Project name:").setText("haxe-project");
      wiz.bot().button("Finish").click();

      // check presence of project
      await(() -> {
         final IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject("haxe-project");
         assertThat(proj.exists()).isTrue();
         assertThat(proj.hasNature(HaxeProjectNature.NATURE_ID)).isTrue();
      }, 10_000);
   }

   boolean await(final ThrowingRunnable<?> condition, final long timeoutMS) {
      final long timeoutNS = TimeUnit.MILLISECONDS.toNanos(timeoutMS);
      final long startTime = System.nanoTime();

      Throwable ex;
      while (true) {
         try {
            condition.runOrThrow();
            return true;
         } catch (final Throwable t) { // CHECKSTYLE:IGNORE IllegalCatch
            ex = t;
         }
         if (System.nanoTime() - startTime >= timeoutNS) {
            Exceptions.throwSneakily(ex);
         }
         SWTUtils.sleep(50);
      }
   }

   /**
    * JUnit 5 extension that ensures test methods never run on the SWT UI thread when launched via Eclipse's "Run > JUnit Plug-in Test" with
    * the default "Run in UI thread" setting.
    * <p>
    * By default, Eclipse Plug-in Tests execute on the UI thread. If you disable that option within Eclipse (unchecking "Run in UI thread"),
    * you may encounter {@code java.lang.IllegalStateException: Workbench has not been created yet}.
    * This extension allows you to keep the default "Run in UI thread" setting while transparently delegating test methods to a background
    * thread.
    * </p>
    * <p>
    * When building via Maven/Tycho, UI-thread execution is already disabled using {@code <useUIThread>false</useUIThread>} in your POM, so
    * this extension has no effect. See the Tycho FAQ for details:
    * https://wiki.eclipse.org/Tycho/FAQ#How_to_use_SWTBot_or_some_UI_tool_for_testing.3F
    * </p>
    */
   static final class RunTestsOnNonUiThreadExtension implements InvocationInterceptor {

      @Override
      public void interceptTestMethod(final Invocation<Void> invocation, final ReflectiveInvocationContext<Method> ictx,
            final ExtensionContext ectx) throws Throwable {

         // If we're on the SWT/UI thread, delegate to a worker thread
         final Display display = Display.getCurrent();
         if (display == null) {
            // Already off UI thread—just run the test normally
            invocation.proceed();
            return;
         }

         final var latch = new CountDownLatch(1);
         final var error = new AtomicReference<Throwable>();
         final var worker = new Thread(() -> {
            try {
               invocation.proceed(); // run the actual test
            } catch (final Throwable t) { // CHECKSTYLE:IGNORE .*
               error.set(t);
            } finally {
               latch.countDown();
            }
         }, "test-worker-thread");
         worker.setDaemon(true);
         worker.start();

         while (latch.getCount() > 0) {
            // Process any pending UI events
            if (!display.readAndDispatch()) {
               display.sleep();
            }
         }

         final var ex = error.get();
         if (ex != null) {
            Exceptions.throwSneakily(ex);
         }
      }
   }
}
