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

package com.actelion.research.spiritapp.animalcare.ui;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

public class JBigButton extends JButton {
	
	public JBigButton(Icon icon, String text) {
		super(text);
		
		setMinimumSize(new Dimension(100, 32));
		setFont(new Font(Font.DIALOG, Font.BOLD, 12));
		setHorizontalAlignment(SwingConstants.LEFT);
		setIcon(icon);
		
	}
	
	public JBigButton(AbstractAction action) {
		this((Icon)action.getValue(AbstractAction.SMALL_ICON), (String) action.getValue(AbstractAction.NAME));
	}

	

}
