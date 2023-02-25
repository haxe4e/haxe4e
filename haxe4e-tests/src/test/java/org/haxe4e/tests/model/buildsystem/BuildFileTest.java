/*
 * Copyright 2021-2022, 2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.tests.model.buildsystem;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
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
      final var parentMock = asNonNull(mock(IContainer.class));
      final var fileMock = asNonNull(mock(IFile.class));
      when(fileMock.getLocation()).thenReturn(Path.fromOSString("test.hxml"));
      when(fileMock.getParent()).thenReturn(parentMock);
      when(parentMock.getProjectRelativePath()).thenReturn(Path.fromOSString(""));
      final var buildFile = new HaxeBuildFile(fileMock) {
         @Override
         public List<String> getArgs() throws RuntimeIOException {
            return parseArgs(Paths.get("src/test/resources/test.hxml"));
         }
      };

      final var args = buildFile.getArgs();
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
         Path.fromOSString("src"), //
         Path.fromOSString("dir/another src"), //
         Path.fromOSString("dir/yet another src") //
      );
   }

   @Test
   public void testLimeBuildFile() {
      final var parentMock = asNonNull(mock(IContainer.class));
      final var fileMock = asNonNull(mock(IFile.class));
      when(fileMock.getLocation()).thenReturn(Path.fromOSString("lime.xml"));
      when(fileMock.getParent()).thenReturn(parentMock);
      when(parentMock.getProjectRelativePath()).thenReturn(Path.fromOSString(""));

      final var buildFile = new LimeBuildFile(fileMock) {
         @Override
         public DOMFile parseFile() throws RuntimeIOException {
            return parseFile(Paths.get("src/test/resources/lime.xml").toFile());
         }
      };

      assertThat(buildFile.getSourcePaths()).containsExactly( //
         Path.fromOSString("src"), //
         Path.fromOSString("dir/another src"), //
         Path.fromOSString("dir/yet another src") //
      );
   }
}
