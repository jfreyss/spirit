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

package com.actelion.research.spiritapp.spirit.ui.admin;

import java.awt.Dimension;
import java.awt.Image;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JEditorPane;

import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.Formatter;

public class TestDocumentPane extends JEditorPane {
	private static Hashtable<URL, Image> imageCache = new Hashtable<URL, Image>();
	
	public TestDocumentPane() {
		super("text/html", "");		
		getDocument().putProperty("imageCache", imageCache);
		setEditable(false);
	}
	
	
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(700, 400);
	}
	
	public void showAllTests() {
		StringBuilder sb = new StringBuilder();

		sb.append("<html><body>");
		List<Test> list = DAOTest.getTests();
		Test prev = null;
		for(Test test: list) {
			sb.append(getHelp(test));
			
			if(prev!=null && !prev.getCategory().equals(test.getCategory())) sb.append("<hr>");
			prev = test;
		}			
		sb.append("</body></html>");	
		setText(sb.toString());		
		setCaretPosition(0);
	}
	
	public void setTest(Test t) {	
		StringBuilder sb = new StringBuilder();
		if(t!=null) {
			sb.append("<html><body style='margin:0px;padding:0px'>");
			sb.append(getHelp(t));							
			sb.append("</body></html>");		
		} 
		setText(sb.toString());		
		setCaretPosition(0);
	}

	private String color(TestAttribute ta) {
		switch (ta.getOutputType()) {
		case INPUT: return "#0000AA";
		case OUTPUT: return "#330000";
		case INFO: return "#330066";
		default: return "";
		}
	}
	private StringBuilder getHelp(Test test) {
		StringBuilder sb = new StringBuilder();
		sb.append("<span style='font-size:11px'>" + test.getCategory()+".<b>"+test.getName() + "</b></span><br>");
		sb.append("<table style='margin:0px 0px 2px 0px'>");
		for(TestAttribute ta: test.getAttributes()) {
			sb.append("<tr>");
			sb.append("<td>&nbsp;&nbsp;</td>");
			sb.append("<td style='font-size:9px;color:" + color(ta) + "' valign=top>" + (ta.getOutputType().name()) + ": </td>");
			sb.append("<td style='font-size:9px;color:" + color(ta) + "' valign=top><b>" + ta.getName() + "</b>: </td>");
			sb.append("<td style='font-size:9px;color:" + color(ta) + "' valign=top>" + ta.getDataType()+ (ta.getParameters()==null?"": " " + ta.getParameters()) + "</td>");
			sb.append("</tr>");			
		}
		sb.append("</table");		
		sb.append("<br><hr>");
		
		if(test.getCreDate()!=null) {
			sb.append("<i>Created by " + (test.getCreUser()==null?"N/A":test.getCreUser()) + " - "+ Formatter.formatDateTime(test.getCreDate())+"</i><br>");
			if(test.getUpdDate()!=null && test.getUpdDate().after(test.getCreDate())) {
				sb.append("<i>Updated by " + (test.getUpdUser()==null?"N/A":test.getUpdUser()) + " - "+ Formatter.formatDateTime(test.getUpdDate())+"</i><br>");
			}
		}
		return sb;
	}
	
	
	
}

