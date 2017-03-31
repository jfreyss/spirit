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

package com.actelion.research.util.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A JTextCombobox on a underlying object.
 * The model is a map of string->object instead of a collection of string
 *
 * The end user must set the values through overriding getValues, or by calling setValues
 * @author Joel Freyss
 *
 */
public class JObjectComboBox<T> extends JTextComboBox {

	private Collection<T> values;
	private Map<String, T> map = null;

	public JObjectComboBox() {
		super();
		setAllowTyping(false);
		setMultipleChoices(false);
	}

	public JObjectComboBox(Collection<T> choices) {
		this();
		setValues(choices);
	}

	public JObjectComboBox(T[] choices) {
		this();
		setValues(Arrays.asList(choices));
	}

	@Override
	public final Collection<String> getChoices() {
		return getMap().keySet();
	}


	/**
	 * To be either overriden by the user
	 * @return
	 */
	public Collection<T> getValues() {
		return values;
	}

	/**
	 * To be called by the user
	 * @param values
	 */
	public void setValues(Collection<T> values) {
		this.values = values;
		map = null;
	}

	public Map<String, T> getMap() {
		if(map==null) {
			map = new LinkedHashMap<>();
			for(T obj: getValues()) {
				map.put(convertObjectToString(obj), obj);
			}
		}
		return map;
	}

	public void setSelection(T eg) {
		setText(eg==null? "": eg.toString());
	}

	public T getSelection() {
		if(getText().length()==0) return null;
		T eg = getMap().get(getText());
		if(eg==null) {
			System.err.println(getText()+" not found in "+map.keySet());
		}
		return eg;
	}

	/**
	 * Unique string representation of the given object. Can be overriden by the user
	 * @param obj
	 * @return
	 */
	public String convertObjectToString(T obj) {
		return obj==null?"": obj.toString();
	}

}
