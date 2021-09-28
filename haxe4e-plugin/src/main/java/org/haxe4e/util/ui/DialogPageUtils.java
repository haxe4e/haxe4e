/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;

/**
 * @author Sebastian Thomschke
 */
public final class DialogPageUtils {

   public static void setMessage(final DialogPage page, final IStatus status) {
      final var msg = status.getMessage();
      switch (status.getSeverity()) {
         case IStatus.ERROR:
            page.setMessage(null);
            page.setErrorMessage(msg);
            break;
         case IStatus.WARNING:
            page.setMessage(msg, IMessageProvider.WARNING);
            page.setErrorMessage(null);
            break;
         case IStatus.INFO:
            page.setMessage(msg, IMessageProvider.INFORMATION);
            page.setErrorMessage(null);
            break;
         case IStatus.OK:
            page.setMessage(msg, IMessageProvider.NONE);
            page.setErrorMessage(null);
            break;
         default:
      }
   }

   private DialogPageUtils() {
   }
}
