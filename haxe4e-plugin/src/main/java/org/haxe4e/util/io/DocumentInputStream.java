/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * @author Sebastian Thomschke
 */
public final class DocumentInputStream extends InputStream {

   private IDocument doc;
   private int pos;

   public DocumentInputStream(final IDocument document) {
      doc = document;
      pos = 0;
   }

   public IDocument getDocument() {
      return doc;
   }

   @Override
   public int read(final byte[] buff, final int buffOffset, final int len) throws IOException {
      Objects.checkFromIndexSize(buffOffset, len, buff.length);

      if (len == 0)
         return 0;

      final var docLen = doc.getLength();
      if (pos >= docLen)
         return -1;

      var bytesRead = -1;
      try {
         buff[buffOffset] = (byte) doc.getChar(pos++);
         bytesRead = 1;

         while (bytesRead < len) {
            if (pos >= docLen) {
               break;
            }

            buff[buffOffset + bytesRead++] = (byte) doc.getChar(pos++);
         }
      } catch (final BadLocationException ex) {
         // ignore
      }
      return bytesRead;
   }

   @Override
   public int read() throws IOException {
      try {
         if (pos < doc.getLength())
            return doc.getChar(pos++) & 0xFF;
      } catch (final BadLocationException ex) {
         // ignore
      }
      return -1;
   }
}
