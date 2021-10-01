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

/**
 * @author Sebastian Thomschke
 */
public final class BundleResourceUtils {

   public static File extractBundleResource(final String resource) throws IOException, URISyntaxException {
      final var bundle = Platform.getBundle(Haxe4EPlugin.PLUGIN_ID);
      var url = FileLocator.find(bundle, new Path(resource), null);
      url = FileLocator.toFileURL(url); // extract the file
      return URIUtil.toFile(URIUtil.toURI(url));
   }

   public static URL getBundleResourceUrl(final String resource) {
      final var bundle = Platform.getBundle(Haxe4EPlugin.PLUGIN_ID);
      return bundle.getResource("src/main/resources/" + resource);
   }

   public static InputStream getBundleResourceAsStream(final String resource) throws IOException {
      final var url = getBundleResourceUrl(resource);
      final var con = url.openConnection();
      return con.getInputStream(); // responsibility of caller to close stream when done
   }

   public static String getBundleResourceAsString(final String resource) throws IOException {
      final var stream = getBundleResourceAsStream(resource);
      final var writer = new StringWriter();
      IOUtils.copy(stream, writer, StandardCharsets.UTF_8);
      stream.close();
      return writer.toString();
   }

   private BundleResourceUtils() {
   }
}
