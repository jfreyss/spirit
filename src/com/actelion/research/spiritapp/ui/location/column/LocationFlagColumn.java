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

package com.actelion.research.spiritapp.ui.location.column;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationFlag;
import com.actelion.research.util.ui.JObjectComboBox;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class LocationFlagColumn extends Column<Location, LocationFlag> {


	private class LocationFlagCellEditor extends AbstractCellEditor implements  TableCellEditor {

		private JObjectComboBox<LocationFlag> combo = new JObjectComboBox<>();

		public LocationFlagCellEditor() {
			combo.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		}
		@Override
		public Object getCellEditorValue() {
			return combo.getSelection();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			LocationFlag flag = (LocationFlag) value;
			combo.setSelection(flag);
			combo.setIcon(flag==null? null: flag.getIcon());
			return combo;
		}
	}

	public LocationFlagColumn() {
		super("Flag", LocationFlag.class, 10);
	}

	@Override
	public LocationFlag getValue(Location row) {
		return row.getLocationFlag();
	}

	@Override
	public void setValue(Location row, LocationFlag value) {
		row.setLocationFlag(value);
	}

	@Override
	public void paste(Location row, String value) throws Exception {
		for (LocationFlag l : LocationFlag.values()) {
			if(l.getName().equalsIgnoreCase(value)) {
				setValue(row, l);
				return;
			}
		}
		setValue(row, null);
	}

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Location> table, Location row, int rowNo, Object value) {
		LocationFlag flag = (LocationFlag) value;
		JLabelNoRepaint lbl = (JLabelNoRepaint)super.getCellComponent(table, row, rowNo, value);
		lbl.setIcon(flag==null? null: flag.getIcon());
		lbl.setToolTipText(flag==null? null: flag.getName());
		lbl.setText("");
		return lbl;
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Location> table) {
		return new LocationFlagCellEditor();
	}

}