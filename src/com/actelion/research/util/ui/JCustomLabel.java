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
import java.awt.Font;

import javax.swing.JLabel;


public class JCustomLabel extends JLabel {

	public JCustomLabel(Font f) {
		super();
		setFont(f);
	}
	public JCustomLabel(String s) {
		super(s);
		setFont(FastFont.REGULAR);
	}
	public JCustomLabel(String s, float size) {
		this(s);
		setFont(FastFont.REGULAR.deriveSize((int)size));
	}
	public JCustomLabel(String s, Font f) {
		this(s);
		setFont(f);
	}
	public JCustomLabel(String s, Color foreground) {
		this(s);
		setForeground(foreground);
	}
	public JCustomLabel(String s, Font f, Color foreground) {
		this(s, f);
		setForeground(foreground);
	}
	public JCustomLabel(String s, int style) {
		this(s);
		setFont(FastFont.REGULAR.deriveFont(style).deriveFont(style));
	}
	public JCustomLabel(String s, int style, float size) {
		super(s);
		setFont(FastFont.REGULAR.deriveFont(style).deriveSize((int)size));
	}
	public JCustomLabel(String s, int style, Color foreground) {
		super(s);
		setFont(FastFont.REGULAR.deriveFont(style).deriveSize(12));
		setForeground(foreground);
	}
	
}
