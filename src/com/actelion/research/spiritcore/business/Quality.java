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

package com.actelion.research.spiritcore.business;


import java.awt.Color;

public enum Quality {
	BOGUS(0, "Bogus", new Color(255, 200, 200)),
	QUESTIONABLE(1, "Questionable", new Color(255, 230, 170)),
	VALID(2, "Valid", null),
	CONFIRMED(3, "Confirmed", new Color(200, 255, 200));

	private final int id;
	private final String name;
	private final Color background;
	
	private Quality(int id, String name, Color background) {
		this.id = id;
		this.name = name;
		this.background = background;
	}
		
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public Color getBackground() {
		return background;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static Quality get(String name) {
		for (Quality q : values()) {
			if(q.name().equals(name)) return q;
		}
		return null;
	}
	
}
