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

package com.actelion.research.spiritapp.spirit.ui.location;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.lf.SpiritExtendTable;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.util.ui.FastFont;

public class LocationTable extends SpiritExtendTable<Location> {
	
	public LocationTable() {
		this(new LocationTableModel());
	}
	
	public LocationTable(LocationTableModel model) {
		super(model);
		LocationActions.attachPopup(this);
		setBorderStrategy(BorderStrategy.NO_BORDER);
		setRowHeight(FastFont.getDefaultFontSize()+3);		
	}
	
	@Override
	public LocationTableModel getModel() {
		return (LocationTableModel) super.getModel();
	}
	
	/**
	 * Sets the rows of the table, while adding the complete parent hierarchy
	 */
	@Override
	public void setRows(List<Location> rows) {
		System.out.println("LocationTable.setRows() "+rows);
		List<Location> rowsWithHierarchy = new ArrayList<>();

		//We must add the roots and the specified rows
		List<Location> toBeAdded = new ArrayList<>(rows);
		toBeAdded.addAll(DAOLocation.getLocationRoots(Spirit.getUser()));
		
		//Make sure we have all the parents
		Set<Location> present = new HashSet<>();
		for (Location loc : toBeAdded) {
			while(loc!=null && !present.contains(loc)) {
				rowsWithHierarchy.add(loc);
				present.add(loc);
				loc = loc.getParent();
			}
		}
		Collections.sort(rowsWithHierarchy);

		super.setRows(rowsWithHierarchy);
	}
	
	public void addRow(Location loc) {
		List<Location> rows = getRows();
		rows.add(loc);
		setRows(rows);
	}
	
	
}
