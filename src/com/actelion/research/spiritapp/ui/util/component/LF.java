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

package com.actelion.research.spiritapp.ui.util.component;

import java.awt.Color;

import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.actelion.research.util.ui.FastFont;

public class LF {

	public static final Color DARK_GREEN = new Color(0, 80, 0);
	public static final Color FGCOLOR_ADMIN = new Color(0, 80, 0);
	public static final Color FGCOLOR_WRITE = new Color(20, 60, 0);
	public static final Color FGCOLOR_READ = new Color(40, 0, 0, 80);
	public static final Color FGCOLOR_VIEW = new Color(255, 50, 50);

	public static final Color COLOR_ERROR_BACKGROUND = new Color(255, 200, 200);
	public static final Color COLOR_ERROR_FOREGROUND = new Color(255, 0, 0);
	public static final Color COLOR_WARNING_FOREGROUND = new Color(200, 100, 0);
	public static final Color COLOR_TEST  = new Color(130, 0, 0);
	public static final Color BGCOLOR_REQUIRED  = new Color(250, 250, 220);

	public static final Color BGCOLOR_LINKED = new Color(255,250,240);
	public static final Color FGCOLOR_LINKED = new Color(80,80,80);

	public static final Color BGCOLOR_TODAY = new Color(255,255,180); //yellowish

	public static final Color BGCOLOR_LOCATION = new Color(245,245,255); //blueish

	public static void initComp(JEditorPane editorPane) {
		StyleSheet stylesheet = ((HTMLEditorKit) editorPane.getEditorKit()).getStyleSheet();
		stylesheet.addRule("body {font-family: " + FastFont.getDefaultFontFamily() + ";font-size:" + FastFont.getDefaultFontSize() + "px}");
		stylesheet.addRule("td, th {margin:0px;padding:0px}");
		stylesheet.addRule("table {font-size:100%;margin:0px;padding:0px}");
	}

}