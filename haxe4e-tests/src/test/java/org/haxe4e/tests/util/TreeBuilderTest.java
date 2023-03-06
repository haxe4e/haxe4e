/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.tests.util;

import static org.assertj.core.api.Assertions.*;

import org.haxe4e.util.TreeBuilder;
import org.junit.Test;

/**
 * @author Sebastian Thomschke
 */
public class TreeBuilderTest {

   @Test
   public void testTreeBuilder() {
      final var mb = new TreeBuilder<String>();

      assertThat(mb.getMap()).isEmpty();
      assertThatIllegalArgumentException().isThrownBy(() -> mb.put("foo", mb.getMap()));

      mb.put("a", true);
      mb.put("b", 1);
      mb.put("c", "");
      mb.put("d", (String) null);
      assertThat(mb.getMap()).hasSize(3);
   }
}
