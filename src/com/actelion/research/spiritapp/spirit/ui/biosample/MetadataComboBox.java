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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;

import com.actelion.research.spiritapp.spirit.ui.util.icons.ImageFactory;
import com.actelion.research.util.ui.JComboBoxBigPopup;

public class MetadataComboBox extends JComboBoxBigPopup<String> {
	public MetadataComboBox() {
		super(140, new DefaultComboBoxModel<String>());
		setBackground(Color.WHITE);
		
		setRenderer(new DefaultListCellRenderer() {			
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				String current = index<0? (String) value : (String) getModel().getElementAt(index);
				
				String currentMain = current;
				if(currentMain.indexOf('/')>=0) currentMain = currentMain.substring(0, currentMain.indexOf('/'));
				
				if(!isSelected) {
					setBackground(Color.WHITE);
				} else {
					setBackground(Color.LIGHT_GRAY);
				}

				setText("<html><body><div style='white-space:nowrap;font-size:90%" + (currentMain.startsWith("<<")?";color:blue;font-style:italic":"") + ";height:10px;width:150px;padding:0px;margin:0px'>" +  toHTML(current) + "</div></body></html>");
				
				Image img = ImageFactory.getImage(current, 24);
				if(img==null) img = ImageFactory.createImage(24, true);
				setIconTextGap(0);
				setIcon(new ImageIcon(img));
				setBorder(null);
			
				return this;
			}
		});
	}

	@Override
	public DefaultComboBoxModel<String> getModel() {
		return (DefaultComboBoxModel<String>) super.getModel();
	}
	
	private static String toHTML(String s) {
		s = s.replace("<", "&lt;").replace(">", "&gt;");
		return s;
	}
	
}
