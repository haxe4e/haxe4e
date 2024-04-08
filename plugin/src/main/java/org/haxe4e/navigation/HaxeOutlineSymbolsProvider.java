/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.navigation;

import org.eclipse.jdt.annotation.Nullable;
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
@SuppressWarnings({"deprecation", "removal", "restriction"})
public final class HaxeOutlineSymbolsProvider extends SymbolsLabelProvider {

   @Override
   public @Nullable Image getImage(final @Nullable Object item) {
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
               // TODO until https://github.com/eclipse/lsp4e/pull/257
               return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_OUTLINE_SYMBOL_ENUM_MEMBER);
            case Struct:
               return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_OUTLINE_SYMBOL_TYPEDEF);
            default:
         }
      }

      return super.getImage(item);
   }
}
