/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.navigation;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeBuildFile;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.project.HaxeProjectNature;

import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Sebastian Thomschke
 */
public class HaxeResourcesDecorator extends BaseLabelProvider implements ILabelDecorator {

   public static HaxeResourcesDecorator getInstance() {
      return (HaxeResourcesDecorator) PlatformUI.getWorkbench().getDecoratorManager() //
         .getLabelDecorator(HaxeResourcesDecorator.class.getName());
   }

   @Override
   public Image decorateImage(final Image image, final Object element) {
      final var res = (IResource) element;
      final var project = res.getProject();

      if (HaxeProjectNature.hasNature(project) != Boolean.TRUE)
         return image;

      if (res.isVirtual()) {
         if (res instanceof IFolder && res.getName().equals(HaxeDependenciesUpdater.HAXE_DEPS_MAGIC_FOLDER_NAME))
            return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_DEPENDENCIES);
         return image;
      }

      if (res.isLinked()) {
         if (res instanceof IFolder && res.getName().equals(HaxeDependenciesUpdater.HAXE_STDLIB_MAGIC_FOLDER_NAME))
            return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_DEPENDENCIES);
         return image;
      }

      if (res instanceof IFile) {
         final var file = (IFile) res;
         if (Constants.HAXE_BUILD_FILE_EXTENSION.equals(file.getFileExtension())) {
            final var prefs = HaxeProjectPreference.get(project);
            final var haxeBuildFilePath = prefs.getEffectiveHaxeBuildFile();
            if (haxeBuildFilePath == null)
               return image;
            if (haxeBuildFilePath.equals(file))
               return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_BUILD_FILE_ACTIVE);
         }
      } else if (res instanceof IFolder) {
         final var folder = (IFolder) res;
         final var prefs = HaxeProjectPreference.get(project);
         final var haxeBuildFilePath = prefs.getEffectiveHaxeBuildFile();
         if (haxeBuildFilePath == null)
            return image;

         final var haxeBuildFile = new HaxeBuildFile(haxeBuildFilePath.getLocation().toFile());
         final var folderPath = folder.getLocation().toFile().toPath();
         final var projectPath = project.getLocation().toFile().toPath();
         try {
            for (final var p : haxeBuildFile.getSourcePaths()) {
               final var sourceFolder = projectPath.resolve(p).normalize();
               if (folderPath.equals(sourceFolder))
                  return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_SOURCE_FOLDER);
               if (folderPath.startsWith(sourceFolder))
                  return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_SOURCE_PACKAGE);
            }
         } catch (final IOException ex) {
            Haxe4EPlugin.log().error(ex);
         }
      }

      return image;
   }

   @Override
   public String decorateText(final String text, final Object element) {
      if (element instanceof IFolder) {
         final var folder = (IFolder) element;
         final var project = folder.getProject();

         if (HaxeProjectNature.hasNature(project) == Boolean.TRUE) {
            if (folder.isLinked() //
               && folder.getName().equals(HaxeDependenciesUpdater.HAXE_STDLIB_MAGIC_FOLDER_NAME) //
            ) {
               final var prefs = HaxeProjectPreference.get(project);
               final var haxeSDK = prefs.getEffectiveHaxeSDK();
               return "Haxe Standard Library" + (haxeSDK == null ? "" : " [" + haxeSDK.getName() + "]");
            }

            if (folder.isVirtual() //
               && folder.getName().equals(HaxeDependenciesUpdater.HAXE_DEPS_MAGIC_FOLDER_NAME) //
            )
               return "Haxe Dependencies";
         }
      }
      return text;
   }

   public void refreshElements(final IResource... res) {
      if (res == null || res.length == 0)
         return;

      UI.run(() -> fireLabelProviderChanged(new LabelProviderChangedEvent(this, res)));
   }
}
