/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4e.outline.SymbolsModel.DocumentSymbolWithFile;
import org.eclipse.lsp4j.SymbolInformation;

/**
 * See https://wiki.eclipse.org/Platform_Expression_Framework
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class FileTypePropertyTester extends PropertyTester {

   private static final String PROPERTY_CONTENT_TYPE_ID = "contentTypeId";
   private static final String PROPERTY_FILE_EXTENSION = "fileExtension";

   @Override
   public boolean test(final Object candidate, final String property, final Object[] args, final Object expectedValue) {

      if (candidate instanceof SymbolInformation) {
         final var location = ((SymbolInformation) candidate).getLocation();
         if (location == null || location.getUri() == null)
            return false;

         final var uri = location.getUri();

         switch (property) {
            case PROPERTY_CONTENT_TYPE_ID:
               final var contentTypeManager = Platform.getContentTypeManager();
               final var contentType = contentTypeManager.findContentTypeFor(uri);
               return contentType != null && contentType.getId().equals(expectedValue.toString());
            case PROPERTY_FILE_EXTENSION:
               return uri.endsWith("." + expectedValue.toString());
            default:
               return false;
         }
      }

      IFile file = null;
      if (candidate instanceof IFile) {
         file = (IFile) candidate;
      } else if (candidate instanceof DocumentSymbolWithFile) {
         file = ((DocumentSymbolWithFile) candidate).file;
      } else
         return false;

      switch (property) {
         case PROPERTY_CONTENT_TYPE_ID:
            final var contentTypeManager = Platform.getContentTypeManager();
            final var contentType = contentTypeManager.findContentTypeFor(file.getName());
            return contentType != null && contentType.getId().equals(expectedValue.toString());
         case PROPERTY_FILE_EXTENSION:
            return file.getFileExtension().equals(expectedValue.toString());
         default:
            return false;
      }
   }
}
