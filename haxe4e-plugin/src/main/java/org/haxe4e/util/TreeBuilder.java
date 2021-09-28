/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import net.sf.jstuff.core.validation.Args;

/**
 * Builder to create Map with maps.
 *
 * @author Sebastian Thomschke
 */
public final class TreeBuilder<K> {

   private final Map<K, Object> map;

   public TreeBuilder() {
      this.map = new TreeMap<>();
   }

   public TreeBuilder(final Map<K, Object> map) {
      this.map = map;
   }

   public TreeBuilder<K> compute(final Consumer<TreeBuilder<K>> action) {
      Args.notNull("action", action);
      action.accept(this);
      return this;
   }

   @SuppressWarnings("unchecked")
   public TreeBuilder<K> compute(final K k, final Consumer<TreeBuilder<K>> action) {
      Args.notNull("action", action);

      final TreeBuilder<K> leafBuilder;
      final var leaf = map.get(k);
      if (leaf instanceof Map) {
         leafBuilder = new TreeBuilder<>((Map<K, Object>) leaf);
      } else {
         leafBuilder = new TreeBuilder<>();
      }
      action.accept(leafBuilder);
      map.put(k, leafBuilder.getMap());

      return this;
   }

   public Map<K, Object> getMap() {
      return map;
   }

   public TreeBuilder<K> put(final K k, final Boolean v) {
      if (v == null)
         return this;

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final K k2, final Boolean v) {
      put(k, k2, (Object) v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final K k2, final List<String> v) {
      put(k, k2, (Object) v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final K k2, final Number v) {
      put(k, k2, (Object) v);
      return this;
   }

   @SuppressWarnings("unchecked")
   private void put(final K k, final K k2, final Object v) {
      if (v == null)
         return;
      final var leaf = map.get(k);
      if (leaf instanceof Map) {
         ((Map<K, Object>) leaf).put(k2, v);
      } else {
         final var newLeaf = new TreeMap<K, Object>();
         newLeaf.put(k2, v);
         map.put(k, newLeaf);
      }
   }

   public TreeBuilder<K> put(final K k, final K k2, final String v) {
      put(k, k2, (Object) v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final List<String> v) {
      if (v == null)
         return this;

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final Map<K, ?> v) {
      if (v == null)
         return this;

      if (map == v)
         throw new IllegalArgumentException("[v] Illegal self-reference");

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final Number v) {
      if (v == null)
         return this;

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final String v) {
      if (v == null)
         return this;

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final TreeBuilder<K> v) {
      if (v == null)
         return this;

      if (this == v)
         throw new IllegalArgumentException("[v] Illegal self-reference");

      map.put(k, v.getMap());
      return this;
   }
}
