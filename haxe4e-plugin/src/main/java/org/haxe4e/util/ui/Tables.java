/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.util.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * @author Sebastian Thomschke
 */
public final class Tables {

   public static void autoResizeColumns(final TableViewer table) {
      autoResizeColumns(table.getTable());
   }

   public static void autoResizeColumns(final Table table) {
      for (final TableColumn column : table.getColumns()) {
         column.pack();
      }
   }

   public static void setLastColumnAutoExpand(final Table table, final int minWidth) {
      table.addControlListener(new ControlAdapter() {
         Set<TableColumn> monitoredColumns = new HashSet<>();

         @Override
         public synchronized void controlResized(final ControlEvent event) {
            final var cols = table.getColumns();
            if (cols.length == 0)
               return;

            if (cols.length == 1) {
               cols[0].setWidth(Math.max(minWidth, table.getClientArea().width));
               return;
            }

            var totalWidthOfOtherColumns = 0;
            for (var i = 0; i < cols.length - 1; i++) {
               final var col = cols[i];
               if (!monitoredColumns.contains(col)) {
                  col.addControlListener(this);
                  monitoredColumns.add(col);
               }
               totalWidthOfOtherColumns += cols[i].getWidth();
            }
            cols[cols.length - 1].setWidth(Math.max(minWidth, table.getClientArea().width - totalWidthOfOtherColumns));
         }
      });
   }

   private Tables() {
   }
}
