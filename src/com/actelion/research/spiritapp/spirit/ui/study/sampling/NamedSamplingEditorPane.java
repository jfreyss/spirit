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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JEditorPane;

import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Study;


public class NamedSamplingEditorPane extends JEditorPane {

	public NamedSamplingEditorPane() {
		super("text/html", "");
		setEditable(false);
	}
	
	public void setStudy(Study study) {
		if(study==null) {
			setText("");			
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body style='background:#FFFFFF;font-size:8px;white-space:nowrap'>");
			sb.append("<table>");
			
			List<NamedSampling> all = new ArrayList<>(study.getNamedSamplings());
			
			//Add the sampling info
			for (NamedSampling ns : all) {
				sb.append("<tr><td valign=top style='white-space:nowrap;background:#FFFFFF;margin:1px'>");
				
				if(ns.getName()!=null) sb.append("<b style='font-size:12px; color:#990000'><u>" + ns.getName() + "</u></b><br>");
				
				sb.append(ns.getHtmlBySampling());
				sb.append("</td></td>");
			}
			sb.append("</tr></table>");
			sb.append("</html>");
			setText(sb.toString());
		}
	}
	
	public void setNamedSampling(NamedSampling ns) {
		if(ns==null) {
			setText("");
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body style='background:#FFFFFF;font-size:8px;white-space:nowrap'>");
			if(ns.getName()!=null) sb.append("<b style='white-space:nowrap;font-size:12px; color:#990000'>" + ns.getName() + "</b><br>");
			sb.append(ns.getHtmlByContainer());
			sb.append("</html>");
			setText(sb.toString());
		}
	}
}
