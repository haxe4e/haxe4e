/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util.ui;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.ref.ObservableRef;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public final class UI {

   public static final Font DEFAULT_FONT_BOLD = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
   public static final Styler DEFAULT_FONT_BOLD_STYLER = new Styler() {
      @Override
      public void applyStyles(final TextStyle textStyle) {
         textStyle.font = DEFAULT_FONT_BOLD;
      }
   };

   /**
    * two-way bind
    */
   public static <E> void bind(final Text widget, final ObservableRef<E> model, final Function<String, E> widget2model,
      final Function<E, String> model2widget) {
      Args.notNull("widget", widget);
      Args.notNull("model", model);
      Args.notNull("widget2model", widget2model);
      Args.notNull("model2widget", model2widget);

      final var initialVal = model.get();
      if (initialVal != null) {
         widget.setText(model2widget.apply(initialVal));
      }
      model.subscribe(newValue -> run(() -> {
         final var oldTxt = widget.getText();
         var newTxt = model2widget.apply(newValue);
         if (newTxt == null) {
            newTxt = "";
         }
         if (!Objects.equals(oldTxt, newTxt)) {
            widget.setText(newTxt);
         }
      }));
      widget.addModifyListener(ev -> model.set(widget2model.apply(widget.getText())));
   }

   /**
    * two-way bind
    */
   public static void bind(final Text widget, final ObservableRef<String> model) {
      Args.notNull("widget", widget);
      Args.notNull("model", model);

      final var initialTxt = model.get();
      if (Strings.isNotEmpty(initialTxt)) {
         widget.setText(initialTxt);
      }

      model.subscribe(txt -> run(() -> {
         final var oldTxt = widget.getText();
         final var newTxt = txt == null ? "" : txt;
         if (!Objects.equals(oldTxt, newTxt)) {
            widget.setText(newTxt);
         }
      }));
      widget.addModifyListener(ev -> model.set(widget.getText()));
   }

   public static void center(final Shell shell) {
      final Rectangle parentBounds;
      if (shell.getParent() == null) {
         parentBounds = shell.getDisplay().getBounds();
      } else {
         parentBounds = shell.getParent().getBounds();
      }
      final var shellBounds = shell.getBounds();
      final var x = (parentBounds.width - shellBounds.width) / 2 + parentBounds.x;
      final var y = (parentBounds.height - shellBounds.height) / 2 + parentBounds.y;
      shell.setLocation(new Point(x, y));
   }

   public static IEditorPart getActiveEditor() {
      final var activePage = getActivePage();
      if (activePage == null)
         return null;
      return activePage.getActiveEditor();
   }

   public static IWorkbenchPage getActivePage() {
      final var window = getActiveWorkbenchWindow();
      return window == null ? null : window.getActivePage();
   }

   public static IWorkbenchWindow getActiveWorkbenchWindow() {
      final var workbench = PlatformUI.getWorkbench();
      final var window = workbench.getActiveWorkbenchWindow();
      if (window == null) {
         final var windows = workbench.getWorkbenchWindows();
         return windows.length == 0 ? null : windows[0];
      }
      return window;
   }

   public static Display getDisplay() {
      var display = PlatformUI.getWorkbench().getDisplay();
      if (display == null) {
         display = Display.getCurrent();
      }
      if (display == null) {
         display = Display.getDefault();
      }
      return display;
   }

   public static Shell getShell() {
      final var window = getActiveWorkbenchWindow();
      return window == null ? null : window.getShell();
   }

   public static boolean isUIThread() {
      return Display.getCurrent() != null;
   }

   public static void onModified(final Text text, final BiConsumer<Text, ModifyEvent> handler) {
      text.addModifyListener(ev -> handler.accept(text, ev));
   }

   public static void onModified(final Text text, final Runnable handler) {
      text.addModifyListener(ev -> handler.run());
   }

   /**
    * Runs the given runnable asynchronous on the UI thread
    */
   public static void run(final Runnable run) {
      if (isUIThread()) {
         run.run();
      } else {
         getDisplay().asyncExec(run);
      }
   }

   private UI() {
   }
}
