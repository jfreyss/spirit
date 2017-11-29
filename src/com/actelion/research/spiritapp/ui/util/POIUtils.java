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

package com.actelion.research.spiritapp.ui.util;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.MiscUtils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class POIUtils {
	
	public static String[][] convertTable(List<List<String>> rows) {
		int maxCols = 0; 
		for(List<String> r: rows) {
			for (int i = maxCols+1; i < r.size(); i++) {
				if(r.get(i)!=null && r.get(i).length()>0) maxCols = i;
			}
		}
		maxCols++;
		
		String[][] res = new String[rows.size()][maxCols];
		for (int r = 0; r < rows.size(); r++) {
			Arrays.fill(res[r], "");
			for (int c = 0; c<maxCols && c < rows.get(r).size(); c++) {
				if(rows.get(r).get(c)!=null) res[r][c] = rows.get(r).get(c);
			}
			
		}
		return res;
	}

	public static String[][] convertTable(String string)  {
		if(string==null) return null;
		string = string.replaceAll("[ ]+", " ");
		List<List<String>> rows = new ArrayList<List<String>>();
		List<String> row = new ArrayList<String>();
		rows.add(row);
		StringTokenizer st = new StringTokenizer(string+"\n", "\t\"\r\n", true);
		boolean inQuotes = false;
		StringBuilder s = new StringBuilder();
		String previousToken = "";
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			if(inQuotes) {
				if(token.equals("\"")) {
					inQuotes = false;
				} else {
					s.append(token);
				}
			} else {
				if(token.equals("\t")) {
					row.add(s.toString());
					s.setLength(0);
				} else if(token.equals("\n") || token.equals("\r")) {
					if(token.equals("\r") || !previousToken.equals("\r")) {
						row.add(s.toString());
						row = new ArrayList<String>();
						rows.add(row);
					}
					s.setLength(0);
				} else if(token.equals("\"")) {
					inQuotes = true;
				} else {
					s.append(token);
				}
			}
			previousToken = token;
		}	
		
		return convertTable(rows);
	}
	
	@SuppressWarnings("rawtypes")
	public static Class[] getTypes(String[][] table) {
		if(table.length==0) return null;
		Class[] res = new Class[table[0].length];
		for (int col = 0; col < res.length; col++) {
			Class c = Integer.class;
			for (int row = 1; row < table.length; row++) {
				if(table[row][col]==null || table[row][col].length()==0) continue;
				if(isInteger(table[row][col])) {
					//ok
				} else if(isDouble(table[row][col])) {
					if(c==Integer.class) c = Double.class;
				} else {
					c = String.class;
				}
			}
			res[col] = c;
		}
		return res;
	}
		
	public static enum ExportMode {
		HEADERS_TOP,
		HEADERS_TOPLEFT
	}
	
	@SuppressWarnings("rawtypes")
	public static void exportToExcel(String[][] table, ExportMode exportMode) throws IOException {
		Class[] types = getTypes(table);
		Workbook wb =  new XSSFWorkbook();
		Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
		CellStyle style;
		DataFormat df = wb.createDataFormat();
		
        Font font = wb.createFont();
        font.setFontName("Serif");
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setFontHeightInPoints((short)15);
        style = wb.createCellStyle();
        style.setFont(font);
        styles.put("title", style);
        
        font = wb.createFont();
        font.setFontName("Serif");
        font.setFontHeightInPoints((short)10);
        style = wb.createCellStyle();
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFont(font);
		style.setWrapText(true);
		style.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        styles.put("th", style);
		
        font = wb.createFont();
        font.setFontName("Serif");
        font.setFontHeightInPoints((short)9);
        style = wb.createCellStyle();
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setFont(font);
        style.setWrapText(true);
		style.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        styles.put("td", style);

        font = wb.createFont();
        font.setFontName("Serif");
        font.setFontHeightInPoints((short)9);
        style = wb.createCellStyle();
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setFont(font);
        style.setWrapText(true);
		style.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        styles.put("td-border", style);

        font = wb.createFont();
        font.setFontName("Serif");
        font.setFontHeightInPoints((short)9);
        style = wb.createCellStyle();
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(font);
		style.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        styles.put("td-double", style);

        font = wb.createFont();
        font.setFontName("Serif");
        font.setFontHeightInPoints((short)9);
        style = wb.createCellStyle();
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(font);
		style.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        styles.put("td-right", style);

        font = wb.createFont();
        font.setFontName("Serif");
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setFontHeightInPoints((short)9);
        style = wb.createCellStyle();
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(font);
		style.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        styles.put("td-bold", style);
        
        font = wb.createFont();
        font.setFontName("Serif");
        font.setFontHeightInPoints((short)9);
        style = wb.createCellStyle();
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(font);
        style.setDataFormat(df.getFormat("d.mm.yyyy h:MM"));
		style.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        styles.put("td-date", style);

		Sheet sheet = wb.createSheet();
		sheet.setFitToPage(true);

		Cell cell;

		int maxRows = 0;
		for (int r = 0; r < table.length; r++) {
	        Row row = sheet.createRow(r);	        
	        if(r==0) {
	        	row.setRowStyle(styles.get("th"));
	        }
	        
	        int rows = 1;
			for (int c = 0; c < table[r].length; c++) {
				cell = row.createCell(c);
				String s = table[r][c];
				if(s==null) continue;
				rows = Math.max(rows, s.split("\n").length);
				try {
					if(exportMode==ExportMode.HEADERS_TOP && r==0) {
						cell.setCellStyle(styles.get("th"));
						cell.setCellValue(s);
						
					} else if(exportMode==ExportMode.HEADERS_TOPLEFT && (r==0 || c==0)) {
						if(r==0 && c==0) {
							cell.setCellStyle(styles.get("td"));
						} else {
							cell.setCellStyle(styles.get("th"));
						}
						cell.setCellValue(s);
					} else if(types[c]==Double.class) {
						cell.setCellStyle(styles.get("td-double"));
						cell.setCellType(Cell.CELL_TYPE_NUMERIC);
						cell.setCellValue(Double.parseDouble(s));
					} else if(types[c]==String.class) {
						cell.setCellStyle(styles.get(exportMode==ExportMode.HEADERS_TOPLEFT? "td-border": "td"));						
						cell.setCellType(Cell.CELL_TYPE_NUMERIC);
						cell.setCellValue(s);
					} else {
						cell.setCellStyle(styles.get("td-right"));
						cell.setCellValue(s);
					}
				} catch (Exception e) {
					cell.setCellStyle(styles.get("td"));
					cell.setCellValue(s);
				}
			}
			maxRows = Math.max(maxRows, rows);
	        row.setHeightInPoints(rows*16f);
	        
		}
		
		autoSizeColumns(sheet);
		if(table.length>0) {
			for (int c = 0; c < table[0].length; c++) {
				if(sheet.getColumnWidth(c)>10000) sheet.setColumnWidth(c, 3000);
			}
		}
		
		if(exportMode==ExportMode.HEADERS_TOPLEFT) {
			for (int r = 1; r < table.length; r++) {
				sheet.getRow(r).setHeightInPoints(maxRows*16f);
			}
		}
				
		File reportFile = File.createTempFile("xls_", ".xlsx");
		FileOutputStream out = new FileOutputStream(reportFile);
		wb.write(out);
		wb.close();
		out.close();
		Desktop.getDesktop().open(reportFile);
	}

	public static boolean isDouble(String s) {
		if(s==null||s.length()==0) return true;
		try {
			Double.parseDouble(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	public static boolean isInteger(String s) {
		if(s==null||s.length()==0) return true;
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static void autoSizeColumns(Sheet sheet) {
		autoSizeColumns(sheet, 10000, true);
	}
	public static void autoSizeColumns(Sheet sheet, int maxColWidth, boolean resizeHeight) {
		ListHashMap<Integer, Integer> col2lens = new ListHashMap<Integer, Integer>();
		for (int row = sheet.getFirstRowNum(); row < sheet.getLastRowNum(); row++) {
			Row r = sheet.getRow(row);
			if(r==null) continue;
			short maxH = 0;
			
			for (int col = r.getFirstCellNum(); col < r.getLastCellNum(); col++) {
				Cell c = r.getCell(col);
				if(c==null || (c.getCellType()!=Cell.CELL_TYPE_STRING && c.getCellType()!=Cell.CELL_TYPE_NUMERIC)) continue;
				
				Font font = sheet.getWorkbook().getFontAt(c.getCellStyle().getFontIndex());				
				String s = c.getCellType()==Cell.CELL_TYPE_STRING? c.getStringCellValue(): ""+c.getNumericCellValue();
				String[] lines = MiscUtils.split(s, "\n");
				int maxLen = 1;
				for (int i = 0; i < lines.length; i++) {
					maxLen = Math.max(lines[i].length(), maxLen);
				}
				if(font.getFontHeightInPoints()<12) {
					col2lens.add(col, 700 + maxLen*(int)((font.getFontHeightInPoints()+(font.getBoldweight()>500?1:0))*20));
				}
				maxH = (short) Math.max(maxH, 50 + lines.length * (font.getFontHeight()*1.2));
			}
			if(resizeHeight) r.setHeight(maxH);
		}

		for (int col: col2lens.keySet()) {
			List<Integer> lens = col2lens.get(col);
			Collections.sort(lens);
			int len = lens.get(lens.size()-1);
			if(lens.size()>10 && lens.get(lens.size()-1) > 2*lens.get(lens.size()-2)) {
				len = lens.get(lens.size()-2);
			}
			sheet.setColumnWidth(col, Math.max(Math.min((int)(len*1.05), maxColWidth>0? maxColWidth: 300000), 1000));
		}
		
	}
	
}
