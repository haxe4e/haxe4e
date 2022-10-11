/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.util.ui.GridDatas;
import org.haxe4e.widget.HaxeBuildFileSelectionGroup;
import org.haxe4e.widget.HaxeBuildSystemSelectionGroup;
import org.haxe4e.widget.HaxeSDKSelectionGroup;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Buttons;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeProjectPropertyPage extends org.eclipse.ui.dialogs.PropertyPage {

   private HaxeProjectPreference prefs = eventuallyNonNull();

   @Override
   protected Control createContents(final Composite parent) {
      // we don't need the "Restore Defaults" button
      noDefaultButton();

      final var container = new Composite(parent, SWT.NONE);
      container.setLayout(new GridLayout(1, true));
      container.setLayoutData(GridDatas.fillHorizontal());

      /*
       * alt SDK selection
       */
      final var project = asNonNullUnsafe(Projects.adapt(getElement()));
      prefs = HaxeProjectPreference.get(project);
      final var grpHaxeSDKSelection = new HaxeSDKSelectionGroup(container, GridDataFactory.fillDefaults().create());
      grpHaxeSDKSelection.selectedAltSDK.set(prefs.getAlternateHaxeSDK());
      grpHaxeSDKSelection.selectedAltSDK.subscribe(prefs::setAlternateHaxeSDK);

      /*
       * build system selection
       */
      final var grpBuildSystem = new HaxeBuildSystemSelectionGroup(container, GridDatas.fillHorizontalExcessive());
      grpBuildSystem.setProject(project);

      /*
       * build file selection
       */
      final var grpBuildFile = new HaxeBuildFileSelectionGroup(container, GridDatas.fillHorizontalExcessive());
      grpBuildFile.setProject(project);
      grpBuildFile.selectedBuildFile.subscribe(buildFile -> prefs.setBuildFilePath(buildFile == null ? null
         : buildFile.getProjectRelativePath()));

      grpBuildSystem.selectedBuildSystem.subscribe(selectedBuildSystem -> {
         prefs.setBuildSystem(selectedBuildSystem);
         // clear build file selection if build file extension is not supported by newly selected build system
         grpBuildFile.selectedBuildFile.set(prefs.getBuildFile());
      });

      /*
       * auto build check box
       */
      final var btnAutoBuild = new Button(container, SWT.CHECK);
      btnAutoBuild.setText("Enable auto build");
      btnAutoBuild.setSelection(prefs.isAutoBuild());
      Buttons.onSelected(btnAutoBuild, () -> prefs.setAutoBuild(btnAutoBuild.getSelection()));
      return container;
   }

   @Override
   public boolean performOk() {
      prefs.save();
      return super.performOk();
   }

   @Override
   public boolean performCancel() {
      prefs.revert();
      return true;
   }
}
