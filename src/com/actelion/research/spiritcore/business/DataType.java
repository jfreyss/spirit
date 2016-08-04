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

import com.actelion.research.spiritcore.adapter.DBAdapter;

public enum DataType {	
	ALPHA("Alphanumeric", null, true, true, false),
	NUMBER("Numeric", null, true, true, false),	
	DATE("Date/Time", null, true, true, false),
	
	D_FILE("File", null, true, false, false),

	LIST("Choice: One", "List of options",  true, true, false),
	MULTI("Choice: Multi*", "List of options",  true, true, false),
	AUTO("Autocomplete", null, true, true, false),
	FORMULA("Formula", "Formula", false, true, false),
	
//	@Deprecated
//	DICO("Dictionary: Hugo", "Domain", true, true, true),
//	@Deprecated
//	ELN("Linked: ActNo or ELN", null, false, true, true),
	BIOSAMPLE("Linked: Biosample", "Biotype", true, true, false);
	
	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy"); 
	public static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm"); 
	
	//private final String name;
	private final String name;
	private final String parametersDescription;
	private final boolean compatibleWithBiotype;
	private final boolean compatibleWithResult;
	private final boolean onlyActelion;
	
	private DataType(String name, String parametersDescription, boolean compatibleWithBiosample, boolean compatibleWithResult, boolean onlyActelion) {
		this.name = name;
		this.parametersDescription = parametersDescription;
		this.compatibleWithBiotype = compatibleWithBiosample;
		this.compatibleWithResult = compatibleWithResult;
		this.onlyActelion = onlyActelion;
	}

	
	public String getDescription() {
		return name;
	}
	
	public String getParametersDescription() {
		return parametersDescription;
	}
	
	public boolean isCompatibleWithBiotype() {
		return compatibleWithBiotype;
	}

	public boolean isCompatibleWithResult() {
		return compatibleWithResult;
	}
	
	public boolean isOnlyActelion() {
		return onlyActelion;
	}

	@Override
	public String toString() {
		return name;
	}
	
	public static List<DataType> valuesForBiotype() {
		List<DataType> res = new ArrayList<DataType>();
		for (DataType dataType : values()) {
			if(dataType.isOnlyActelion() && !DBAdapter.getAdapter().isInActelionDomain()) continue;
			if(!dataType.isCompatibleWithBiotype()) continue; 
			res.add(dataType);
		}
		return res;
	}
	
	public static List<DataType> valuesForResult() {
		List<DataType> res = new ArrayList<DataType>();
		for (DataType dataType : values()) {
			if(dataType.isOnlyActelion() && !DBAdapter.getAdapter().isInActelionDomain()) continue;
			if(!dataType.isCompatibleWithResult()) continue; 
			res.add(dataType);
		}
		return res;
	}

	
}
