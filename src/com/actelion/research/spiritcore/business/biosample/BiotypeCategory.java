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

package com.actelion.research.spiritcore.business.biosample;

import java.awt.Color;

public enum BiotypeCategory {
	LIVING("Living", new Color(255, 255, 140)),
	SOLID("Solid sample", new Color(255, 255, 220)),
	LIQUID("Liquid sample", new Color(245, 245, 200)),
	PURIFIED("Purified sample", new Color(235, 225, 200)),
	FORMULATION("Formulation", new Color(240, 225, 255)),
	LIBRARY("Library (abstract / used as references)", new Color(240, 240, 240));
	
	private final String name;
	private final Color background;
	
	private BiotypeCategory(String name, Color background) {
		this.name = name;
		this.background = background;
	}
	@Override
	public String toString() {
		return name;
	}
	public String getName() {
		return name;
	}
	
	public Color getBackground() {
		return background;
	}
	
	
	
}
