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

package com.actelion.research.spiritapp.spirit.ui.location;

import java.awt.Component;

import javax.swing.JLabel;

import com.actelion.research.spiritcore.business.location.Direction;
import com.actelion.research.util.ui.JGenericComboBox;

public class DirectionCombobox extends JGenericComboBox<Direction> {
	
	/**
	 * Creates a ComboBox for the directions
	 * @param proposeDefault (true if the default choice should be proposed)
	 */
	public DirectionCombobox(boolean proposeDefault) {
		super(proposeDefault? Direction.values(): new Direction[] {Direction.LEFT_RIGHT, Direction.TOP_BOTTOM}, false); 
		setPreferredWidth(120);
	}
	
	@Override
	public Component processCellRenderer(JLabel comp, Direction value, int index) {
		comp.setIcon(value.getImage());
		return comp;
	}
	

}
