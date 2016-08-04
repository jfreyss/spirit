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

package com.actelion.research.spiritcore.business.biosample;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.CompareUtils;

public class LocPos implements Comparable<LocPos> {

	
	private Location location;
	private int pos = -1;
	
	
	public LocPos() {
	}
	
	public LocPos(Location location) {
		this(location, -1);		
	}
	public LocPos(Location location, int pos) {
		this.location = location;		
		this.pos = pos;
	}
	
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}


//	/**
//	 * Format the indexed position to A/01, 1,... 
//	 * @return
//	 */
//	public String formatPosition() {
//		if(getLocation()==null) throw new IllegalArgumentException("Invalid location");
//		return getLocation().getPositionType().formatPosition(location, pos);	
//	}
	
	public int getRow() {
		if(getLocation()==null) return 0; 
		return getLocation().getLabeling().getRow(location, pos);
	}
	
	public int getCol() {
		if(getLocation()==null) return 0; 
		return getLocation().getLabeling().getCol(location, pos);
	}
	
	
	@Override
	public String toString() {
		return location==null?"": location.getLabeling().formatPosition(location, pos);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null) return false;
		if(! (obj instanceof LocPos)) return false;
		LocPos l = (LocPos) obj;
		return CompareUtils.compare(location, l.location)==0 && pos==l.pos;
	}
	
	@Override
	public int compareTo(LocPos o) {
		int c = CompareUtils.compare(location, o.getLocation());
		if(c!=0) return c;
		return pos-o.pos;
	}
	
	
}
