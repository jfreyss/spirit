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

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Util class to import/export CSV files to/from String[][]
 * 
 * @author Joel Freyss
 */
public class CSVUtils {

	public static String flatten(String[][] table) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < table.length; i++) {
			if(i>0) sb.append("\n");			
			for (int j = 0; j < table[i].length; j++) {
				String s = table[i][j];
				if(s==null) s="";
				if(j>0) sb.append("\t");
				boolean quote = s.indexOf('\t')>=0 || s.indexOf('\n')>=0;
				if(quote) {
					sb.append("\"" + s.replace("\"", "'") + "\"");
				} else {
					sb.append(s);
				}
			}
		}
		return sb.toString();
	}	
	
	public static void exportToCsv(String[][] table, File file) throws Exception {	
		System.out.println("Export csv to "+file);
		StringBuilder sb = new StringBuilder();
		for (String[] line : table) {
			for (String item : line) {
				item = item==null?"": item.replaceAll("[\r\n]+", " ");
				if(item.contains(",") || item.contains("\"") || item.contains("\r") || item.contains("\n") || item.contains("\t")) {
					sb.append("\"" + item.replace("\"", "\"\"") + "\"");
				} else {
					sb.append(item);
				}
				sb.append(",");
			}
			sb.append("\r\n");
		}
		
		try(FileWriter writer = new FileWriter(file)) {
			writer.append(sb.toString());			
		}
	}

	public static void exportToCsv(String[][] table) throws Exception {	
		File reportFile = File.createTempFile("xls_", ".csv");
		exportToCsv(table, reportFile);
		Desktop.getDesktop().open(reportFile);
	}
	
	/**
	 * Convert the csv into an array of String[]. The dimension of each row can vary depending of the size of the splits 
	 * Example:
	 * <pre>
	 * File example
	 * 
	 * H1, H2, H3
	 * D1, "D1,Dd", D3
	 * <pre>
	 * will return:
	 * <pre>
	 * [[File Example], [H1, H2, H3], [D1, 'D1,dd', D3]]
	 * </pre>
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static String[][] importCsv(File file) throws Exception {
		if(!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
		return importCsv(new FileReader(file));
	}
	public static String[][] importCsv(Reader r) throws Exception {
		List<String[]> lines = new ArrayList<String[]>();
		try (LineNumberReader reader = new LineNumberReader(r)) {
			String line;
			while((line = reader.readLine())!=null) {
				String[] split = split(line, ",;\t");
				lines.add(split);
			}
		}
		return lines.toArray(new String[lines.size()][]);
	}
	
	
	public static String[] split(String s, String separators) {
		if(s==null) return new String[0];
		StringTokenizer st = new StringTokenizer(s, "\"" + separators, true);
		List<String> res = new ArrayList<String>();
		boolean inQuote = false;
		StringBuilder sb = new StringBuilder();
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			if(token.equals("\"")) {
				if(inQuote) {
					if(sb.toString().trim().length()>0) res.add(sb.toString().trim());
					sb.setLength(0);
					inQuote = false;
				} else {
					inQuote = true;
				}
			} else if(!inQuote && separators.indexOf(token)>=0) {
				res.add(sb.toString().trim());
				sb.setLength(0);    			
			} else {
				sb.append(token);
			}    				
		}
		res.add(sb.toString().trim());
		
		return res.toArray(new String[res.size()]);
	}
}
