/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.prefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.buildsystem.BuildFile;
import org.haxe4e.model.buildsystem.BuildSystem;
import org.haxe4e.model.buildsystem.LixVirtualBuildFile;
import org.haxe4e.navigation.HaxeDependenciesUpdater;
import org.haxe4e.navigation.HaxeResourcesDecorator;

import de.sebthom.eclipse.commons.ui.Dialogs;
import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeProjectPreference {

   private static final WeakHashMap<IProject, HaxeProjectPreference> PREFS_BY_PROJECT = new WeakHashMap<>();

   private static final String PROPERTY_ALTERNATE_AUTO_BUILD = "haxe.project.auto_build";
   private static final String PROPERTY_ALTERNATE_HAXE_SDK = "haxe.project.alternate_sdk";
   private static final String PROPERTY_BUILD_FILE = "haxe.project.build_file";
   private static final String PROPERTY_BUILD_SYSTEM = "haxe.project.build_system";

   public static HaxeProjectPreference get(final IProject project) {
      synchronized (PREFS_BY_PROJECT) {
         return PREFS_BY_PROJECT.computeIfAbsent(project, HaxeProjectPreference::new);
      }
   }

   private final IPersistentPreferenceStore prefs;
   private final IProject project;
   private @Nullable BuildFile effectiveBuildFileBeforeSave;
   private final List<PropertyChangeEvent> changeEvents = new ArrayList<>();

   private HaxeProjectPreference(final IProject project) {
      this.project = project;
      prefs = new ScopedPreferenceStore(new ProjectScope(project), Haxe4EPlugin.PLUGIN_ID);
      prefs.setDefault(PROPERTY_ALTERNATE_AUTO_BUILD, true);
      prefs.addPropertyChangeListener(changeEvents::add);
   }

   /**
    * @return null if none configured
    */
   public @Nullable HaxeSDK getAlternateHaxeSDK() {
      return HaxeWorkspacePreference.getHaxeSDK(prefs.getString(PROPERTY_ALTERNATE_HAXE_SDK));
   }

   /**
    * Determines the effective default build file
    *
    * @return null if none configured/not found/not compatible with selected build system
    */
   public @Nullable BuildFile getBuildFile() {
      final var buildSystem = getBuildSystem();

      // build file manually configured?
      final var buildFilePath = prefs.getString(PROPERTY_BUILD_FILE);
      if (Strings.isNotEmpty(buildFilePath)) {
         final var buildFile = project.getFile(buildFilePath);
         if (buildSystem.getBuildFileExtension().equalsIgnoreCase(buildFile.getFileExtension()) && buildFile.exists())
            return buildSystem.toBuildFile(buildFile);
      }

      // fallback to default build files
      for (final var buildFileName : buildSystem.getDefaultBuildFileNames()) {
         final var buildFile = project.getFile(buildFileName);
         if (buildFile.exists())
            return buildSystem.toBuildFile(buildFile);
      }

      if (buildSystem == BuildSystem.LIX)
         return new LixVirtualBuildFile(project);

      return null;
   }

   public BuildSystem getBuildSystem() {
      final var bs = prefs.getString(PROPERTY_BUILD_SYSTEM);
      if (Strings.isNotBlank(bs)) {
         try {
            return BuildSystem.valueOf(bs);
         } catch (final IllegalArgumentException ex) {
            Haxe4EPlugin.log().error(ex);
         }
      }

      return BuildSystem.guessBuildSystemOfProject(project);
   }

   /**
    * @return null if none found
    */
   public @Nullable HaxeSDK getEffectiveHaxeSDK() {
      final var sdk = getAlternateHaxeSDK();
      if (sdk == null)
         return HaxeWorkspacePreference.getDefaultHaxeSDK(false, true);
      return sdk;
   }

   public IProject getProject() {
      return project;
   }

   public boolean isAutoBuild() {
      return prefs.getBoolean(PROPERTY_ALTERNATE_AUTO_BUILD);
   }

   /**
    * Reverts the preference state to the last persistent state.
    */
   public void revert() {
      final var changedEventsCopy = new ArrayList<>(changeEvents);
      Collections.reverse(changedEventsCopy);
      for (final var event : changedEventsCopy) {
         final var oldValue = event.getOldValue();
         if (oldValue == null) {
            prefs.setToDefault(event.getProperty());
         }
         if (oldValue instanceof final String oldValue_) {
            prefs.setValue(event.getProperty(), oldValue_);
         } else if (oldValue instanceof final Boolean oldValue_) {
            prefs.setValue(event.getProperty(), oldValue_);
         } else if (oldValue instanceof final Integer oldValue_) {
            prefs.setValue(event.getProperty(), oldValue_);
         } else if (oldValue instanceof final Long oldValue_) {
            prefs.setValue(event.getProperty(), oldValue_);
         } else if (oldValue instanceof final Float oldValue_) {
            prefs.setValue(event.getProperty(), oldValue_);
         } else if (oldValue instanceof final Double oldValue_) {
            prefs.setValue(event.getProperty(), oldValue_);
         }
      }
      changeEvents.clear();
   }

   public boolean save() {
      try {
         changeEvents.clear();
         prefs.save();

         // force refresh of the affected build file icons
         final var effectiveBuildFileBeforeSave = this.effectiveBuildFileBeforeSave;
         if (effectiveBuildFileBeforeSave != null) {
            final var effectiveBuildFile = getBuildFile();
            HaxeResourcesDecorator.getInstance().refreshElements( //
               effectiveBuildFileBeforeSave.location, //
               effectiveBuildFile == null ? null : effectiveBuildFile.location //
            );
            this.effectiveBuildFileBeforeSave = null;
         }
         HaxeDependenciesUpdater.INSTANCE.onProjectConfigChanged(project);

         return true;
      } catch (final IOException ex) {
         Dialogs.showStatus(Messages.Prefs_SavingPreferencesFailed, Haxe4EPlugin.status().createError(ex), true);
         return false;
      }
   }

   public void setAlternateHaxeSDK(final @Nullable HaxeSDK sdk) {
      prefs.setValue(PROPERTY_ALTERNATE_HAXE_SDK, sdk == null ? "" : sdk.getName());
   }

   public void setAutoBuild(final boolean value) {
      prefs.setValue(PROPERTY_ALTERNATE_AUTO_BUILD, value);
   }

   public void setBuildFilePath(final @Nullable String projectRelativePath) {
      if (effectiveBuildFileBeforeSave == null) {
         effectiveBuildFileBeforeSave = getBuildFile();
      }
      prefs.setValue(PROPERTY_BUILD_FILE, projectRelativePath == null ? "" : projectRelativePath);
   }

   public void setBuildSystem(final @Nullable BuildSystem buildSystem) {
      prefs.setValue(PROPERTY_BUILD_SYSTEM, buildSystem == null ? "" : buildSystem.name());
   }
}
