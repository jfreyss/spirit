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

package com.actelion.research.spiritapp.ui.admin;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.spiritapp.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.FormatterUtils;

public class TestDocumentPane extends ImageEditorPane {

	private List<Test> tests = new ArrayList<>();
	private Test selection = null;

	public TestDocumentPane(List<Test> tests) {
		super();
		setEditable(false);
		setTests(tests);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(700, 400);
	}

	public void setSelection(Test t) {
		this.selection = t;
		updateText();
	}

	public void setTests(List<Test> tests) {
		this.tests = tests;
		updateText();
	}

	private void updateText() {
		int caret = getCaretPosition();
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table style='padding:0px;white-space:nowrap'><tr style='padding:4px;background:#CCCCCC'>");
		sb.append("<td><b>Category</b></td>");
		sb.append("<td><b>Name</b></td>");
		sb.append("<td><b>Input</b></td>");
		sb.append("<td><b>Output</b></td>");
		sb.append("<td><b>Infos</b></td>");
		sb.append("<td><b>Creation</b></td>");
		sb.append("<td><b>Update</b></td>");
		Test previous = null;
		for(Test test: tests) {
			sb.append("<tr style='color:" + (test.isHidden()?"#CCCCCC":"black") + ";background:" + (test.equals(selection)?"yellow": "white") + ";" + (previous==null || (previous.getCategory()!=null && !previous.getCategory().equals(test.getCategory()))?"border-top:solid 1px black":"") + "'>");
			previous = test;

			//Category
			sb.append("<td>");
			sb.append(MiscUtils.convert2Html(test.getCategory()));
			sb.append("</td>");
			//Name
			sb.append("<td><a href='test:" + MiscUtils.convert2Html(test.getName()) + "'>");
			sb.append("<b>" + MiscUtils.convert2Html(test.getName()) + "</b>");
			sb.append("</a></td>");

			for(OutputType outputType: OutputType.values()) {
				sb.append("<td style='color:" + color(outputType) + "'>");
				int n = 0;
				for(TestAttribute ta: test.getAttributes(outputType)) {
					if(n>0) sb.append(", ");
					if(n>=3) {sb.append("..."); break;}
					sb.append(MiscUtils.convert2Html(ta.getName()));
					n++;
				}
				sb.append("</td>");
			}

			//Creation
			sb.append("<td>");
			if(test.getCreUser()!=null && test.getCreUser().length()>0) {
				sb.append(FormatterUtils.formatDate(test.getCreDate()) + " [" + test.getCreUser()+"]");
			}
			sb.append("</td>");
			//Update
			sb.append("<td>");
			if(test.getUpdUser()!=null && test.getUpdUser().length()>0) {
				sb.append(FormatterUtils.formatDate(test.getUpdDate()) + " [" + test.getUpdUser()+"]");
			}
			sb.append("</td>");


			sb.append("</tr>");
		}
		setText(sb.toString());
		if(caret>=0 && caret<getText().length()) {
			setCaretPosition(caret);
		}
	}

	private String color(OutputType ta) {
		switch (ta) {
		case INPUT: return "#0000AA";
		case OUTPUT: return "#330000";
		case INFO: return "#330066";
		default: return "";
		}
	}
}

