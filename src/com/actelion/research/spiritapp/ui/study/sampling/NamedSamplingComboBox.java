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

package com.actelion.research.spiritapp.ui.study.sampling;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Collection;

import javax.swing.JLabel;

import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.util.ui.JObjectComboBox;

public class NamedSamplingComboBox extends JObjectComboBox<NamedSampling> {


	public NamedSamplingComboBox() {
		setTextWhenEmpty("NamedSampling...");
	}

	public NamedSamplingComboBox(Collection<NamedSampling> values) {
		this();
		setValues(values);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(320, 28);
	}

	@Override
	public Component processCellRenderer(JLabel comp, String object, int index) {
		NamedSampling ns = getMap().get(object);
		if(ns==null) {
			comp.setText(" ");
			comp.setToolTipText(null);
		} else {
			comp.setText("<html>" + (ns.getStudy()==null?"": "<b>" + ns.getStudy().getStudyId() + "</b> - ") +
					"<span style='" + (ns.getId()>0?"":"font-weight:bold") + "'>"+  ns.getName() + "<span>" +
					"</html>");
			try {
				comp.setToolTipText("<html>" + ns.getHtmlBySampling());
			} catch(Exception e) {
				comp.setToolTipText(null);
			}
		}
		return comp;
	}

	@Override
	public String convertObjectToString(NamedSampling obj) {
		return obj==null?"": obj.toString();
	}
}
