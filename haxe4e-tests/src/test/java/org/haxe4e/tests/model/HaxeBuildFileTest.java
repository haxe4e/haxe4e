/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.tests.model;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;

import org.haxe4e.model.HaxeBuildFile;
import org.junit.Test;

/**
 * @author Sebastian Thomschke
 */
public class HaxeBuildFileTest {

   @Test
   public void testHaxeBuildFile() throws IOException {
      final var buildFile = new HaxeBuildFile(Paths.get("src/test/resources/test.hxml"));

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

      assertThat(buildFile.getSourcePaths()).contains( //
         Paths.get("src"), //
         Paths.get("dir/another src"), //
         Paths.get("dir/yet another src") //
      );
   }
}
