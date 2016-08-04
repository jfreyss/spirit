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
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class LocationPrivacyColumn extends Column<Location, Privacy> {
	
	
	private class LocationPrivacyCellEditor extends AbstractCellEditor implements  TableCellEditor {

		private JGenericComboBox<Privacy> comboBox = new JGenericComboBox<Privacy>(Privacy.values(), false);

		@Override
		public Object getCellEditorValue() {
			return comboBox.getSelection();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			comboBox.setSelection((Privacy) value);
			return comboBox;
		}
		
	}
	
	public LocationPrivacyColumn() {
		super("Privacy", Privacy.class);				
	}
	@Override
	public Privacy getValue(Location row) {
		return row.getPrivacy();
	}
	@Override
	public void setValue(Location row, Privacy value) {
		row.setPrivacy(value);
	}
	@Override
	public void paste(Location row, String value) throws Exception {
		Privacy p = Privacy.get(value);
		setValue(row, p==null? Privacy.INHERITED: p);
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Location> table) {
		return new LocationPrivacyCellEditor();
	}
}