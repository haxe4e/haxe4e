/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
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
   public static final String PLUGIN_ID = Haxe4EPlugin.class.getPackage().getName();

   private static Haxe4EPlugin instance;

   /**
    * @return the shared instance
    */
   public static Haxe4EPlugin get() {
      Assert.notNull(instance, "Default plugin instance is still null.");
      return instance;
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
      if (bundleResources == null) {
         bundleResources = new BundleResources(this, "src/main/resources");
      }
      return bundleResources;
   }

   @Override
   protected void initializeImageRegistry(final ImageRegistry registry) {
      for (final var field : Constants.class.getFields()) {
         if (Fields.isStatic(field) && field.getType() == String.class && field.getName().startsWith("IMAGE_")) {
            registerImage(registry, Fields.read(null, field));
         }
      }
   }

   @Override
   public void start(final BundleContext context) throws Exception {
      super.start(context);
      instance = this;

      WindowListener.INSTANCE.attach();
      ResourcesPlugin.getWorkspace().addResourceChangeListener(HaxeDependenciesUpdater.INSTANCE, IResourceChangeEvent.POST_CHANGE);
   }

   @Override
   public void stop(final BundleContext context) throws Exception {
      WindowListener.INSTANCE.detatch();
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(HaxeDependenciesUpdater.INSTANCE);

      instance = null;
      super.stop(context);
   }
}
