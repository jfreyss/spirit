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

package com.actelion.research.spiritapp.spirit.ui.lf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

/**
 * Creates a Biotype ComboBox, displaying the icons and the hierarchy for each type.
 * The values have to be set by the programmer
 * @author freyssj
 *
 */
public class BiotypeComboBox extends JGenericComboBox<Biotype> {

	public BiotypeComboBox() {
		this("");
	}
	
	public BiotypeComboBox(String label) {
		this(null, label);
	}
	public BiotypeComboBox(Collection<Biotype> values) {
		this(values, "");
	}
	public BiotypeComboBox(Collection<Biotype> values, String label) {
		this.setTextWhenEmpty(label);
		setMaximumRowCount(35);
		setValues(values);			
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension dim = super.getPreferredSize();
		dim.width+=20;
		return dim;
	}
	
	@Override
	public Component processCellRenderer(JLabel comp, Biotype type, int index) {
		if(type!=null) {
			int depth = type.getDepth();
			Image img = ImageFactory.getImage(type, FastFont.getAdaptedSize(16));
			comp.setText(type.getName());
			comp.setFont(depth==0? FastFont.BOLD: FastFont.MEDIUM);						
			comp.setIcon(new ImageIcon(img));
			comp.setIconTextGap(1);
			comp.setBorder(BorderFactory.createEmptyBorder(depth==0?2:0, depth*11, 1, 0));
			comp.setOpaque(true);
			comp.setBackground(type.isHidden()? UIUtils.getDilutedColor(Color.LIGHT_GRAY, type.getCategory().getBackground()): type.getCategory().getBackground());	
			comp.setForeground(type.isHidden()? Color.LIGHT_GRAY: Color.BLACK);
		}
		
		return comp;
	}
	
	@Override
	public void setValues(Collection<Biotype> values, String textWhenEmpty) {
		super.setValues(values, textWhenEmpty);
	}
	
	@Override
	public void setSelectionString(String type) {
		for (int i=0; i <= getModel().getSize(); i++) {
			String val = getModel().getElementAt(i)==null?"": getModel().getElementAt(i).toString();
			if(val.equalsIgnoreCase(type)) {
				super.setSelectedIndex(i);
				break;
			}			
		}
	}
	


	public String getSelectionString() {
		return getSelectedItem().toString();
	}

	
}
