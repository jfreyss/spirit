/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.location.LocationLabel;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.AlphaNumericalCellEditor;
import com.actelion.research.util.ui.exceltable.Column;

public class LocationNameColumn extends Column<Location, String> {

	private LocationLabel label = new LocationLabel(false);

	public LocationNameColumn() {
		super("Name", String.class, 140);
	}

	public void setDiplayIcon(boolean displayIcon) {
		label.setDisplayIcon(displayIcon);
	}

	@Override
	public String getToolTipText(Location row) {
		if(row.getLocationType()==null) return null;
		return row.getLocationType().getName() + ": " + row.getName();
	}

	@Override
	public String getValue(Location row) {
		return row.getHierarchyFull();
	}

	@Override
	public void setValue(Location row, String value) {
		if(value==null) {
			row.setName("");
		} else {
			int index = value.lastIndexOf( Location.SEPARATOR);
			row.setName(index<0? value: value.substring(index+1));
		}
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Location> table) {
		return new AlphaNumericalCellEditor() {
			@Override
			public JCustomTextField getTableCellEditorComponent(JTable table, Object v, boolean isSelected, int row, int column) {
				JCustomTextField c = super.getTableCellEditorComponent(table, v, isSelected, row, column);
				String value = (String) v;
				if(value==null) {
					c.setText("");
				} else {
					int index = value.lastIndexOf(Location.SEPARATOR);
					c.setText(index<0? value: value.substring(index+1));
				}
				return c;
			}
		};
	}

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Location> table, Location row, int rowNo, Object value) {
		label.setLocation(row);
		label.setForeground(row==null || row.getPrivacy()==null? Color.BLACK: row.getPrivacy().getFgColor());
		label.setFont(row==null || row.getLocationType().getCategory()==LocationCategory.MOVEABLE? FastFont.REGULAR:
			row.getLocationType()==LocationType.BUILDING? FastFont.BIG:
				FastFont.BOLD);
		return label;
	}

	@Override
	public void postProcess(AbstractExtendTable<Location> table, Location row, int rowNo, Object value, JComponent comp) {
		comp.setForeground(row==null || row.getPrivacy()==null? Color.BLACK: row.getPrivacy().getFgColor());
	}

	@Override
	public boolean isEditable(Location row) {
		return SpiritRights.canEdit(row, SpiritFrame.getUser());
	}
}