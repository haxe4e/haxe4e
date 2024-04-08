/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ImageRegistry;
import org.haxe4e.navigation.HaxeDependenciesUpdater;
import org.haxe4e.navigation.WindowListener;
import org.osgi.framework.BundleContext;

import de.sebthom.eclipse.commons.AbstractEclipsePlugin;
import de.sebthom.eclipse.commons.BundleResources;
import de.sebthom.eclipse.commons.logging.PluginLogger;
import de.sebthom.eclipse.commons.logging.StatusFactory;
import net.sf.jstuff.core.reflection.Fields;
import net.sf.jstuff.core.validation.Assert;

/**
 * @author Sebastian Thomschke
 */
public class Haxe4EPlugin extends AbstractEclipsePlugin {

   /**
    * during runtime you can get ID with getBundle().getSymbolicName()
    */
   public static final String PLUGIN_ID = asNonNull(Haxe4EPlugin.class.getPackage()).getName();

   private static @Nullable Haxe4EPlugin instance;

   /**
    * @return the shared instance
    */
   public static Haxe4EPlugin get() {
      Assert.notNull(instance, "Default plugin instance is still null.");
      return asNonNullUnsafe(instance);
   }

   public static PluginLogger log() {
      return get().getLogger();
   }

   public static BundleResources resources() {
      return get().getBundleResources();
   }

   public static StatusFactory status() {
      return get().getStatusFactory();
   }

   @Override
   public BundleResources getBundleResources() {
      var bundleResources = this.bundleResources;
      if (bundleResources == null) {
         bundleResources = this.bundleResources = new BundleResources(this, "src/main/resources");
      }
      return bundleResources;
   }

   @Override
   protected void initializeImageRegistry(final ImageRegistry registry) {
      for (final var field : Constants.class.getFields()) {
         if (Fields.isStatic(field) && field.getType() == String.class && field.getName().startsWith("IMAGE_")) {
            final String imagePath = Fields.read(null, field);
            if (imagePath != null) {
               registerImage(registry, imagePath);
            }
         }
      }
   }

   @Override
   public void start(final BundleContext context) throws Exception {
      super.start(context);
      instance = this;

      HaxeDependenciesUpdater.INSTANCE.install();
      WindowListener.INSTANCE.attach();
   }

   @Override
   public void stop(final BundleContext context) throws Exception {
      HaxeDependenciesUpdater.INSTANCE.uninstall();
      WindowListener.INSTANCE.detatch();

      instance = null;
      super.stop(context);
   }
}
