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

package com.actelion.research.spiritapp.spirit.ui.study.sampling;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JGenericComboBox;

public class BlocNoComboBox extends JGenericComboBox<Integer> {
		
	public BlocNoComboBox(boolean allowNull) {
		List<Integer> values = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			values.add(i);
		}
		setValues(values, allowNull);
		
		setPreferredWidth(75);
		setSelection(1);
		setRenderer(new DefaultListCellRenderer() {			
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(value!=null && value.toString().length()>0) {
					setFont(FastFont.MEDIUM);
					setText("#"+value);
				}
				return this;
			}
		});				
	}
	
	@Override
	public Component processCellRenderer(JLabel comp, Integer value, int index) {
		comp.setText("#"+value);
		return comp;
	}
	
}
