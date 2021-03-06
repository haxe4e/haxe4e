/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;

/**
 * @author Sebastian Thomschke
 */
public class HaxeFileLoadingListener implements IDocumentSetupParticipant {

   @Override
   public void setup(final IDocument document) {
      // here we can do things when a document gets loaded
   }
}
