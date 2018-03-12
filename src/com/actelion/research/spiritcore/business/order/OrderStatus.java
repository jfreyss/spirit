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

package com.actelion.research.spiritcore.business.order;

import java.awt.Color;

public enum OrderStatus {
	PLANNED("Planned", new Color(0,0,0)),
	ACCEPTED("Accepted", new Color(0,200,0)),
	REFUSED("Refused", new Color(200,0,0)),
	DISPATCHED("Dispatched", new Color(100,100,0)),
	CLOSED("Closed", new Color(100,100,100)),
	CANCELED("Canceled", new Color(200,0,0));
	
	private String name;
	private Color color;
	
	private OrderStatus(String name, Color color) {
		this.name = name;
		this.color = color;
	}
	
	public String getName() {
		return name;
	}
	
	public Color getColor() {
		return color;
	}
	
	public String getHtmlColor() {
		return "#" + Integer.toHexString(color.getRGB()).substring(2);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	
}
