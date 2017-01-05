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

package com.actelion.research.spiritapp.spirit.ui.container;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.actelion.research.spiritcore.business.biosample.BarcodeType;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JGenericComboBox;

public class ContainerTypeComboBox extends JGenericComboBox<ContainerType> {
		
	public ContainerTypeComboBox() {
		this(ContainerType.values());		
	}
	
	public ContainerTypeComboBox(ContainerType[] containerTypes) {
		super();
		setMaximumRowCount(35);
		setValues(Arrays.asList(containerTypes), "");
	}
	
	@Override
	public Component processCellRenderer(JLabel comp, ContainerType type, int index) {
		if(type!=null) {
			comp.setIcon(new ImageIcon(type.getImage(FastFont.getDefaultFontSize()*2-4)));
			comp.setIconTextGap(0);
			if(type.getBarcodeType()==BarcodeType.MATRIX) {
				comp.setBackground(new Color(235, 245, 255));
			} else if(type.getBarcodeType()==BarcodeType.NOBARCODE) {
				comp.setBackground(new Color(245, 235, 255));												
			} else if(type.getBarcodeType()==BarcodeType.GENERATE) {
				comp.setBackground(new Color(255, 255, 240));																		
			}			
		}
		return comp;
	}
	
}
