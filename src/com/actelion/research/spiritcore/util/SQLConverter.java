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

package com.actelion.research.spiritcore.util;

public class SQLConverter {

	public static enum SQLVendor {
		ORACLE,
		MYSQL,
		HSQL
	}
	
	/**
	 * Takes an Oracle Script as an input and converts it to the appropriate vendor syntax
	 * (this is a super limited converter tool)
	 * @param script
	 * @param vendor
	 * @return
	 */
	public static String convertScript(String script, SQLVendor vendor) {
		if(vendor==SQLVendor.ORACLE) return script;
//		System.out.println("SQLConverter.convertScript()<< "+script.replaceAll(";[\n]*", ";\n"));
		//Replace datatypes
		script = script.replaceAll("(?i)NUMBER\\([1-9](,0)?\\)", "integer");
		script = script.replaceAll("(?i)NUMBER\\([12][0-2](,0)?\\)", "integer");
		script = script.replaceAll("(?i)NUMBER\\(\\d[3-9](,0)?\\)", "bigint");
		script = script.replaceAll("(?i)VARCHAR2\\((.*?)(\\schar)?\\)", "varchar($1)");
				
		
		//Replace alter tables
		script = script.replaceAll("(?i)alter table (.*?) add \\((.*?)\\)", "alter table $1 add $2");
		script = script.replaceAll("(?i)alter table (.*?) modify \\((.*?)\\)", "alter table $1 alter column $2");
		
		//remove Enable
		script = script.replaceAll("(?i)\\senable", "");
		
//		System.out.println("SQLConverter.convertScript()>> "+script.replaceAll(";[\n]*", ";\n"));
		

		return script;
	}
	
}
