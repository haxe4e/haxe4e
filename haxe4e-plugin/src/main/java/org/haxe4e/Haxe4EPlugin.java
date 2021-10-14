/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.haxe4e.navigation.HaxeDependenciesUpdater;
import org.haxe4e.navigation.WindowListener;
import org.osgi.framework.BundleContext;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.logging.Logger;
import net.sf.jstuff.core.reflection.Fields;

/**
 * @author Sebastian Thomschke
 */
public class Haxe4EPlugin extends AbstractUIPlugin {

   private static final Logger LOG = Logger.create();

   /**
    * during runtime you can get ID with getBundle().getSymbolicName()
    */
   public static final String PLUGIN_ID = Haxe4EPlugin.class.getPackage().getName();

   /**
    * the shared instance
    */
   private static Haxe4EPlugin instance;

   /**
    * @return the shared instance
    */
   public static Haxe4EPlugin getDefault() {
      if (instance == null) {
         LOG.error("Default plugin instance is still null.", new Throwable());
      }
      return instance;
   }

   public static Image getSharedImage(final String path) {
      return instance == null ? null : instance.getImageRegistry().get(path);
   }

   public static ImageDescriptor getSharedImageDescriptor(final String path) {
      return instance == null ? null : instance.getImageRegistry().getDescriptor(path);
   }

   @Override
   protected void initializeImageRegistry(final ImageRegistry registry) {
      for (final var field : Constants.class.getFields()) {
         if (Fields.isStatic(field) && field.getType() == String.class && field.getName().startsWith("IMAGE_")) {
            registerImage(registry, Fields.read(null, field));
         }
      }
   }

   private void registerImage(final ImageRegistry registry, final String path) {
      if (path.startsWith("platform:/plugin/")) {
         final var pluginId = Strings.substringBetween(path, "platform:/plugin/", "/");
         final var imagePath = Strings.substringAfter(path, pluginId);
         registry.put(path, AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, imagePath));
         return;
      }
      final var url = FileLocator.find(getBundle(), new Path(path), null);
      final var desc = ImageDescriptor.createFromURL(url);
      registry.put(path, desc);
   }

   @Override
   public void start(final BundleContext context) throws Exception {
      getLog().info("starting...");
      super.start(context);
      instance = this;

      WindowListener.INSTANCE.attach();
      ResourcesPlugin.getWorkspace().addResourceChangeListener(HaxeDependenciesUpdater.INSTANCE, IResourceChangeEvent.POST_CHANGE);
   }

   @Override
   public void stop(final BundleContext context) throws Exception {
      WindowListener.INSTANCE.detatch();
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(HaxeDependenciesUpdater.INSTANCE);

      getLog().info("stopping...");
      instance = null;
      super.stop(context);
   }
}
