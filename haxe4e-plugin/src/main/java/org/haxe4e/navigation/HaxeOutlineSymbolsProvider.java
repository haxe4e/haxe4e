/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.navigation;

import org.eclipse.lsp4e.outline.SymbolsLabelProvider;
import org.eclipse.lsp4e.outline.SymbolsModel.DocumentSymbolWithFile;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.swt.graphics.Image;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;

/**
 * LSP4E's SymbolsLabelProvider does not provide images for all
 * SymbolKinds, thus we extend it.
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class HaxeOutlineSymbolsProvider extends SymbolsLabelProvider {

   @Override
   public Image getImage(final Object item) {
      SymbolKind kind = null;
      if (item instanceof final SymbolInformation symbolInfo) {
         kind = symbolInfo.getKind();
      } else if (item instanceof final DocumentSymbol docSymbol) {
         kind = docSymbol.getKind();
      } else if (item instanceof final DocumentSymbolWithFile docSymbol) {
         kind = docSymbol.symbol.getKind();
      } else if (item instanceof final WorkspaceSymbol symbol) {
         kind = symbol.getKind();
      }
      if (kind != null) {
         switch (kind) {
            case EnumMember:
               return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_OUTLINE_SYMBOL_ENUM_MEMBER);
            case Struct:
               return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_OUTLINE_SYMBOL_TYPEDEF);
            default:
         }
      }

      return super.getImage(item);
   }
}
