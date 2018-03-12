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

package com.actelion.research.spiritapp.ui.biosample;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.ui.JGenericComboBox;

public class BiosampleComboBox extends JGenericComboBox<Biosample> {
	public BiosampleComboBox() {
		super();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(130, 28);
	}
	
	@Override
	public Dimension getMaximumSize() {
		return new Dimension(130, 28);
	}
	
	
	@Override
	public Component processCellRenderer(JLabel comp, Biosample b, int index) {
		SampleIdLabel lbl = new SampleIdLabel();
//		if(b!=null) {
			lbl.setBiosample(b);
//		} else {
//			lbl.setBiosample(null, " ");		
//		}
		super.processCellRenderer(comp, b, index);
		return lbl;
	}
	
}
