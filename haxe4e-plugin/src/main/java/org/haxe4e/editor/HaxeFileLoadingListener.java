/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
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
