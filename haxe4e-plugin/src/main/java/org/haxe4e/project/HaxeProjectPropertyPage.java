/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.navigation.HaxeDependenciesBuilder;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.util.ui.GridDatas;
import org.haxe4e.widget.HaxeBuildFileSelectionGroup;
import org.haxe4e.widget.HaxeSDKSelectionGroup;

import net.sf.jstuff.core.ref.ObservableRef;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeProjectPropertyPage extends org.eclipse.ui.dialogs.PropertyPage {

   private ObservableRef<HaxeSDK> selectedAltSDK;
   private ObservableRef<String> buildFile;
   private HaxeProjectPreference prefs;

   @Override
   protected Control createContents(final Composite parent) {
      // we don't need the "Restore Defaults" button
      noDefaultButton();

      final var container = new Composite(parent, SWT.NONE);
      container.setLayout(new GridLayout(1, true));
      container.setLayoutData(GridDatas.fillHorizontal());

      var project = Adapters.adapt(getElement(), IProject.class);
      if (project == null) {
         final var resource = Adapters.adapt(getElement(), IResource.class);
         Assert.isNotNull(resource, "unable to adapt element to a project");
         project = resource.getProject();
      }

      prefs = new HaxeProjectPreference(project);
      final var grpHaxeSDKSelection = new HaxeSDKSelectionGroup(container, GridDataFactory.fillDefaults().create());
      selectedAltSDK = grpHaxeSDKSelection.selectedAltSDK;
      selectedAltSDK.set(prefs.getAlternateHaxeSDK());

      final var grpBuildFile = new HaxeBuildFileSelectionGroup(container, GridDatas.fillHorizontalExcessive());
      grpBuildFile.project.set(prefs.getProject());
      buildFile = grpBuildFile.buildFile;
      buildFile.set(prefs.getHaxeBuildFile());

      return container;
   }

   @Override
   public boolean performOk() {
      prefs.setAlternateHaxeSDK(selectedAltSDK.get());
      prefs.setHaxeBuildFile(buildFile.get());
      prefs.save();
      HaxeDependenciesBuilder.onHaxeProjectConfigChanged(prefs.getProject());
      return super.performOk();
   }

}
