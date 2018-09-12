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

package com.actelion.research.util.ui.iconbutton;

import java.awt.event.ActionListener;

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

	public JIconButton(String text, ActionListener action) {
		this((Icon) null, text);
		addActionListener(action);
	}

	public JIconButton(AbstractAction action) {
		super(action);
	}

	public JIconButton(Icon icon, AbstractAction action) {
		super(action);
		setIcon(icon);
	}

	public JIconButton(IconType iconType) {
		this(iconType, "");
	}

	public JIconButton(IconType iconType, AbstractAction action) {
		super(action);
		ImageIcon icon = iconType==null? null: iconType.getIcon();
		if(icon!=null) setIcon(icon);
	}

	public JIconButton(IconType iconType, String text, String tooltip) {
		this(iconType, text);
		setToolTipText(tooltip);
	}

	public JIconButton(Icon icon, String text) {
		setIcon(icon);
		setText(text);
	}


	public JIconButton(Icon icon, String text, ActionListener l) {
		this(icon, text);
		addActionListener(l);
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

	public JIconButton(IconType iconType, String text, ActionListener l) {
		this(iconType, text);
		addActionListener(l);
	}
}
