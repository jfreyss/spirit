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

import javax.swing.JComponent;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class LocationColsColumn extends Column<Location, String> {
	
	public LocationColsColumn() {
		super("Cols", String.class, 35);				
	}
	@Override
	public String getValue(Location row) {
		return row.getCols()>0? "" + row.getCols(): "";
	}
	@Override
	public void setValue(Location row, String value) {
		if(value==null || value.length()==0) {
			row.setCols(-1);
		} else {
			try {
				row.setCols(Integer.parseInt(value.trim()));
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public boolean isEditable(Location row) {
		return row!=null && row.getLocationType()!=null && row.getLocationType().getCategory()==LocationCategory.MOVEABLE;
	}

	@Override
	public void postProcess(AbstractExtendTable<Location> table, Location row, int rowNo, Object value, JComponent comp) {
//		((JLabelNoRepaint)comp).setHorizontalAlignment(SwingConstants.CENTER);
	}
}