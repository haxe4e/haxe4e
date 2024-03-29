/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.project;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;

/**
 * @author Sebastian Thomschke
 */
public final class NewHaxeFilePage extends WizardNewFileCreationPage {

   public NewHaxeFilePage(final String pageName, final IStructuredSelection selection) {
      super(pageName, selection);
      setAllowExistingResources(false);
      setFileExtension("hx");
      setImageDescriptor(Haxe4EPlugin.get().getSharedImageDescriptor(Constants.IMAGE_HAXE_WIZARD_BANNER));
   }
}
