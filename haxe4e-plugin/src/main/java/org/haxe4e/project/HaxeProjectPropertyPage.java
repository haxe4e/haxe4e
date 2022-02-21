/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.util.ui.GridDatas;
import org.haxe4e.widget.HaxeBuildFileSelectionGroup;
import org.haxe4e.widget.HaxeSDKSelectionGroup;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Buttons;
import net.sf.jstuff.core.ref.ObservableRef;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeProjectPropertyPage extends org.eclipse.ui.dialogs.PropertyPage {

   private ObservableRef<HaxeSDK> selectedAltSDK;
   private ObservableRef<String> buildFile;
   private ObservableRef<Boolean> autoBuild;
   private HaxeProjectPreference prefs;

   @Override
   protected Control createContents(final Composite parent) {
      // we don't need the "Restore Defaults" button
      noDefaultButton();

      final var container = new Composite(parent, SWT.NONE);
      container.setLayout(new GridLayout(1, true));
      container.setLayoutData(GridDatas.fillHorizontal());

      final var project = Projects.adapt(getElement());
      prefs = HaxeProjectPreference.get(project);
      final var grpHaxeSDKSelection = new HaxeSDKSelectionGroup(container, GridDataFactory.fillDefaults().create());
      selectedAltSDK = grpHaxeSDKSelection.selectedAltSDK;
      selectedAltSDK.set(prefs.getAlternateHaxeSDK());

      final var grpBuildFile = new HaxeBuildFileSelectionGroup(container, GridDatas.fillHorizontalExcessive());
      grpBuildFile.project.set(prefs.getProject());
      buildFile = grpBuildFile.buildFile;
      buildFile.set(prefs.getHaxeBuildFile());

      autoBuild = new ObservableRef<>(prefs.isAutoBuild());
      final var btnAutoBuild = new Button(container, SWT.CHECK);
      btnAutoBuild.setText("Enable auto build");
      Buttons.bind(btnAutoBuild, autoBuild);
      return container;
   }

   @Override
   public boolean performOk() {
      prefs.setAlternateHaxeSDK(selectedAltSDK.get());
      prefs.setHaxeBuildFile(buildFile.get());
      prefs.setAutoBuild(autoBuild.get());
      prefs.save();
      return super.performOk();
   }

}
