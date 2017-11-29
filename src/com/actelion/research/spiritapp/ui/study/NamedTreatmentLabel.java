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

package com.actelion.research.spiritapp.ui.study;

import java.awt.Graphics;
import java.awt.Graphics2D;

import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class NamedTreatmentLabel extends JGenericComboBox<NamedTreatment> {
	
	private NamedTreatment nt;
	
	public NamedTreatmentLabel() {
		super();
	}
	
	public void setNamedTreatment(NamedTreatment namedTreatment) {
		this.nt = namedTreatment;
	}
	public NamedTreatment getNamedTreatment() {
		return nt;
	}

	@Override		
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics; 
		g.setBackground(getBackground());
		g.clearRect(0, 0, getWidth(), getHeight());
		
		if(nt!=null) {
			g.setFont(FastFont.REGULAR);
			g.setColor(UIUtils.getDilutedColor(getForeground(), nt.getColor()));
			g.drawString(nt.getName(), 2, 10);

			g.setColor(getForeground());
			g.setFont(FastFont.SMALLER);
			g.drawString(nt.getCompoundAndUnits(), 2, 19);
		}
	}
	
	
}
