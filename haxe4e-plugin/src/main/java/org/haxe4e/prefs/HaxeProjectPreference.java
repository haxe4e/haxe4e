/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.prefs;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.navigation.HaxeDependenciesUpdater;
import org.haxe4e.navigation.HaxeResourcesDecorator;

import de.sebthom.eclipse.commons.ui.Dialogs;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public class HaxeProjectPreference {

   private static final String PROPERTY_ALTERNATE_HAXE_SDK = "haxe.project.alternate_sdk";
   private static final String PROPERTY_HAXE_BUILD_FILE = "haxe.project.build_file";

   private final IPersistentPreferenceStore prefs;
   private final IProject project;

   private IFile effectiveBuildFileBeforeSave;

   public HaxeProjectPreference(final IProject project) {
      Args.notNull("project", project);

      this.project = project;
      prefs = new ScopedPreferenceStore(new ProjectScope(project), Haxe4EPlugin.PLUGIN_ID);
   }

   /**
    * @return null if none configured
    */
   public HaxeSDK getAlternateHaxeSDK() {
      return HaxeWorkspacePreference.getHaxeSDK(prefs.getString(PROPERTY_ALTERNATE_HAXE_SDK));
   }

   /**
    * @return null if none found
    */
   public HaxeSDK getEffectiveHaxeSDK() {
      final var sdk = getAlternateHaxeSDK();
      if (sdk == null)
         return HaxeWorkspacePreference.getDefaultHaxeSDK(false, true);
      return sdk;
   }

   /**
    * @return null if none found
    */
   public IFile getEffectiveHaxeBuildFile() {
      final var buildFilePath = getHaxeBuildFile();
      if (Strings.isNotEmpty(buildFilePath)) {
         final var buildFile = project.getFile(buildFilePath);
         if (buildFile.exists())
            return buildFile;
      }
      var buildFile = project.getFile("build.hxml");
      if (buildFile.exists())
         return buildFile;
      buildFile = project.getFile("tests.hxml");
      if (buildFile.exists())
         return buildFile;

      return null;
   }

   /**
    * @return null if none configured
    */
   public String getHaxeBuildFile() {
      return prefs.getString(PROPERTY_HAXE_BUILD_FILE);
   }

   public IProject getProject() {
      return project;
   }

   public boolean save() {
      try {
         prefs.save();

         // force refresh of the affected build file icons
         if (effectiveBuildFileBeforeSave != null) {
            HaxeResourcesDecorator.getInstance().refreshElements(effectiveBuildFileBeforeSave, getEffectiveHaxeBuildFile());
            effectiveBuildFileBeforeSave = null;
         }
         HaxeDependenciesUpdater.INSTANCE.onHaxeProjectConfigChanged(project);

         return true;
      } catch (final IOException ex) {
         Dialogs.showStatus(Messages.Prefs_SavingPreferencesFailed, Haxe4EPlugin.status().createError(ex), true);
         return false;
      }
   }

   public void setAlternateHaxeSDK(final HaxeSDK sdk) {
      if (sdk == null) {
         prefs.setValue(PROPERTY_ALTERNATE_HAXE_SDK, "");
      } else {
         prefs.setValue(PROPERTY_ALTERNATE_HAXE_SDK, sdk.getName());
      }
   }

   public void setHaxeBuildFile(final String buildFile) {
      if (effectiveBuildFileBeforeSave == null) {
         effectiveBuildFileBeforeSave = getEffectiveHaxeBuildFile();
      }
      prefs.setValue(PROPERTY_HAXE_BUILD_FILE, buildFile);
   }

}
