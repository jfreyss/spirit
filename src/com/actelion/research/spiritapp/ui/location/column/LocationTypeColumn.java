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
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.location.LocationTypeComboBox;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class LocationTypeColumn extends Column<Location, LocationType> {


	private class LocationTypeCellEditor extends AbstractCellEditor implements  TableCellEditor {

		private LocationTypeComboBox locationTypeComboBox = new LocationTypeComboBox();

		public LocationTypeCellEditor() {
			locationTypeComboBox.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		}
		@Override
		public Object getCellEditorValue() {
			return locationTypeComboBox.getSelection();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			locationTypeComboBox.setSelection((LocationType) value);
			return locationTypeComboBox;
		}

	}

	public LocationTypeColumn() {
		super("Type", LocationType.class, 70);
	}
	@Override
	public LocationType getValue(Location row) {
		return row.getLocationType();
	}
	@Override
	public void setValue(Location row, LocationType value) {
		LocationType previous = row.getLocationType();
		row.setLocationType(value);
		if(previous==null && value!=null) {
			row.setRows(value.getDefaultRows());
			row.setCols(value.getDefaultCols());
		}
	}
	@Override
	public void postProcess(AbstractExtendTable<Location> table, Location row, int rowNo, Object value, JComponent comp) {
		((JLabelNoRepaint)comp).setIcon(row==null || row.getLocationType()==null? null: new ImageIcon(row.getLocationType().getImageThumbnail()));
	}
	@Override
	public void paste(Location row, String value) throws Exception {
		for (LocationType l : LocationType.values()) {
			if(l.getName().equalsIgnoreCase(value)) {
				setValue(row, l);
				return;
			}
		}
		setValue(row, null);
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Location> table) {
		return new LocationTypeCellEditor();
	}

	@Override
	public boolean isHideable() {
		return true;
	}
}