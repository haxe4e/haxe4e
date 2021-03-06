/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4e.outline.SymbolsModel.DocumentSymbolWithFile;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolLocation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import de.sebthom.eclipse.commons.ui.Editors;

/**
 * See https://wiki.eclipse.org/Platform_Expression_Framework
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings({"restriction", "deprecation"})
public final class FileTypePropertyTester extends PropertyTester {

   private static final String PROPERTY_CONTENT_TYPE_ID = "contentTypeId";
   private static final String PROPERTY_FILE_EXTENSION = "fileExtension";

   private boolean matchesPropertyValue(final String propertyName, final String fileName, final Object expectedPropertyValue) {
      if (fileName == null)
         return false;

      return switch (propertyName) {
         case PROPERTY_CONTENT_TYPE_ID -> {
            final var contentType = Platform.getContentTypeManager().findContentTypeFor(fileName);
            yield contentType != null && contentType.getId().equals(expectedPropertyValue);
         }
         case PROPERTY_FILE_EXTENSION -> fileName.endsWith("." + expectedPropertyValue);
         default -> false;
      };
   }

   @Override
   public boolean test(final Object candidate, final String property, final Object[] args, final Object expectedPropertyValue) {

      if (candidate instanceof final SymbolInformation symbolInfo) {
         final var location = symbolInfo.getLocation();
         if (location == null)
            return false;
         return matchesPropertyValue(property, location.getUri(), expectedPropertyValue);
      }

      if (candidate instanceof final WorkspaceSymbol wsSymbol) {
         final Either<Location, WorkspaceSymbolLocation> location = wsSymbol.getLocation();
         final String uri = location.isLeft() ? location.getLeft().getUri() : location.getRight().getUri();
         return matchesPropertyValue(property, uri, expectedPropertyValue);
      }

      final IFile file;
      if (candidate instanceof final IFile f) {
         file = f;
      } else if (candidate instanceof final DocumentSymbolWithFile d) {
         file = d.file;
      } else if (candidate instanceof DocumentSymbol) {
         final var editor = Editors.getActiveTextEditor();
         if (editor == null)
            return false;
         file = editor.getEditorInput().getAdapter(IFile.class);
      } else
         return false;

      return matchesPropertyValue(property, file.getName(), expectedPropertyValue);
   }
}
