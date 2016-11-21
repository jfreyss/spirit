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

package com.actelion.research.spiritcore.util;

import com.actelion.research.util.CompareUtils;

/**
 * Immutable class to represent a triple of Objects (works like Pair)
 * @author Joel Freyss
 *
 * @param <FIRST>
 * @param <SECOND>
 * @param <THIRD>
 */
public class Triple<FIRST, SECOND, THIRD> implements Comparable<Triple<FIRST, SECOND, THIRD>>{
	private final FIRST first;
	private final SECOND second;
	private final THIRD third;
	
	public Triple(FIRST first, SECOND second, THIRD third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public FIRST getFirst() {
		return first;
	}
	
	public SECOND getSecond() {
		return second;
	}
	
	public THIRD getThird() {
		return third;
	}
	
	@Override
	public int hashCode() {
		return ((first==null?0: first.hashCode()) + (second==null?0: second.hashCode()))%Integer.MAX_VALUE;
	}
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		Triple<FIRST, SECOND, THIRD> p = (Triple<FIRST, SECOND, THIRD>) obj;
		if(first==null && p.first!=null) return false;
		if(first!=null && !first.equals(p.first)) return false;
		
		if(second==null && p.second!=null) return false; 
		if(second!=null && !second.equals(p.second)) return false;
		
		if(third==null && p.third!=null) return false; 
		if(third!=null && !third.equals(p.third)) return false;
		
		return true;
	}
	
	@Override
	public int compareTo(Triple<FIRST, SECOND, THIRD> o) {
		if(o==null) return -1;
		int c = CompareUtils.compare(first, o.first);
		if(c!=0) return c;
		c = CompareUtils.compare(second, o.second);
		if(c!=0) return c;
		c = CompareUtils.compare(third, o.third);
		return c;
	}
	
	
	@Override
	public String toString() {
		return "<" + first+", " + second + ", " + third + ">";
	}
	
}
