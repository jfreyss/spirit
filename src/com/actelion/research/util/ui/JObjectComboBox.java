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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.UIManager;

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


	/**
	 * Should not be called from subclasses. It is expected that this component is always editable for autocompletion.
	 * To allow free text, call setAllowTyping
	 */
	@Override
	public void setEditable(boolean b) {
		super.setEditable(b);
	}


	/**
	 * Example of use
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.put("nimbusSelectionBackground", new Color(173,207,231));
		} catch (Exception e) {
			e.printStackTrace();
		}
		class Test {
			String name;
			public Test(String name) {this.name = name;}
			@Override
			public String toString() {return "T-"+name;}
		}

		List<Test> choices = new ArrayList<>();
		choices.add(new Test("1"));
		choices.add(new Test("2"));
		choices.add(new Test("3"));
		choices.add(new Test("4"));
		choices.add(new Test("5"));
		choices.add(new Test("6"));
		choices.add(new Test("7"));
		choices.add(new Test("8"));
		choices.add(new Test("9"));
		choices.add(new Test("10"));
		choices.add(new Test("11"));
		choices.add(new Test("12"));
		choices.add(new Test("100"));
		choices.add(new Test("AAA"));


		JObjectComboBox<Test> cb = new JObjectComboBox<>(choices);
		JFrame testFrame = new JFrame("Test: JObjectComboBox");
		testFrame.setContentPane(
				UIUtils.createVerticalBox(
						UIUtils.createTitleBox("Simple", UIUtils.createTable(3, cb))));
		testFrame.pack();
		testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		testFrame.setVisible(true);

	}
}
