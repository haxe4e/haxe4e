/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Combo;
import org.haxe4e.util.LOG;

import net.sf.jstuff.core.functional.TriConsumer;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public class ComboWrapper<E> {

   private static final IStructuredSelection EMPTY_SELECTION = new StructuredSelection();
   private Combo combo;
   private ComboViewer viewer;

   private CopyOnWriteArrayList<TriConsumer<ComboWrapper<E>, Collection<E>, Collection<E>>> itemsChangedListener = new CopyOnWriteArrayList<>();

   public ComboWrapper(final Combo combo) {
      Args.notNull("combo", combo);
      this.combo = combo;
      this.viewer = new ComboViewer(combo);
   }

   public ComboWrapper<E> clearSelection() {
      combo.clearSelection();
      return this;
   }

   @SuppressWarnings("unchecked")
   public E getItemAt(final int index) {
      return (E) viewer.getElementAt(index);
   }

   public int getItemCount() {
      return combo.getItemCount();
   }

   @SuppressWarnings("unchecked")
   public Collection<E> getItems() {
      return (Collection<E>) viewer.getInput();
   }

   @SuppressWarnings("unchecked")
   public E getSelection() {
      return (E) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
   }

   public boolean isEnabled() {
      return combo.isEnabled();
   }

   public ComboWrapper<E> onItemsChanged(final TriConsumer<ComboWrapper<E>, Collection<E>, Collection<E>> listener) {
      itemsChangedListener.add(listener);
      return this;
   }

   @SuppressWarnings("unchecked")
   public ComboWrapper<E> onSelectionChanged(final Consumer<E> listener) {
      viewer.addSelectionChangedListener(event -> listener.accept((E) event.getStructuredSelection().getFirstElement()));
      return this;
   }

   public ComboWrapper<E> setEnabled(final boolean enabled) {
      combo.setEnabled(enabled);
      return this;
   }

   public ComboWrapper<E> setItems(final Collection<E> items) {
      Args.notNull("items", items);

      viewer.setContentProvider((IStructuredContentProvider) input -> {
         final var coll = (Collection<?>) input;
         return coll.toArray(Object[]::new);
      });
      @SuppressWarnings("unchecked")
      final var old = (Collection<E>) viewer.getInput();
      viewer.setInput(items);
      for (final var l : itemsChangedListener) {
         try {
            l.accept(this, old, items);
         } catch (final Exception ex) {
            LOG.error(ex);
         }
      }
      return this;
   }

   public ComboWrapper<E> setItems(final Collection<E> items, final Comparator<E> comparator) {
      Args.notNull("items", items);
      Args.notNull("comparator", comparator);

      viewer.setContentProvider((IStructuredContentProvider) input -> {
         @SuppressWarnings("unchecked")
         final var list = new ArrayList<>((Collection<E>) input);
         list.sort(comparator);
         return list.toArray(Object[]::new);
      });
      @SuppressWarnings("unchecked")
      final var old = (Collection<E>) viewer.getInput();
      viewer.setInput(items);
      for (final var l : itemsChangedListener) {
         try {
            l.accept(this, old, items);
         } catch (final Exception ex) {
            LOG.error(ex);
         }
      }
      return this;
   }

   public ComboWrapper<E> setLabelComparator(final Comparator<? super String> comparator) {
      viewer.setComparator(new ViewerComparator(comparator));
      return this;
   }

   public ComboWrapper<E> setLabelProvider(final Function<E, String> provider) {
      viewer.setLabelProvider(new LabelProvider() {
         @SuppressWarnings("unchecked")
         @Override
         public String getText(final Object item) {
            return provider.apply((E) item);
         }
      });
      return this;
   }

   public ComboWrapper<E> setSelection(final E item) {
      final var currentSelection = getSelection();
      if (currentSelection == item)
         return this;

      viewer.setSelection(item == null ? EMPTY_SELECTION : new StructuredSelection(item));
      return this;
   }

   public ComboWrapper<E> setSelection(final E item, final boolean reveal) {
      final var currentSelection = getSelection();
      if (currentSelection == item)
         return this;

      viewer.setSelection(item == null ? EMPTY_SELECTION : new StructuredSelection(item), reveal);
      return this;
   }
}
