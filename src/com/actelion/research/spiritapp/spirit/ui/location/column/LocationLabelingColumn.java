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

package com.actelion.research.spiritapp.spirit.ui.location.column;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class LocationLabelingColumn extends Column<Location, LocationLabeling> {

	private class LocationLabelingCellEditor extends AbstractCellEditor implements  TableCellEditor {

		private JGenericComboBox<LocationLabeling> comboBox = new JGenericComboBox<LocationLabeling>(LocationLabeling.values(), false);

		public LocationLabelingCellEditor() {
			comboBox.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		}
		@Override
		public Object getCellEditorValue() {
			return comboBox.getSelection();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			comboBox.setSelection((LocationLabeling) value);
			return comboBox;
		}

	}

	public LocationLabelingColumn() {
		super("Labeling", LocationLabeling.class, 50);
	}
	@Override
	public LocationLabeling getValue(Location row) {
		return row.getLabeling();
	}
	@Override
	public void setValue(Location row, LocationLabeling value) {
		row.setLabeling(value);
	}

	@Override
	public void paste(Location row, String value) throws Exception {
		setValue(row, LocationLabeling.get(value));
	}

	@Override
	public boolean isEditable(Location row) {
		return row.getLocationType()!=null && row.getLocationType().getCategory()==LocationCategory.MOVEABLE;
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Location> table) {
		return new LocationLabelingCellEditor();
	}

}