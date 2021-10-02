/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.haxe4e.Haxe4EPlugin;
import org.osgi.framework.Bundle;

import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public final class BundleResourceUtils {

   private static final String RESOURCE_PATH_PREFIX = "src/main/resources/";

   /**
    * @throws IllegalArgumentException if given resource cannot be found
    */
   public static File extractBundleResource(String resource) throws IOException {
      Args.notBlank("resource", resource);

      resource = RESOURCE_PATH_PREFIX + resource;
      var url = FileLocator.find(getBundle(), new Path(resource), null);
      if (url == null)
         throw new IllegalArgumentException("Resource not found: " + resource);
      url = FileLocator.toFileURL(url); // extract the file
      try {
         return URIUtil.toFile(URIUtil.toURI(url));
      } catch (final URISyntaxException ex) {
         throw new IOException(ex);
      }
   }

   public static Bundle getBundle() {
      return Platform.getBundle(Haxe4EPlugin.PLUGIN_ID);
   }

   /**
    * @throws IllegalArgumentException if given resource cannot be found
    */
   public static InputStream getBundleResourceAsStream(final String resource) throws IOException {
      final var url = getBundleResourceUrl(resource);
      final var con = url.openConnection();
      return con.getInputStream(); // responsibility of caller to close stream when done
   }

   /**
    * @throws IllegalArgumentException if given resource cannot be found
    */
   public static String getBundleResourceAsString(final String resource) throws IOException {
      try (var stream = getBundleResourceAsStream(resource)) {
         final var writer = new StringWriter();
         IOUtils.copy(stream, writer, StandardCharsets.UTF_8);
         return writer.toString();
      }
   }

   /**
    * @throws IllegalArgumentException if given resource cannot be found
    */
   public static URL getBundleResourceUrl(String resource) {
      Args.notBlank("resource", resource);
      resource = RESOURCE_PATH_PREFIX + resource;

      final var url = getBundle().getEntry(resource);
      if (url == null)
         throw new IllegalArgumentException("Resource not found: " + resource);
      return url;
   }

   private BundleResourceUtils() {
   }
}
