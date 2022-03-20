/*
 * Copyright 2021-2022, 2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.tests.model.buildsystem;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Paths;
import java.util.List;

import org.haxe4e.model.buildsystem.HaxeBuildFile;
import org.haxe4e.model.buildsystem.LimeBuildFile;
import org.junit.Test;

import net.sf.jstuff.core.io.RuntimeIOException;
import net.sf.jstuff.xml.DOMFile;

/**
 * @author Sebastian Thomschke
 */
public class BuildFileTest {

   @Test
   public void testHaxeBuildFile() {
      final var buildFile = new HaxeBuildFile(null) {
         @Override
         public List<String> parseArgs() throws RuntimeIOException {
            return parseArgs(Paths.get("src/test/resources/test.hxml"));
         }
      };

      final var args = buildFile.parseArgs();
      assertThat(args).contains( //
         "-cp", "src", //
         "-p", "dir/another src", //
         "--class-path", "dir/yet another src", //
         //
         "-lib", "lib1", //
         "-L", "lib2", //
         "--library", "lib3:1.0.0"//
      ).doesNotContain( //
         "commented-out-src", //
         "commented-out-lib:1.0.0" //
      );

      assertThat(buildFile.getSourcePaths()).containsExactly( //
         Paths.get("src"), //
         Paths.get("dir/another src"), //
         Paths.get("dir/yet another src") //
      );
   }

   @Test
   public void testLimeBuildFile() {
      final var buildFile = new LimeBuildFile(null) {
         @Override
         public DOMFile parseFile() throws RuntimeIOException {
            return parseFile(Paths.get("src/test/resources/lime.xml").toFile());
         }
      };

      assertThat(buildFile.getSourcePaths()).containsExactly( //
         Paths.get("src"), //
         Paths.get("dir/another src"), //
         Paths.get("dir/yet another src") //
      );
   }
}
