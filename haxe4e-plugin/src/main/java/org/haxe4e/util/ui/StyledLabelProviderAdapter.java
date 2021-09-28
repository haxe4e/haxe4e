/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util.ui;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * @author Sebastian Thomschke
 */
public class StyledLabelProviderAdapter extends BaseLabelProvider implements IColorProvider, IFontProvider, IStyledLabelProvider {

   @Override
   public Color getBackground(final Object element) {
      return null;
   }

   @Override
   public Font getFont(final Object element) {
      return null;
   }

   @Override
   public Color getForeground(final Object element) {
      return null;
   }

   @Override
   public Image getImage(final Object element) {
      return null;
   }

   @Override
   public StyledString getStyledText(final Object element) {
      return null;
   }
}
