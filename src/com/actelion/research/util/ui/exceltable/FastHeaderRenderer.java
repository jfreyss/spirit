/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
 * CH-4123 Allschwil, Switzerland.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.util.ui.exceltable;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

/**
 * Wrap a JLabelNoRepaintWithArrow, implementing TableCellRenderer.
 * Used by AbstractTable to display the header with the sorting arrow
 * @author freyssj
 *
 * @param <T>
 */
public class FastHeaderRenderer<T> implements TableCellRenderer {
	
	private final JLabelNoRepaintWithArrow label = new JLabelNoRepaintWithArrow();
	
	public FastHeaderRenderer() {
		label.setVerticalAlignment(SwingConstants.BOTTOM);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		AbstractExtendTable<T> t = (AbstractExtendTable<T>) table;
		int modelColumn = t.convertColumnIndexToModel(column);
		Column<T, ?> col = modelColumn<t.getModel().getColumnCount()? t.getModel().getColumn(t.convertColumnIndexToModel(column)): null;
		Column<T, ?> nextCol = column+1<t.getModel().getColumnCount()? t.getModel().getColumn(t.convertColumnIndexToModel(column+1)): null;
		Column<T, ?> prevCol = column-1>=0 && column-1<t.getModel().getColumnCount()? t.getModel().getColumn(t.convertColumnIndexToModel(column-1)): null;
		
		if(t.getCurrentSortColumn()!=null && t.getCurrentSortColumn()==modelColumn+1) {
			label.setArrow(1);
		} else if(t.getCurrentSortColumn()!=null && -t.getCurrentSortColumn()==modelColumn+1) {
			label.setArrow(-1);
		} else {
			label.setArrow(0);
		}
		if(table instanceof AbstractExtendTable) {
			label.setCondenseText(((AbstractExtendTable<T>)table).isPreferredCondenseText());
		}
		
		label.setForeground(Color.BLACK);
		label.setBackground(table.getBackground());
		
		String str = col==null?"": col.getName();
		if(col!=null && prevCol!=null && col.getCategory().length()>0 && prevCol.getCategory().equals(col.getCategory())) {
			int index = str.indexOf('\n');
			if(index>0 && index<str.length() && str.indexOf('\n', index+1)<0) str = str.substring(index+1);
		}
		
		label.setText(str);
		
		boolean rightBorder = nextCol==null || col==null || col.getCategory()=="" || !col.getCategory().equalsIgnoreCase(nextCol.getCategory());
		Border border;
		if(rightBorder) {
			border = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.BLACK);
		} else {
			border = BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
					BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
		}
		label.setBorder(border);

		return label;
	}
}
