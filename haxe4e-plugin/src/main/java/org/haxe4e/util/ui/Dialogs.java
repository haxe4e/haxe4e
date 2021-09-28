/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util.ui;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @author Sebastian Thomschke
 */
public final class Dialogs {

   public static void showError(final String title, final String msg, final Object... msgArgs) {
      if (UI.isUIThread()) {
         showErrorInternal(title, msg, msgArgs);
         return;
      }
      UI.run(() -> showErrorInternal(title, msg, msgArgs));
   }

   private static void showErrorInternal(final String title, final String msg, final Object... msgArgs) {
      MessageDialog.openError(UI.getShell(), title, ArrayUtils.isEmpty(msgArgs) ? msg : NLS.bind(msg, msgArgs));
   }

   public static void showWarning(final String title, final String msg, final Object... msgArgs) {
      if (UI.isUIThread()) {
         showWarningInternal(title, msg, msgArgs);
         return;
      }
      UI.run(() -> showWarningInternal(title, msg, msgArgs));
   }

   private static void showWarningInternal(final String title, final String msg, final Object... msgArgs) {
      MessageDialog.openWarning(UI.getShell(), title, ArrayUtils.isEmpty(msgArgs) ? msg : NLS.bind(msg, msgArgs));
   }

   public static void showStatus(final String title, final IStatus status, final boolean log) {
      final var statusAdapter = new StatusAdapter(status);
      statusAdapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY, title);
      var style = StatusManager.SHOW | StatusManager.BLOCK;
      if (log) {
         style = style | StatusManager.LOG;
      }
      StatusManager.getManager().handle(statusAdapter, style);
   }

   private Dialogs() {
   }
}
