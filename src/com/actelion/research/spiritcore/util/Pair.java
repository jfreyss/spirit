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

package com.actelion.research.spiritcore.util;

import com.actelion.research.util.CompareUtils;


/**
 * Immutable class corresponding to a pair of objects.
 * It is comparable and has a hashcode and can be used as a key in Map or in Collections
 * @author freyssj
 *
 * @param <FIRST>
 * @param <SECOND>
 */
public class Pair<FIRST, SECOND> implements Comparable<Pair<FIRST, SECOND>>{
	private final FIRST first;
	private final SECOND second;
	
	public Pair(FIRST first, SECOND second) {
		this.first = first;
		this.second = second;
	}

	public FIRST getFirst() {
		return first;
	}
	
	public SECOND getSecond() {
		return second;
	}
	
	@Override
	public int hashCode() {
		return ((first==null?0: first.hashCode()) + (second==null?0: second.hashCode()))%Integer.MAX_VALUE;
	}
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		Pair<FIRST, SECOND> p = (Pair<FIRST, SECOND>) obj;
		
		return CompareUtils.compare(first, p.first)==0 && CompareUtils.compare(second, p.second)==0;
	}
	
	@Override
	public int compareTo(Pair<FIRST, SECOND> o) {
		if(o==null) return -1;
		
		int c = CompareUtils.compare(first, o.first);
		if(c!=0) return c;
		return CompareUtils.compare(second, o.second);
	}
	
	
	@Override
	public String toString() {
		return "<"+first+", "+second+">";
	}
	
}
