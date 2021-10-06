/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.navigation;

import java.io.IOException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.swt.graphics.Image;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeBuildFile;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.util.LOG;

/**
 * @author Sebastian Thomschke
 */
public class HaxeResourcesDecorator extends BaseLabelProvider implements ILabelDecorator {

   @Override
   public Image decorateImage(final Image image, final Object element) {
      final var folder = (IFolder) element;
      final var project = folder.getProject();

      if (HaxeProjectNature.hasNature(project) != Boolean.TRUE)
         return image;
      if (folder.isVirtual()) {
         if (folder.getName().equals(HaxeDependenciesBuilder.HAXE_DEPS_MAGIC_FOLDER_NAME))
            return Haxe4EPlugin.getSharedImage(Constants.IMAGE_HAXE_DEPENDENCIES);
         return image;
      }

      if (folder.isLinked())
         return image;

      final var prefs = new HaxeProjectPreference(project);
      prefs.getEffectiveHaxeSDK();
      final var haxeBuildFilePath = prefs.getEffectiveHaxeBuildFile();
      if (haxeBuildFilePath == null)
         return image;

      final var haxeBuildFile = new HaxeBuildFile(haxeBuildFilePath.getLocation().toFile());
      final var folderPath = folder.getLocation().toFile().toPath();
      final var projectPath = project.getLocation().toFile().toPath();
      try {
         for (final var p : haxeBuildFile.getSourcePaths()) {
            final var sourceFolder = projectPath.resolve(p);
            if (folderPath.equals(sourceFolder))
               return Haxe4EPlugin.getSharedImage(Constants.IMAGE_HAXE_SOURCE_FOLDER);
            if (folderPath.startsWith(sourceFolder))
               return Haxe4EPlugin.getSharedImage(Constants.IMAGE_HAXE_SOURCE_PACKAGE);

         }
      } catch (final IOException ex) {
         LOG.error(ex);
      }

      return image;
   }

   @Override
   public String decorateText(final String text, final Object element) {
      final var folder = (IFolder) element;
      final var project = folder.getProject();

      if (HaxeProjectNature.hasNature(project) == Boolean.TRUE //
         && folder.isVirtual() //
         && folder.getName().equals(HaxeDependenciesBuilder.HAXE_DEPS_MAGIC_FOLDER_NAME) //
      )
         return "Haxe Dependencies";

      return text;
   }
}
