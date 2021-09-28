/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.FrameworkUtil;

import net.sf.jstuff.core.logging.Logger;

/**
 * @author Sebastian Thomschke
 */
public final class LOG {

   private static final Logger JRE_LOG = Logger.create(FrameworkUtil.getBundle(LOG.class).getSymbolicName());
   private static final ILog ECLIPSE_LOG = Platform.getLog(LOG.class);

   public static void debug(final String msg, final Object... msgArgs) {
      JRE_LOG.info(NLS.bind(msg, msgArgs));
   }

   public static void debug(final Throwable ex) {
      JRE_LOG.info(ex);
   }

   public static void debug(final Throwable ex, final String msg, final Object... msgArgs) {
      JRE_LOG.info(ex, NLS.bind(msg, msgArgs));
   }

   public static IStatus error(final Object msg) {
      final var status = StatusUtils.createError(msg == null ? null : msg.toString());
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus error(final String msg, final Object... msgArgs) {
      final var status = StatusUtils.createError(msg, msgArgs);
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus error(final Throwable ex) {
      final var status = StatusUtils.createError(ex);
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus error(final Throwable ex, final String msg, final Object... msgArgs) {
      final var status = StatusUtils.createError(ex, msg, msgArgs);
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus info(final Object msg) {
      final var status = StatusUtils.createInfo(msg == null ? null : msg.toString());
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus info(final String msg, final Object... msgArgs) {
      final var status = StatusUtils.createInfo(msg, msgArgs);
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus info(final Throwable ex, final String msg, final Object... msgArgs) {
      final var status = StatusUtils.createInfo(ex, msg, msgArgs);
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus log(final IStatus status) {
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus warn(final Object msg) {
      final var status = StatusUtils.createWarning(msg == null ? null : msg.toString());
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus warn(final String msg, final Object... msgArgs) {
      final var status = StatusUtils.createWarning(msg, msgArgs);
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus warn(final Throwable ex) {
      final var status = StatusUtils.createWarning(ex);
      ECLIPSE_LOG.log(status);
      return status;
   }

   public static IStatus warn(final Throwable ex, final String msg, final Object... msgArgs) {
      final var status = StatusUtils.createWarning(ex, msg, msgArgs);
      ECLIPSE_LOG.log(status);
      return status;
   }

   private LOG() {
   }
}
