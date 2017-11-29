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
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.location.edit.LocationEditTable;
import com.actelion.research.spiritapp.ui.util.lf.EmployeeGroupComboBox;
import com.actelion.research.spiritapp.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.util.ui.JObjectComboBox;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class LocationPrivacyColumn extends Column<Location, String> {


	private class LocationPrivacyCellEditor extends AbstractCellEditor implements  TableCellEditor {

		private JObjectComboBox<Privacy> privacyComboBox = new JObjectComboBox<>(Privacy.values());
		private EmployeeGroupComboBox deptComboBox = new EmployeeGroupComboBox(true);
		private JPanel editor = UIUtils.createHorizontalBox(privacyComboBox, deptComboBox);


		public LocationPrivacyCellEditor() {
			privacyComboBox.setColumns(15);
			privacyComboBox.addTextChangeListener(e-> onChange());
			privacyComboBox.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		}

		private void onChange() {
			deptComboBox.setEnabled(privacyComboBox.getSelection()==Privacy.PRIVATE || privacyComboBox.getSelection()==Privacy.PROTECTED);
		}

		@Override
		public Object getCellEditorValue() {
			String s = (privacyComboBox.getSelection()==null?"": privacyComboBox.getSelection().getName()) + (deptComboBox.isEnabled() && deptComboBox.getSelection()!=null? " to " + deptComboBox.getSelection().getName():"");
			return s;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			if(row>=((LocationEditTable) table).getModel().getRows().size()) {
				System.err.println("Invalid row "+row);
				return null;
			}
			Location loc = ((LocationEditTable) table).getModel().getRows().get(row);
			privacyComboBox.setSelection(loc.getPrivacy());
			deptComboBox.setSelection(loc.getEmployeeGroup());
			onChange();
			return editor;
		}

	}

	public LocationPrivacyColumn() {
		super("Privacy", String.class, 140);
		setHideable(true);
	}

	@Override
	public String getValue(Location row) {
		return row.getPrivacy()==null || row.getPrivacy()==Privacy.INHERITED?"": row.getPrivacy().getName() + ((row.getPrivacy()==Privacy.PROTECTED || row.getPrivacy()==Privacy.PRIVATE) && row.getEmployeeGroup()!=null? " to " +row.getEmployeeGroup().getName():"");
	}

	@Override
	public void postProcess(AbstractExtendTable<Location> table, Location row, int rowNo, Object value, JComponent comp) {
		if((row.getPrivacy()==Privacy.PROTECTED || row.getPrivacy()==Privacy.PRIVATE) && row.getEmployeeGroup()==null) {
			comp.setForeground(LF.COLOR_ERROR_FOREGROUND);
		}
	}

	@Override
	public void setValue(Location row, String value) {
		if(value==null) return;
		int index = value.indexOf(" to ");
		String privacy = value;
		String dept = "";
		if(index>0) {
			privacy = value.substring(0, index);
			dept = value.substring(index+4);
		}
		row.setPrivacy(Privacy.get(privacy));
		row.setEmployeeGroup((row.getPrivacy()==Privacy.PROTECTED || row.getPrivacy()==Privacy.PRIVATE)? DAOEmployee.getEmployeeGroup(dept): null);
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Location> table) {
		return new LocationPrivacyCellEditor();
	}
}