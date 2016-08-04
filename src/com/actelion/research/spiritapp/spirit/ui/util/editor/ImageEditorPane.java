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

package com.actelion.research.spiritapp.spirit.ui.util.editor;

import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JEditorPane;

public class ImageEditorPane extends JEditorPane {
	
	private final Map<String, Image> imageCache;
	
	public ImageEditorPane() {
		this(new HashMap<String, Image>());
	}
	
	public ImageEditorPane(Map<String, Image> imageCache) {
		super("text/html", "");
		this.imageCache = imageCache;
		setEditorKit(new MyHTMLEditorKit(imageCache));
		setEditable(false);
	}
	
	public ImageEditorPane(Map<String, Image> imageCache, String html) {
		this(imageCache);
		setText(html);
	}
	
	public Map<String, Image> getImageCache() {
		return imageCache;
	}

}
