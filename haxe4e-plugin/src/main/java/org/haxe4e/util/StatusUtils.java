/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.haxe4e.Haxe4EPlugin;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class StatusUtils {

   public static IStatus createError(final String msg, final Object... msgArgs) {
      return createStatus(IStatus.ERROR, msg, msgArgs);
   }

   public static IStatus createError(final Throwable ex) {
      return createStatus(IStatus.ERROR, ex, ex.getMessage());
   }

   public static IStatus createError(final Throwable ex, final String msg, final Object... msgArgs) {
      return createStatus(IStatus.ERROR, ex, msg, msgArgs);
   }

   public static IStatus createInfo(final String msg, final Object... msgArgs) {
      return createStatus(IStatus.INFO, msg, msgArgs);
   }

   public static IStatus createInfo(final Throwable ex) {
      return createStatus(IStatus.INFO, ex, ex.getMessage());
   }

   public static IStatus createInfo(final Throwable ex, final String msg, final Object... msgArgs) {
      return createStatus(IStatus.INFO, ex, msg, msgArgs);
   }

   public static IStatus createStatus(final int severity, final String msg, final Object... msgArgs) {
      if (msgArgs == null || msgArgs.length == 0)
         return new Status(severity, Haxe4EPlugin.PLUGIN_ID, msg);
      return new Status(severity, Haxe4EPlugin.PLUGIN_ID, NLS.bind(msg, msgArgs));
   }

   public static IStatus createStatus(final int severity, final Throwable ex, final String msg, final Object... msgArgs) {
      final var statusMsg = Strings.isNotEmpty(msg) //
         ? NLS.bind(msg, msgArgs) //
         : ex == null //
            ? null //
            : Strings.isEmpty(ex.getMessage()) //
               ? ex.getClass().getName() //
               : ex.getMessage();

      if (statusMsg == null)
         throw new IllegalArgumentException("[ex] or [msg] must be specified");

      return new Status(severity, Haxe4EPlugin.PLUGIN_ID, severity, statusMsg, ex);
   }

   public static IStatus createWarning(final String msg, final Object... msgArgs) {
      return createStatus(IStatus.WARNING, msg, msgArgs);
   }

   public static IStatus createWarning(final Throwable ex) {
      return createStatus(IStatus.WARNING, ex, ex.getMessage());
   }

   public static IStatus createWarning(final Throwable ex, final String msg, final Object... msgArgs) {
      return createStatus(IStatus.WARNING, ex, msg, msgArgs);
   }

   private StatusUtils() {
   }
}
