/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.formatting;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;

import formatter.Formatter;
import formatter.config.Config;

/**
 * @author Sebastian Thomschke
 */
public final class Format {

   /**
    * locate custom formatter config
    */
   @Nullable
   public static Config getFormatterConfig(@Nullable final IFile file) {
      if (file == null)
         return null;

      var parent = file.getParent();
      while (true) {
         final var cfgFile = parent.findMember("hxformat.json");
         if (cfgFile instanceof IFile && cfgFile.exists())
            return Formatter.loadConfig(cfgFile.getLocation().toOSString());
         if (parent == parent.getProject()) {
            break;
         }
         parent = parent.getParent();
      }
      return null;
   }

   private Format() {
   }
}
