/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.navigation;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.project.HaxeProjectNature;

import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Sebastian Thomschke
 */
public class HaxeResourcesDecorator extends BaseLabelProvider implements ILabelDecorator {

   public static HaxeResourcesDecorator getInstance() {
      return (HaxeResourcesDecorator) asNonNullUnsafe(UI.getWorkbench().getDecoratorManager() //
         .getLabelDecorator(HaxeResourcesDecorator.class.getName()));
   }

   @Override
   public @Nullable Image decorateImage(final @Nullable Image image, final @Nullable Object element) {
      if (element == null)
         return null;
      final var res = (IResource) element;
      var project = res.getProject();

      if (!HaxeProjectNature.hasNature(project))
         return image;

      project = asNonNullUnsafe(project);

      if (res.isVirtual()) {
         if (res instanceof IFolder && res.getName().equals(HaxeDependenciesUpdater.DEPS_MAGIC_FOLDER_NAME))
            return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_DEPENDENCIES);
         return image;
      }

      if (res.isLinked()) {
         if (res instanceof IFolder && res.getName().equals(HaxeDependenciesUpdater.STDLIB_MAGIC_FOLDER_NAME))
            return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_DEPENDENCIES);
         return image;
      }

      if (res instanceof final IFile file) {
         final var prefs = HaxeProjectPreference.get(project);
         if (prefs.getBuildSystem().getBuildFileExtension().equals(file.getFileExtension())) {
            final var buildFile = prefs.getBuildFile();
            if (buildFile == null)
               return image;
            if (buildFile.location.equals(file))
               return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_BUILD_FILE_ACTIVE);
         }
      } else if (res instanceof final IFolder folder) {
         final var prefs = HaxeProjectPreference.get(project);
         final var buildFile = prefs.getBuildFile();
         if (buildFile == null)
            return image;

         final var folderPath = folder.getLocation().toFile().toPath();
         final var projectPath = project.getLocation().toFile().toPath();
         try {
            for (final var p : buildFile.getSourcePaths()) {
               final var sourceFolder = projectPath.resolve(p).normalize();
               if (folderPath.equals(sourceFolder))
                  return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_SOURCE_FOLDER);
               if (folderPath.startsWith(sourceFolder))
                  return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_SOURCE_PACKAGE);
            }
         } catch (final Exception ex) {
            Haxe4EPlugin.log().error(ex);
         }
      }

      return image;
   }

   @Override
   public @Nullable String decorateText(final @Nullable String text, final @Nullable Object element) {
      if (element instanceof final IFolder folder) {
         final var project = folder.getProject();

         if (HaxeProjectNature.hasNature(project)) {
            if (folder.isLinked() //
               && folder.getName().equals(HaxeDependenciesUpdater.STDLIB_MAGIC_FOLDER_NAME) //
            ) {
               final var prefs = HaxeProjectPreference.get(asNonNullUnsafe(project));
               final var haxeSDK = prefs.getEffectiveHaxeSDK();
               return "Haxe Standard Library" + (haxeSDK == null ? "" : " [" + haxeSDK.getVersion() + "]");
            }

            if (folder.isVirtual() //
               && folder.getName().equals(HaxeDependenciesUpdater.DEPS_MAGIC_FOLDER_NAME) //
            ) {
               int depCount = 0;
               try {
                  depCount = folder.members().length;
               } catch (final CoreException ex) {
                  Haxe4EPlugin.log().error(ex);
               }
               return "Haxe Dependencies" + (depCount == 0 ? "" : " (" + depCount + ")");
            }
         }
      }
      return text;
   }

   public void refreshElements(final IResource @Nullable... res) {
      if (res == null || res.length == 0)
         return;

      UI.run(() -> fireLabelProviderChanged(new LabelProviderChangedEvent(this, res)));
   }
}
