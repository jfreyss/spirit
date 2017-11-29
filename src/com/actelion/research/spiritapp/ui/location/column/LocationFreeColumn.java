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

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.exceltable.Column;

public class LocationFreeColumn extends Column<Location, Integer> {		
	public LocationFreeColumn() {
		super("Free", Integer.class);
	}
	@Override
	public Integer getValue(Location row) {
		return row.getSize()<=0? null: Math.max(0,  row.getSize() - row.getOccupancy());
	}
	@Override
	public String getToolTipText() {
		return "Free space";
	}
	
	@Override
	public boolean isHideable() {
		return true;
	}
}