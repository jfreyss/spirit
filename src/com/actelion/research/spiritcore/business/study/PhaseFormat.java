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

package com.actelion.research.spiritcore.business.study;

public enum PhaseFormat {

	DAY_MINUTES("d0, d2, d3"),
	NUMBER("1. Baseline, 2., 3. EOT");
	

	private final String description;
	
	private PhaseFormat(String description) {
		this.description = description;
	} 
	
	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString() {
		return description;
	}
	
	public String getName(int days, int hours, int minutes, String label) {
		switch(this) {
		case DAY_MINUTES:
			return "d" + days
					+ (hours!=0 || minutes!=0? "_" + hours + "h": "")		
					+ (minutes!=0? (minutes<10?"0":"") + minutes: "")
					+ (label!=null && label.length()>0? " " + label: "");
		case NUMBER:
			return days+"." + (label!=null && label.length()>0? " " + label: "");
		default:
			throw new RuntimeException("The format "+this+" is invalid");
		}
		
	}
	
}
