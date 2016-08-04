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

package com.actelion.research.util.ui.iconbutton;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import com.actelion.research.util.ui.FastFont;

/**
 * Library of 16x16 icons for buttons
 * @author freyssj
 *
 */
public class JIconButton extends JButton {
	
	public static enum IconType {
		PRINT("Print", "print.png", "Print Label"),
		INSERT_ROW("Insert Row", "insertRow.png", "Insert Row"),
		ADD_ROW("Add Row", "addRow.png", "Add Row"),
		DEL_ROW("Del Row", "deleteRow.png", "Delete Row"),
		TRASH("Trash", "trash.png", "Send to Trash"),
		SEARCH("Search", "search.png"),
		SAVE("Save", "save.png"),
		DATAWARRIOR("DW", "dw.png", "Export to DataWarrior"),
		EXCEL("Excel", "excel.png", "Export to Excel"),
		CSV("CSV", "csv.png", "Export to CSV"),
		NEW("New", "new.png"),
		DUPLICATE("Duplicate", "duplicate.png"),
		OPEN("Open", "open.png"),
		DELETE("Delete", "delete.png"),
		EDIT("Edit", "edit.png"),
		SETUP("Setup", "setup.png"),
		CLEAR("Clear", "clear.png"),
		PIVOT("Pivot", "pivot.png", "Pivot Table"),
		QUALITY("Quality", "quality.png", "Set Quality"),
		CHECK("Check", "check.png", "Check Errors"),
		ADMIN("Admin", "administration.png"),
		ANIMAL("Animal", "rat.png"),
		BIOSAMPLE("Biosample", "dna.png"),
		LOCATION("Location", "location.png"),
		STUDY("Study", "study.png"),
		RESULT("Result", "results.png"),
		REFRESH("Refresh", "refresh.png"),
		UNDO("Undo", "undo.png"),
		REDO("Redo", "redo.png"),
		CENTER("Center", "center.png"),
		HELP("Help", "help.png"),
		EMPTY("Empty", "empty.png"),
		STATUS("Status", "status.png"),
		ERROR("Error", "error.png"),
		RED_FLAG("Red", "redflag.png"),
		GREEN_FLAG("Green", "greenflag.png"),
		ORANGE_FLAG("Orange", "orangeflag.png"),
		HISTORY("History", "history.png", "View History"),
		EXCHANGE("Exchange", "exchange.png"),
		ZOOM_IN("Zoom +", "zoom_in.png"),
		ZOOM_OUT("Zoom -", "zoom_out.png"),
		LINK("Link", "link.png"),
		BALANCE("Balance", "balance.png"),
		MATRIX("Matrix", "matrix.png"),
		ORBIT("Orbit", "orbit.png"),
		RAW("Raw", "raw.png"),
		HIERARCHY("Hierarchy", "hierarchy.png"),
		SCANNER("Scan", "scan96.png"),
		TODO("ToDo", "todo.png"),
		HOME("Home", "home.png"),
		FOOD("Food", "food.png"),
		STATS("Stats", "stats.png"),
		ROTATE_LEFT("Rotate", "rotate_left.png"),
		SANDGLASS("Sandglass", "sandglass.png"),
		BOOKMARKS("Mark", "bookmarks.png"),
		FIT2SIZE("Fit", "fit_to_size.png"),
		NEXT("Next", "next.png"),
		HTML("HTML", "html.png", "F1 for help")
		;
		
		
		private String img;
		private String text;
		private String tooltip;
		private static Map<String, ImageIcon> mapIcons = new HashMap<>();
		
		private IconType(String text, String img) {
			this(text, img, null); 
		}
		private IconType(String text, String img, String tooltip) {
			this.text = text;
			this.img = img;
			this.tooltip = tooltip;
		}
		
		public ImageIcon getIcon() {
			ImageIcon res = mapIcons.get(img); 
			if(res==null) {
				URL url = JIconButton.class.getResource(img);
				if(url!=null) {
					res = new ImageIcon(url);
				} else {
					res = new ImageIcon();
				}
				mapIcons.put(img, res);
			}
			return res;
		}		
	}
	
	public JIconButton() {
	}
	
	public JIconButton(AbstractAction action) {
		super(action);
	}
	
	public JIconButton(IconType iconType, String text) {
		this(iconType);
		setText(text);
	}
	public JIconButton(IconType iconType, String text, String tooltip) {
		this(iconType);
		setText(text);
		setToolTipText(tooltip);
	}
	public JIconButton(Icon icon, String text) {
		setIcon(icon);
		setText(text);
	}
	
	public JIconButton(IconType iconType) {
		URL url = getClass().getResource(iconType.img);
		if(url!=null) {
			setIcon(new ImageIcon(url));
		} else {
			setText(iconType.text);
		}
		if(iconType.tooltip!=null) setToolTipText(iconType.tooltip);
	}
	
	public void highlight() {
		setFont(FastFont.BOLD);
	}
}
