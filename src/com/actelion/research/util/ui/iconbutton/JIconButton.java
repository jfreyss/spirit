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

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 * Library of 16x16 icons for buttons
 * @author freyssj
 *
 */
public class JIconButton extends JButton {
	
	public JIconButton() {
	}
	
	public JIconButton(AbstractAction action) {
		super(action);
	}
	
	public JIconButton(IconType iconType) {
		this(iconType, "");
	}
	
	public JIconButton(IconType iconType, String text, String tooltip) {
		this(iconType, text);
		setToolTipText(tooltip);
	}
	
	public JIconButton(Icon icon, String text) {
		setIcon(icon);
		setText(text);
	}
	
	public JIconButton(IconType iconType, String text) {
		ImageIcon icon = iconType==null? null: iconType.getIcon();
		setText(text);
		if(icon!=null) {
			setIcon(icon);
		} else {
			setText(iconType.text);
		}
		if(iconType!=null && iconType.tooltip!=null) setToolTipText(iconType.tooltip);
	}	
}
