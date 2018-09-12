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

import javax.swing.ImageIcon;

public enum Direction {

	DEFAULT("Default", "default-dir.png"),
	LEFT_RIGHT("Left->Right", "left-right.png"),
	TOP_BOTTOM("Top->Bottom", "top-bottom.png"),
	PATTERN("Keep Pattern", null);

	private String name;
	private ImageIcon img;

	private Direction(String name, String img) {
		this.name = name;
		if(img!=null) {
			try {
				this.img = new ImageIcon(Direction.class.getResource(img));
			} catch(Exception e) {
				this.img = null;
			}
		}

	}

	public String getName() {
		return name;
	}

	public ImageIcon getImage() {
		return img;
	}

	@Override
	public String toString() {
		return getName();
	}

}
