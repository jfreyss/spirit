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

package com.actelion.research.spiritapp.spirit.ui.pivot;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.util.ui.FastFont;

public class PivotTemplateButton extends JToggleButton {

	private final String title;
	private final PivotTemplate template;

	public PivotTemplateButton(PivotTemplate template) {
		this.template = template;
		this.title = template==null?"": template.getName();
		if(template!=null) {
			Image img;
			try {
				img = template.getThumbnail();
			} catch (Exception e) {
				img = null;
			}
			
			init(template.getName(), img);
		}
	}
	public PivotTemplateButton(String title, Image img) {
		this.title = title;
		this.template = null;
		init(title, img);
	}
		
	
	private void init(String title, Image img) {
		if(img!=null) {
			int size = FastFont.getAdaptedSize(16);
			setIcon(new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH)));
		}
		setText(title);
	}
		
	/**
	 * @return the view
	 */
	public PivotTemplate getPivotTemplate() {
		return template;
	}

	public String getTitle() {
		return title;
	}
	
	@Override
	public String toString() {
		return title;
	}
		
}
