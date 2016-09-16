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


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public enum DataType {	
	ALPHA("Alphanumeric", null, true, true),
	NUMBER("Numeric", null, true, true),	
	DATE("Date/Time", null, true, true),
	
	D_FILE("File", null, true, false),

	LIST("Choice: One", "List of options",  true, true),
	MULTI("Choice: Multi*", "List of options",  true, true),
	AUTO("Autocomplete", null, true, true),
	FORMULA("Formula", "Formula", false, true),
	
	BIOSAMPLE("Linked: Biosample", "Biotype", true, true);
	
	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy"); 
	public static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm"); 
	
	private final String name;
	private final String parametersDescription;
	private final boolean compatibleWithBiotype;
	private final boolean compatibleWithResult;
	
	private DataType(String name, String parametersDescription, boolean compatibleWithBiosample, boolean compatibleWithResult) {
		this.name = name;
		this.parametersDescription = parametersDescription;
		this.compatibleWithBiotype = compatibleWithBiosample;
		this.compatibleWithResult = compatibleWithResult;
	}

	
	public String getDescription() {
		return name;
	}
	
	public String getParametersDescription() {
		return parametersDescription;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static List<DataType> valuesForBiotype() {
		List<DataType> res = new ArrayList<>();
		for (DataType dataType : values()) {
			if(!dataType.compatibleWithBiotype) continue; 
			res.add(dataType);
		}
		return res;
	}
	
	public static List<DataType> valuesForResult() {
		List<DataType> res = new ArrayList<>();
		for (DataType dataType : values()) {
			if(!dataType.compatibleWithResult) continue; 
			res.add(dataType);
		}
		return res;
	}

	
}
