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

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.lf.SpiritExtendTable;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.util.ui.FastFont;


/**
 * LocationTable is used to represent an hierarchical view of locations
 *
 * @author Joel Freyss
 *
 */
public class LocationTable extends SpiritExtendTable<Location> {

	public static enum LocationTableMode {
		ALL,
		ADMIN_LOCATION,
		USER_LOCATION
	}

	private boolean showHierarchy = true;

	public LocationTable() {
		this(LocationTableMode.ALL);
	}

	public LocationTable(LocationTableMode mode) {
		super(new LocationTableModel(mode));
		LocationActions.attachPopup(this);
		setBorderStrategy(BorderStrategy.NO_BORDER);
		setRowHeight(FastFont.getDefaultFontSize()+4);
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
		List<Location> rowsWithHierarchy = new ArrayList<>();

		if(showHierarchy) {
			//Make sure we have all the parents
			List<Location> toBeAdded = new ArrayList<>(rows);
			toBeAdded.addAll(DAOLocation.getLocationRoots(SpiritFrame.getUser()));
			Set<Location> present = new HashSet<>();
			for (Location loc : toBeAdded) {
				while(loc!=null && !present.contains(loc)) {
					rowsWithHierarchy.add(loc);
					present.add(loc);
					//				if(loc.getParent()!=null) rowsWithHierarchy.addAll(loc.getParent().getChildren());
					loc = loc.getParent();

				}
			}
		} else {
			rowsWithHierarchy.addAll(rows);
		}
		Collections.sort(rowsWithHierarchy);

		super.setRows(rowsWithHierarchy);
	}

	public void addRow(Location loc) {
		List<Location> rows = getRows();
		if(!rows.contains(loc)) {
			rows.add(loc);
			setRows(rows);
		}
	}

	public void setShowHierarchy(boolean showHierarchy) {
		this.showHierarchy = showHierarchy;
	}
	public boolean isShowHierarchy() {
		return showHierarchy;
	}

}
