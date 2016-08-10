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

package com.actelion.research.spiritcore.business;

import java.util.Map;
import java.util.TreeMap;

import com.actelion.research.spiritcore.util.MiscUtils;

/**
 * Util class used to serialize/deserialize a map from a comma (or tab) separated list (keys are sorted)
 * @author freyssj
 */
public class IntegerMap extends TreeMap<Integer, String> {
	
	public IntegerMap() {
	}
	
	public IntegerMap(Map<Integer, String> map) {
		putAll(map);
	}
	
	public IntegerMap(String list) {		
		if(list!=null) { 
			putAll(MiscUtils.deserializeIntegerMap(list));
		}
	}
	
	public String getSerializedMap() {
		return MiscUtils.serializeIntegerMap(this);
	}
	
	public String getValues() {
		StringBuilder res = new StringBuilder();
		for (Object key : keySet()) {
			String val = get(key);
			if(val.length()==0) continue;
			if(res.length()>0) res.append("; ");
			res.append(val);
		}
		return res.toString();
	}
	
	
}
