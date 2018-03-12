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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SetHashMap<KEY, VALUE> extends LinkedHashMap<KEY, Set<VALUE>> {

	public SetHashMap() {
	}	
	public SetHashMap(SetHashMap<KEY, VALUE> copy) {
		addAll(copy);
	}
	public void add(KEY key, VALUE value) {
		Set<VALUE> l = get(key);
		if(l==null) {
			l = new LinkedHashSet<VALUE>();
			put(key, l);
		}
		l.add(value);
	}

	
	public void delete(KEY key, VALUE value) {
		Set<VALUE> l = get(key);
		if(l==null) {
			return;
		}
		l.remove(value);
	}

	public void addAll(KEY key, Collection<VALUE> values) {
		Set<VALUE> l = get(key);
		if(l==null) {
			l = new LinkedHashSet<VALUE>();
			put(key, l);
		}
		l.addAll(values);
	}
	
	public void addAll(SetHashMap<KEY, VALUE> values) {
		for (KEY key : values.keySet()) {
			addAll(key, values.get(key));
		}
	}
	
	public void addAll(Map<KEY, Collection<VALUE>> values) {
		for (KEY key : values.keySet()) {
			addAll(key, values.get(key));
		}
	}


}
