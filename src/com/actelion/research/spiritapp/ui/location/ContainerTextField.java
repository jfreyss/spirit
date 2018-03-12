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

package com.actelion.research.spiritapp.ui.location;

import java.awt.Dimension;

import com.actelion.research.util.ui.JCustomTextField;

public class ContainerTextField extends JCustomTextField {

	/**ContainerType used to suggest name*/
	//	private ContainerType containerType;

	//	private JButton generateButton = new JButton("Gen");
	private Dimension size = new Dimension(160, 27);

	@Override
	public Dimension getPreferredSize() {
		return size;
	}

	@Override
	public Dimension getMinimumSize() {
		return size;
	}

	public ContainerTextField() {
		super(CustomFieldType.ALPHANUMERIC, 15);
		setLayout(null);

	}

}
