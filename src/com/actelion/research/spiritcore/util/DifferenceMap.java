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

import java.util.LinkedHashMap;

/**
 * Utility class used to store diffences between entities
 *
 * @author Joel Freyss
 */
public class DifferenceMap extends LinkedHashMap<String, Pair<String, String>> {


	public void put(String key, String newValue, String oldValue) {
		super.put(key, new Pair<String, String>(newValue==null?"": newValue, oldValue==null?"": oldValue));
	}

	public String flatten() {
		StringBuilder sb = new StringBuilder();
		for (String key : keySet()) {
			Pair<String, String> newOld = get(key);
			if(sb.length()>0) sb.append("\n");
			sb.append((key.equals("ACTION")?"": key + "=" ) + newOld.getFirst() + (newOld.getSecond()!=null && newOld.getSecond().length()>0?" replacing " + newOld.getSecond():""));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return flatten();
	}


}
