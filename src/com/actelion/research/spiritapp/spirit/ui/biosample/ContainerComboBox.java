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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;

import com.actelion.research.spiritapp.spirit.ui.container.ContainerLabel;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.util.ui.JGenericComboBox;

public class ContainerComboBox extends JGenericComboBox<Container> {

	private ContainerLabel lbl = new ContainerLabel();

	public ContainerComboBox() {
		super();
		setMinimumSize(new Dimension(200, 20));
	}

	@Override
	public Component processCellRenderer(JLabel comp, Container container, int index) {
		lbl.setBackground(Color.WHITE);
		lbl.setForeground(Color.BLACK);
		lbl.setContainer(container);
		super.processCellRenderer(comp, container, index);
		return lbl;
	}




}
