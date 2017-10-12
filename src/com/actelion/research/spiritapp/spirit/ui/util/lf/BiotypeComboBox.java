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

package com.actelion.research.spiritapp.spirit.ui.util.lf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.icons.ImageFactory;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JObjectComboBox;
import com.actelion.research.util.ui.UIUtils;

/**
 * Creates a Biotype ComboBox, displaying the icons and the hierarchy for each type.
 * The values have to be set by the programmer
 * @author freyssj
 *
 */
public class BiotypeComboBox extends JObjectComboBox<Biotype> {

	private boolean memorization;

	public BiotypeComboBox() {
		this("Biotype");
	}

	public BiotypeComboBox(String label) {
		this(null, label);
	}

	public BiotypeComboBox(Collection<Biotype> values) {
		this(values, "Biotype");
	}

	public BiotypeComboBox(Collection<Biotype> values, String label) {
		this.setTextWhenEmpty(label);
		setValues(values);
	}
	public void setMemorization(boolean v) {
		if(v!=memorization) {
			this.memorization = v;
			addTextChangeListener(e-> {
				Spirit.getConfig().setProperty("biotype", getText());
			});
			if(getValues()!=null && Biotype.getNames(getValues()).contains(Spirit.getConfig().getProperty("biotype", ""))) {
				setText(Spirit.getConfig().getProperty("biotype", ""));
			}
		}
	}

	@Override
	public Component processCellRenderer(JLabel comp, String typeName, int index) {
		Biotype biotype = getMap().get(typeName);
		if(biotype!=null) {
			Image img = ImageFactory.getImage(biotype, FastFont.getAdaptedSize(18));
			comp.setText(typeName);
			comp.setFont(biotype.getDepth()==0? FastFont.BOLD: FastFont.MEDIUM);
			comp.setIcon(new ImageIcon(img));
			comp.setIconTextGap(1);
			comp.setBorder(BorderFactory.createEmptyBorder(biotype.getDepth()==0?2:0, biotype.getDepth()*11, 1, 0));
			comp.setOpaque(true);
			comp.setBackground(biotype.isHidden()? UIUtils.getDilutedColor(Color.LIGHT_GRAY, biotype.getCategory().getBackground()): biotype.getCategory().getBackground());
			comp.setForeground(biotype.isHidden()? Color.LIGHT_GRAY: Color.BLACK);
		} else {
			comp.setIcon(null);
			comp.setText(typeName);
		}
		return comp;
	}

}
