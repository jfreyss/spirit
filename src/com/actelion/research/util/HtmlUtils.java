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

package com.actelion.research.util;


public class HtmlUtils {

	public static String convert2Html(String s) {
		if(s==null) return "";
		
		//Convert special chars
		s = s.replaceAll("&", "&amp;");
		s = s.replaceAll("<", "&lt;");
		s = s.replaceAll(">", "&gt;");
		
		//convert tab to tables;
		StringBuilder sb = new StringBuilder();
		boolean inTable = false;
		String[] lines = s.split("\n"); 
		for(String line: lines ) {
			if(line.indexOf('\t')<0) {
				if(inTable) {
					sb.append("</table>");
					inTable = false;
				}				
				sb.append(line+"<br>");
			} else {
				if(inTable) {
					sb.append("<tr><td>" + line.replaceAll("\t", "</td><td>") + "</td></tr>");
					
				} else {
					inTable = true;
					sb.append("<table>");
					sb.append("<tr><th>" + line.replaceAll("\t", "</th><th>") + "</th></tr>");
					
				}
			}
		}
		if(inTable) sb.append("</table>");
		
		s = sb.toString();
		return s;
	}
}
