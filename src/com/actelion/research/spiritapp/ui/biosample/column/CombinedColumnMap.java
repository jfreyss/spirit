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

package com.actelion.research.spiritapp.ui.biosample.column;

import java.util.LinkedHashMap;

public class CombinedColumnMap extends LinkedHashMap<String, String> implements Comparable<CombinedColumnMap>{
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(String key: keySet() ) {
			if(sb.length()>0) sb.append("; ");
			sb.append(get(key));
		}
		return sb.toString();
	}
	
	@Override
	public int compareTo(CombinedColumnMap c) {
		return toString().compareTo(c.toString());
	}
	
	@Override
	public boolean equals(Object o) {
		if(o==null) return false;
		return toString().equals(o.toString());
	}
	
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
}