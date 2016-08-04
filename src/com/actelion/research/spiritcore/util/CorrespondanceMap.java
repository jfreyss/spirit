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

import java.util.ArrayList;
import java.util.List;

public class CorrespondanceMap<T1, T2> {
	
	private List<T1> originals = new ArrayList<T1>();
	private List<T2> clones = new ArrayList<T2>();
	
	public void put(T1 original, T2 clone) {
		originals.add(original);
		clones.add(clone);
	}
	
	public T2 get(T1 original) {
		for (int i = 0; i < originals.size(); i++) {
			if(originals.get(i)==original) return clones.get(i);
		}
		return null;
	}

}
