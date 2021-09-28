/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

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

   private BundleResourceUtils() {
   }
}
