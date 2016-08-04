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

import java.util.*;

public class Counter<T> {
	
	private Map<T, Integer> counter = new LinkedHashMap<T, Integer>();
	
	public void increaseCounter(T s, int n) {
		Integer c = counter.get(s);
		if(c==null) c = 0;
		counter.put(s, c+n);
	}
	
	public void increaseCounter(T s) {
		increaseCounter(s, 1);
	}
	
	public int getCount(T name) {
		Integer c = counter.get(name);
		return c==null?0:c;
	}
	
	/**
	 * Get keys sorted by order of entries
	 * @return
	 */
	public Set<T> getKeys() {		
		return counter.keySet();
	}
	
	/**
	 * Get keys sorted by decreasing order of frequencies
	 * @return
	 */
	public List<T> getKeySorted() {
		List<T> res = new ArrayList<T>(counter.keySet());
		Collections.sort(res, new Comparator<T>(){
			@Override
			public int compare(T o1, T o2) {
				return getCount(o2) - getCount(o1);
			}
		});
		return res;
	}
}
