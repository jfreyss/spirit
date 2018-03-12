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

package com.actelion.research.spiritcore.business.location;

import javax.swing.Icon;

import com.actelion.research.util.ui.iconbutton.IconType;

public enum LocationFlag {
	GREEN("Green", IconType.GREEN_FLAG.getIcon()),
	ORANGE("Orange", IconType.ORANGE_FLAG.getIcon()),
	RED("Red", IconType.RED_FLAG.getIcon());

	private final String name;
	private final Icon icon;

	private LocationFlag(String name, Icon icon) {
		this.name = name;
		this.icon = icon;
	}

	public String getName() {
		return name;
	}

	public Icon getIcon() {
		return icon;
	}

	@Override
	public String toString() {
		return name;
	}
}
