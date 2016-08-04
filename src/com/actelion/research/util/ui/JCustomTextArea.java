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

package com.actelion.research.util.ui;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JTextArea;

public class JCustomTextArea extends JTextArea {

	private String textWhenEmpty;
	
	public JCustomTextArea(int rows, int columns) {
		super(rows, columns);
	}
	
	public void setTextWhenEmpty(String textWhenEmpty) {
		this.textWhenEmpty = textWhenEmpty;
	}
	
	public String getTextWhenEmpty() {
		return textWhenEmpty;
	}
	
	private static final Color LABEL_COLOR = new Color(110, 110, 210);
	private static final Color LABEL_COLOR_DISABLED = new Color(160, 160, 210);

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(getText().length()==0 && textWhenEmpty!=null && !hasFocus()) {
			g.setColor(isEnabled()? LABEL_COLOR: LABEL_COLOR_DISABLED);
			g.setFont(FastFont.MONO);
			g.drawString(textWhenEmpty, 5, 17);
		} 
	}
}
