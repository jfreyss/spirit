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
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.lf.EmployeeGroupComboBox;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class LocationDepartmentColumn extends Column<Location, EmployeeGroup> {
	
	private class LocationDepartmentCellEditor extends AbstractCellEditor implements  TableCellEditor {

		private EmployeeGroupComboBox comboBox = new EmployeeGroupComboBox(true);

		@Override
		public Object getCellEditorValue() {
			return comboBox.getSelection();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			comboBox.setSelection((EmployeeGroup) value);
			return comboBox;
		}
	}
		
		
	public LocationDepartmentColumn() {
		super("Department", EmployeeGroup.class, 70);
	}
	@Override
	public EmployeeGroup getValue(Location row) {
		return row.getEmployeeGroup();
	}
	@Override
	public void setValue(Location row, EmployeeGroup value) {
		row.setEmployeeGroup(value);
	}
	@Override
	public void paste(Location row, String value) throws Exception {
		if(value==null || value.length()==0) {
			row.setEmployeeGroup(null);
		} else {
			List<EmployeeGroup> res = new ArrayList<EmployeeGroup>();
			for(EmployeeGroup eg: DBAdapter.getAdapter().getEmployeeGroups()) {
				if(eg.getName().equalsIgnoreCase(value)) {
					res.add(eg);
				}				
			}
			if(res.size()==0) throw new Exception("There are no groups called: "+value);
			if(res.size()>1) throw new Exception("There are several groups called: "+value);
			setValue(row, res.get(0));
		}
	}
	
	@Override
	public boolean isEditable(Location row) {
		return row!=null && (row.getPrivacy()==Privacy.PRIVATE || row.getPrivacy()==Privacy.PROTECTED);
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Location> table) {
		return new LocationDepartmentCellEditor();
	}
	@Override
	public boolean isHideable() {
		return true;
	}
}