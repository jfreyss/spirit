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

package com.actelion.research.spiritcore.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

import com.actelion.research.spiritcore.util.Pair;

public class DBProperty {
	private final String key;
	private final String display;
	private final String defaultOption;
	private final Pair<String, String>[] options;
	
	public DBProperty(String key, String display, String defaultValue) {
		this.key = key;
		this.display = display;
		this.options = null;
		this.defaultOption = defaultValue;
	}
	
	/**
	 * 
	 * @param key
	 * @param displayName
	 * @param options Map of key->display
	 */
	public DBProperty(String key, String display, Pair<String, String>[] options) {
		this.key = key;
		this.display = display;
		this.options = options;
		this.defaultOption = options!=null && options.length>0? options[0].getFirst(): null;
	}
	public String getPropertyName() {
		return key;
	}
	public String getDisplayName() {
		return display;
	}
	public Pair<String, String>[] getOptions() {
		return options;
	}
	
	/**
	 * Map of propertyKey to diplayValue
	 * @return
	 */
	public Map<String, String> getOptionMap() {
		Map<String, String> map = new LinkedHashMap<>();
		if(options!=null){
			for (Pair<String, String> pair : options) {
				map.put(pair.getFirst(), pair.getSecond()); 
			}
		}
		return map;
	}
	public String getKeyFromDisplay(String display) {
		if(options!=null){
			for (Pair<String, String> pair : options) {
				if(pair.getSecond().equals(display)) return pair.getFirst();
			}
		}
		return null;
	}
	
	public String getDisplayFromKey(String key) {
		if(options!=null){
			for (Pair<String, String> pair : options) {
				if(pair.getFirst().equals(key)) return pair.getSecond();
			}
		}
		return null;
	}
	
	public String getDefaultOptionKey() {
		return defaultOption;
	}
	
	@Override
	public boolean equals(Object obj) {
		return key.equals(((DBProperty)obj).getPropertyName());
	}
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@Override
	public String toString() {
		return key;
	}
	
}