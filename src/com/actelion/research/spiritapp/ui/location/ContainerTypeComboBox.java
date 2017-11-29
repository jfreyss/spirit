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

package com.actelion.research.spiritapp.ui.location;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JObjectComboBox;

public class ContainerTypeComboBox extends JObjectComboBox<ContainerType> {

	public ContainerTypeComboBox() {
		this(ContainerType.values());
	}

	public ContainerTypeComboBox(ContainerType[] containerTypes) {
		super();
		setTextWhenEmpty("ContainerType");
		setValues(Arrays.asList(containerTypes));
	}

	@Override
	public Component processCellRenderer(JLabel comp, String object, int index) {
		ContainerType type = getMap().get(object);
		if(type!=null) {
			comp.setIcon(new ImageIcon(type.getImage(FastFont.getAdaptedSize(18))));
			comp.setIconTextGap(0);
			if(type.isMultiple()) {
				comp.setBackground(new Color(245, 235, 255));
			} else {
				comp.setBackground(new Color(255, 255, 240));
			}
		} else {
			comp.setIcon(null);
			comp.setText(" ");
		}
		return comp;
	}

	@Override
	public String convertObjectToString(ContainerType obj) {
		return obj==null?"": obj.getName();
	}

}
