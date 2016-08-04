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

package com.actelion.research.spiritcore.business.pivot;

import com.actelion.research.spiritcore.business.study.Phase;

public class PivotCellKey implements Comparable<PivotCellKey> {
	private Phase phase;
	private String key;
	
	public PivotCellKey(Phase phase, String key) {
		this.phase = phase;
		this.key = key;
	}
	public String getKey() {
		return key;
	}
	@Override
	public String toString() {
		return key;
	}
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof PivotCellKey) && key.equals(((PivotCellKey)obj).key);
	}
	@Override
	public int compareTo(PivotCellKey o) {
		int c = phase==null? (o.phase==null?0:1): phase.compareTo(o.phase); 
		if(c!=0) return c;
		return key.compareToIgnoreCase(o.key); 
		
//		c =CompareUtils.compare(phase, o.phase);
//		if(c!=0) return c;
//		return CompareUtils.compare(key, o.key);
	}
}