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

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.location.LocationLabel;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class LocationParentColumn extends Column<Location, Location> {
	
	private LocationCellEditor editor = new LocationCellEditor();
	private static LocationLabel label = new LocationLabel(true);	

	public LocationParentColumn() {		
		super("Parent", Location.class, 110);
		label.setDisplayIcon(false);
	}
	@Override
	public Location getValue(Location row) {
		return row.getParent();
	}
	
	@Override
	public void setValue(Location row, Location value) {
		row.setParent(value);
	}
	
	@Override
	public void paste(Location row, String value) throws Exception {
		//Find if this location is already in the table
		if(getTable()!=null) {
			for (Location loc : getTable().getRows()) {
				if(value.equals(loc.getHierarchyFull())) {
					row.setParent(loc);
					return;
				}			
			}
		}
		
		//Otherwise load from the DB
		Location loc = value==null || value.length()==0? null: DAOLocation.getCompatibleLocation(value, SpiritFrame.getUser());
		row.setParent(loc);
	}
	
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Location> table, Location row, int rowNo, Object value) {
		label.setLocation(row==null? null: row.getParent());
		return label;
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Location> table) {
		return editor;
	}
}