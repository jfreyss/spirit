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
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JGenericComboBox;

public class NamedSamplingComboBox extends JGenericComboBox<NamedSampling> {

	
	public NamedSamplingComboBox() {
		this(new ArrayList<NamedSampling>());
	}

	public NamedSamplingComboBox(Collection<NamedSampling> values) {		
		this(values, false);
	}
	
	public NamedSamplingComboBox(Collection<NamedSampling> values, boolean allowNull) {		
		super();
		setValues(values, allowNull);		
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(320, 28);
	}
	
	@Override
	public Component processCellRenderer(JLabel comp, NamedSampling ns, int index) {
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
	
	public static void main(String[] args) {
		Study s = DAOStudy.getStudyByStudyId("S-00085");
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		NamedSamplingComboBox cb = new NamedSamplingComboBox();
		cb.setValues(s.getNamedSamplings());
		f.setContentPane(cb);
		f.pack();
		f.setVisible(true);
	}
	
}
