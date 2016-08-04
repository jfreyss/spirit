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

public enum AmountUnit {
	VOL_ML("Volume", "ml"),
	VOL_UL("Volume", "ul"),
	WEIGHT_G("Weight", "g"),
	M_CELLS("Million Cells", "MCells");
	
	private final String name;
	private final String unit;
	
	private AmountUnit(String name, String unit) {
		this.name = name;
		this.unit = unit;
	}		
	
	public String getName() {return name;}		
	public String getUnit() {return unit;}				
	public String getNameUnit() {return name + " [" + unit + "]";}
	
	@Override
	public String toString() {
		return getNameUnit();
	}
}