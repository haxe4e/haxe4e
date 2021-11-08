/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.prefs.HaxeWorkspacePreference;
import org.haxe4e.widget.HaxeSDKSelectionGroup;

import net.sf.jstuff.core.ref.ObservableRef;

/**
 * @author Sebastian Thomschke
 */
public final class NewHaxeProjectPage extends WizardNewProjectCreationPage {

   public ObservableRef<HaxeSDK> selectedAltSDK;

   public NewHaxeProjectPage(final String pageName) {
      super(pageName);
      setImageDescriptor(Haxe4EPlugin.get().getSharedImageDescriptor(Constants.IMAGE_HAXE_WIZARD_BANNER));
   }

   @Override
   public void createControl(final Composite parent) {
      super.createControl(parent);

      final var control = (Composite) getControl();
      final var grpHaxeSDKSelection = new HaxeSDKSelectionGroup(control, GridDataFactory.fillDefaults().create());
      selectedAltSDK = grpHaxeSDKSelection.selectedAltSDK;
   }

   @Override
   protected boolean validatePage() {
      if (!super.validatePage())
         return false;

      if (selectedAltSDK.get() == null && HaxeWorkspacePreference.getDefaultHaxeSDK(false, true) == null) {
         setMessage(Messages.NewHaxeProject_SDKNotFound_Message, IMessageProvider.WARNING);
      }
      return true;
   }
}
