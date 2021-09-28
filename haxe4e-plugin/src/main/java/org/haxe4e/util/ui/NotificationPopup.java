/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util.ui;

import org.eclipse.jface.notifications.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public class NotificationPopup extends AbstractNotificationPopup {

   private final String title;
   private final String message;

   public NotificationPopup(final String message) {
      super(UI.getDisplay());
      title = null;
      this.message = message;
   }

   public NotificationPopup(final String title, final String message) {
      super(UI.getDisplay());
      this.title = title;
      this.message = message;
   }

   @Override
   protected Shell getParentShell() {
      return UI.getShell();
   }

   @Override
   protected void createContentArea(final Composite parent) {
      final var label = new StyledText(parent, SWT.WRAP);
      label.setEditable(false);
      label.setCaret(null);
      label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
      label.setText(message);
   }

   @Override
   protected Image getPopupShellImage(final int maximumHeight) {
      return Haxe4EPlugin.getSharedImage(Constants.IMAGE_ICON);
   }

   @Override
   protected String getPopupShellTitle() {
      return Strings.isEmpty(title) ? super.getPopupShellTitle() : title;
   }
}
