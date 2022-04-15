/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.prefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.buildsystem.BuildFile;
import org.haxe4e.model.buildsystem.BuildSystem;
import org.haxe4e.navigation.HaxeDependenciesUpdater;
import org.haxe4e.navigation.HaxeResourcesDecorator;

import de.sebthom.eclipse.commons.ui.Dialogs;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.validation.Args;

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
   private BuildFile effectiveBuildFileBeforeSave;
   private List<PropertyChangeEvent> changeEvents = new ArrayList<>();

   private HaxeProjectPreference(final IProject project) {
      Args.notNull("project", project);

      this.project = project;
      prefs = new ScopedPreferenceStore(new ProjectScope(project), Haxe4EPlugin.PLUGIN_ID);
      prefs.setDefault(PROPERTY_ALTERNATE_AUTO_BUILD, true);
      prefs.addPropertyChangeListener(changeEvents::add);
   }

   /**
    * @return null if none configured
    */
   public HaxeSDK getAlternateHaxeSDK() {
      return HaxeWorkspacePreference.getHaxeSDK(prefs.getString(PROPERTY_ALTERNATE_HAXE_SDK));
   }

   /**
    * Determines the effective default build file
    *
    * @return null if none configured/not found/not compatible with selected build system
    */
   public BuildFile getBuildFile() {
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
      return BuildSystem.HAXE;
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
         if (oldValue instanceof String) {
            prefs.setValue(event.getProperty(), (String) oldValue);
         } else if (oldValue instanceof Boolean) {
            prefs.setValue(event.getProperty(), (Boolean) oldValue);
         } else if (oldValue instanceof Integer) {
            prefs.setValue(event.getProperty(), (Integer) oldValue);
         } else if (oldValue instanceof Long) {
            prefs.setValue(event.getProperty(), (Long) oldValue);
         } else if (oldValue instanceof Float) {
            prefs.setValue(event.getProperty(), (Float) oldValue);
         } else if (oldValue instanceof Double) {
            prefs.setValue(event.getProperty(), (Double) oldValue);
         }
      }
      changeEvents.clear();
   }

   public boolean save() {
      try {
         changeEvents.clear();
         prefs.save();

         // force refresh of the affected build file icons
         if (effectiveBuildFileBeforeSave != null) {
            final var effectiveBuildFile = getBuildFile();
            HaxeResourcesDecorator.getInstance().refreshElements( //
               effectiveBuildFileBeforeSave == null ? null : effectiveBuildFileBeforeSave.location, //
               effectiveBuildFile == null ? null : effectiveBuildFile.location //
            );
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
      prefs.setValue(PROPERTY_ALTERNATE_HAXE_SDK, sdk == null ? "" : sdk.getName());
   }

   public void setAutoBuild(final boolean value) {
      prefs.setValue(PROPERTY_ALTERNATE_AUTO_BUILD, value);
   }

   public void setBuildFilePath(final String projectRelativePath) {
      if (effectiveBuildFileBeforeSave == null) {
         effectiveBuildFileBeforeSave = getBuildFile();
      }
      prefs.setValue(PROPERTY_BUILD_FILE, projectRelativePath == null ? "" : projectRelativePath);
   }

   public void setBuildSystem(final BuildSystem buildSystem) {
      prefs.setValue(PROPERTY_BUILD_SYSTEM, buildSystem == null ? "" : buildSystem.name());
   }
}
