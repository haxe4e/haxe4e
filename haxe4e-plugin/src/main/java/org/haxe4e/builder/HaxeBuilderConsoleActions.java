/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.builder;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeBuilderConsoleActions implements IConsolePageParticipant {

   private Action terminate = eventuallyNonNull();

   @Override
   public void activated() {
   }

   @Override
   public void deactivated() {
   }

   @Override
   public void dispose() {
   }

   @Override
   public <T> @Nullable T getAdapter(final @Nullable Class<T> adapter) {
      return null;
   }

   @Override
   public void init(final IPageBookViewPage page, final IConsole console) {
      final var builderConsole = (HaxeBuilderConsole) console;

      builderConsole.buildContext.onTerminated.thenRun(() -> terminate.setEnabled(false));

      terminate = new Action("Terminate") {
         @Override
         public void run() {
            builderConsole.buildContext.monitor.setCanceled(true);
            terminate.setEnabled(false);
         }
      };
      terminate.setImageDescriptor(Haxe4EPlugin.get().getSharedImageDescriptor(Constants.IMAGE_TERMINATE_BUTTON));
      terminate.setDisabledImageDescriptor(Haxe4EPlugin.get().getSharedImageDescriptor(Constants.IMAGE_TERMINATE_BUTTON_DISABLED));

      final var bars = page.getSite().getActionBars();
      bars.getMenuManager().add(new Separator());
      bars.getMenuManager().add(terminate);

      final var toolbarManager = bars.getToolBarManager();
      toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminate);

      bars.updateActionBars();
   }
}
