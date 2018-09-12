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

		//Replace datatypes number(?1,?2) becomes:
		// - tinyint if ?1=1, ?2=
		// - integer if ?1<19 and ?2=0,
		// - integer if ?1>=19, ?2=0,
		script = script.replaceAll("(?i)NUMBER\\(1\\)", "tinyint");
		script = script.replaceAll("(?i)NUMBER\\([2-9](,0)?\\)", "integer");
		script = script.replaceAll("(?i)NUMBER\\(1[0-8](,0)?\\)", "integer");
		script = script.replaceAll("(?i)NUMBER\\(\\d+(,0)?\\)", "bigint");
		script = script.replaceAll("(?i)VARCHAR2\\((.*?)(\\schar)?\\)", "varchar($1)");


		//Replace alter tables
		if(vendor==SQLVendor.HSQL) {
			script = script.replaceAll("(?i)alter table (.*?) add \\((.*?)\\)", "alter table $1 add $2");
			script = script.replaceAll("(?i)alter table (.*?) modify \\((.*?)\\)", "alter table $1 alter column $2");
		} else if(vendor==SQLVendor.MYSQL) {
			script = script.replaceAll("(?i)alter table (.*?) add (.*?)", "alter table $1 add $2");
			script = script.replaceAll("(?i)alter table (.*?) modify \\((.*?)\\)", "alter table $1 modify $2");
			script = script.replace("\\", "\\\\");
		}

		//remove Enable
		script = script.replaceAll("(?i)\\senable", "");


		if(script.contains("||")) throw new RuntimeException("The script should not concatenate with ||, but with concat(arg1, arg2)");
		if(vendor==SQLVendor.MYSQL) {
			//concat gives pb for null value, but not concat_ws (given the '' separator)
			script = script.replaceAll("(?i)concat\\(", "concat_ws\\('',");
		}

		return script;
	}

}
