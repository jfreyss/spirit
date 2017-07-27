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

package com.actelion.research.spiritapp.spirit.services.report;

import java.awt.Component;
import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.actelion.research.spiritapp.spirit.ui.study.GroupComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.GroupLabel;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.UIUtils;

/**
 * Excel Report on a study.
 * This abstract class is used to categorized each report and serves as helper class.
 * @author Joel Freyss
 *
 */
public abstract class AbstractReport {

	public static enum ReportCategory {
		STUDY,
		TOP,
		SAMPLES,
		ADMIN;
		public String getName() {
			return name().substring(0, 1) + name().substring(1).toLowerCase();
		}
	}

	public enum Style {
		S_TITLE14,
		S_TITLE14BLUE,
		S_TITLE12,
		S_PLAIN,
		S_TH_CENTER,
		S_TH_LEFT,
		S_TH_RIGHT,
		S_TD_LEFT,
		S_TD_SMALL,
		S_TD_DATE,
		S_TD_RIGHT,
		S_TD_BLUE,
		S_TD_DOUBLE0,
		S_TD_DOUBLE1,
		S_TD_DOUBLE2,
		S_TD_DOUBLE3,
		S_TD_DOUBLE1_BLUE,
		S_TD_DOUBLE2_BLUE,
		S_TD_DOUBLE3_BLUE,
		S_TD_DOUBLE100_RED,
		S_TD_DOUBLE3_RED,
		S_TD_RED,
		S_TD_CENTER,
		S_TD_BOLD_LEFT,
		S_TD_BOLD_CENTER,
		S_TD_BOLD_RIGHT
	}

	private Map<ReportParameter, Object> parameterValues = new HashMap<>();

	private final ReportCategory category;
	private final String name;
	private final String description;
	private final ReportParameter[] parameters;


	protected Study study;
	protected Workbook wb;
	protected Map<Integer, CellStyle> styleWithBordersUnder = new HashMap<>();
	protected Map<Integer, CellStyle> styleWithBordersAbove = new HashMap<>();
	protected Map<Style, CellStyle> styles = new HashMap<>();


	public AbstractReport(ReportCategory category, String name, String description) {
		this(category, name, description, new ReportParameter[0]);
	}

	public AbstractReport(ReportCategory category, String name, String description, ReportParameter[] parameters) {
		this.category = category;
		this.name = name;
		this.description = description;
		this.parameters = parameters;
	}

	public void initFromReport(AbstractReport rep) {
		this.styleWithBordersUnder = rep.styleWithBordersUnder;
		this.styleWithBordersAbove = rep.styleWithBordersAbove;
		this.styles = rep.styles;
		this.wb = rep.wb;
		this.study = rep.study;
	}

	public ReportCategory getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	private void initWorkbook() {

		wb = new XSSFWorkbook();
		styles.clear();
		styleWithBordersAbove.clear();
		styleWithBordersUnder.clear();

		CellStyle style;
		DataFormat df = wb.createDataFormat();

		Font font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setFontHeightInPoints((short)14);
		style = wb.createCellStyle();
		style.setFont(font);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TITLE14, style);

		font = wb.createFont();
		font.setColor(IndexedColors.INDIGO.getIndex());
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setFontHeightInPoints((short)14);
		style = wb.createCellStyle();
		style.setFont(font);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TITLE14BLUE, style);

		font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setFontHeightInPoints((short)12);
		style = wb.createCellStyle();
		style.setFont(font);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TITLE12, style);

		font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setFontHeightInPoints((short)9);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_NONE);
		style.setBorderLeft(CellStyle.BORDER_NONE);
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_PLAIN, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
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
		style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TH_CENTER, style);

		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TH_LEFT, style);

		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TH_RIGHT, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_LEFT, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_RIGHT, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)7);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setFont(font);
		style.setWrapText(true);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_SMALL, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setDataFormat(df.getFormat("0"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DOUBLE0, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setColor(IndexedColors.INDIGO.getIndex());
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_BLUE, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setDataFormat(df.getFormat("0.0"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DOUBLE1, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setColor(IndexedColors.INDIGO.getIndex());
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setDataFormat(df.getFormat("0.0"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DOUBLE1_BLUE, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setColor(IndexedColors.INDIGO.getIndex());
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setDataFormat(df.getFormat("0.00"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DOUBLE2_BLUE, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setDataFormat(df.getFormat("0.00"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DOUBLE2, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setDataFormat(df.getFormat("0.000"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DOUBLE3, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setColor(IndexedColors.MAROON.getIndex());
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setDataFormat(df.getFormat("0%"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DOUBLE100_RED, style);


		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setColor(IndexedColors.MAROON.getIndex());
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setDataFormat(df.getFormat("0.000"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DOUBLE3_RED, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setColor(IndexedColors.INDIGO.getIndex());
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setDataFormat(df.getFormat("0.000"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DOUBLE3_BLUE, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setColor(IndexedColors.MAROON.getIndex());
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_RED, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_CENTER, style);

		font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_BOLD_LEFT, style);

		font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_BOLD_CENTER, style);

		font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		style.setFont(font);
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_BOLD_RIGHT, style);

		font = wb.createFont();
		font.setFontHeightInPoints((short)9);
		style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		style.setFont(font);
		style.setDataFormat(df.getFormat("d.mm.yyyy h:MM"));
		style.setWrapText(false);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		styles.put(Style.S_TD_DATE, style);
	}

	public void populateReport(Study study) throws Exception {
		assert study!=null;

		this.study = study;

		initWorkbook();

		//Create the workbook
		populateWorkBook();

		//Post processing
		//Add Table borders (between different styles of cells)
		for(int i=0; i<wb.getNumberOfSheets(); i++) {
			Sheet sheet = wb.getSheetAt(i);

			for(int r=4; r<=sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if(row==null) continue;

				Row rowUp = sheet.getRow(r-1);
				Row rowDown = sheet.getRow(r+1);
				for(int c=0; c<=row.getLastCellNum(); c++) {
					Cell cell = row.getCell(c);
					Cell cellLeft = c==0? null: row.getCell(c-1);
					boolean borderLeftAbove = cellLeft!=null && cellLeft.getCellStyle().getBorderTop()==1;
					boolean borderLeftUnder = cellLeft!=null && cellLeft.getCellStyle().getBorderBottom()==1;

					if((cell!=null && cell.getCellStyle().getBorderLeft()+cell.getCellStyle().getBorderRight()>0)
							|| (cell==null && c+1<=row.getLastCellNum() && row.getCell(c+1)!=null)) {
						if(borderLeftAbove) drawLineAbove(sheet, r, c, c, (short)1);
						if(borderLeftUnder) drawLineUnder(sheet, r, c, c, (short)1);
					}

					if(cell!=null) {
						Font font = wb.getFontAt(cell.getCellStyle().getFontIndex());
						if(font.getFontHeightInPoints()>=12) continue;

						Cell cellUp = rowUp!=null && c<rowUp.getLastCellNum()? rowUp.getCell(c): null;
						Cell cellDown = rowDown!=null && c<rowDown.getLastCellNum()? rowDown.getCell(c): null;

						if( cellUp==null /*|| (cell.getCellType()!=0 && cellUp.getCellType()!=0 && cellUp.getCellType()!=cell.getCellType())*/ ) {
							//Border above
							drawLineAbove(sheet, r, c, c, (short)1);
						}
						if( cellDown==null /*|| (cell.getCellType()!=0 && cellDown.getCellType()!=0 && cellDown.getCellType()!=cell.getCellType())*/) {
							//Border under
							drawLineUnder(sheet, r, c, c, (short)1);
						}
					}
				}
			}
		}

	}

	protected abstract void populateWorkBook() throws Exception;

	protected Cell set(Sheet sheet, int row, int col, Object text, Style style) {
		return set(sheet, row, col, text, style, 1, 1);
	}
	protected Cell set(Sheet sheet, int row, int col, Object text, Style style, int rowspan, int colspan) {
		Row r = sheet.getRow(row);
		if(r==null) r = sheet.createRow(row);
		Cell c = r.getCell(col);
		if(c==null) c = r.createCell(col);
		c.setCellStyle(styles.get(style));
		if(text==null) {
			if(c.getCellStyle().getDataFormatString().startsWith("0")) {
				c.setCellType(Cell.CELL_TYPE_NUMERIC);
				c.setCellValue("");
			} else {
				c.setCellType(Cell.CELL_TYPE_STRING);
				c.setCellValue("");
			}
		} else if(text instanceof String) {
			try {
				c.setCellType(Cell.CELL_TYPE_NUMERIC);
				c.setCellValue(Integer.parseInt((String)text));
			} catch(Exception e) {
				try {
					c.setCellType(Cell.CELL_TYPE_NUMERIC);
					c.setCellValue(Double.parseDouble((String)text));
				} catch(Exception e2) {
					c.setCellType(Cell.CELL_TYPE_STRING);
					c.setCellValue((String)text);
				}
			}
		} else if(text instanceof Double) {
			c.setCellValue((Double)text);
			c.setCellType(Cell.CELL_TYPE_NUMERIC);
		} else if(text instanceof Integer) {
			c.setCellValue((Integer)text);
			c.setCellType(Cell.CELL_TYPE_NUMERIC);
		} else if(text instanceof Date) {
			c.setCellValue((Date)text);
		}
		if(rowspan>1 || colspan>1) {
			sheet.addMergedRegion(new CellRangeAddress(row, row+rowspan-1, col, col+colspan-1));
			for (int i = 0; i < rowspan; i++) {
				for (int j = 0; j < colspan; j++) {
					if(i>0 || j>0) set(sheet, row+i, col+j, "", style);
				}
			}
		}
		return c;
	}


	protected Cell get(Sheet sheet, int row, int col) {
		Row r = sheet.getRow(row);
		if(r==null) r = sheet.createRow(row);
		Cell c = r.getCell(col);
		return c;
	}

	protected Cell setFormula(Sheet sheet, int row, int col, String text, Style style) {
		Row r = sheet.getRow(row);
		if(r==null) r = sheet.createRow(row);
		Cell c = r.getCell(col);
		if(c==null) c = r.createCell(col);
		c.setCellStyle(styles.get(style));
		c.setCellType(Cell.CELL_TYPE_STRING);
		try {
			c.setCellFormula(text);
		} catch(Exception e) {
			e.printStackTrace();
			c.setCellValue("Err. "+e.getMessage());
		}
		return c;
	}
	protected void setAverage(Sheet sheet, int row, int col, String cellRange, Style style) {
		String formula = "IF(COUNT(" + cellRange + "), AVERAGE(" + cellRange + "), \"\")";
		setFormula(sheet, row, col, formula, style);
	}
	protected void setStd(Sheet sheet, int row, int col, String cellRange, Style style) {
		String formula = "IF(COUNT(" + cellRange + ")>1, STDEV(" + cellRange + "), \"\")";
		setFormula(sheet, row, col, formula, style);
	}


	protected void drawLineUnder(Sheet sheet, int row, int colMin, int colMax, short thickness) {
		Row r = sheet.getRow(row);
		if(r==null) r = sheet.createRow(row);
		for (int col = colMin; col <= colMax; col++) {
			Cell c = r.getCell(col);
			if(c==null) c = r.createCell(col);
			CellStyle style = styleWithBordersUnder.get((c.getCellStyle().getIndex()<<4)+thickness);
			if(style==null) {
				style = sheet.getWorkbook().createCellStyle();
				style.cloneStyleFrom(c.getCellStyle());
				style.setBorderBottom(thickness);
				styleWithBordersUnder.put((c.getCellStyle().getIndex()<<4)+thickness, style);
			}
			c.setCellStyle(style);

		}
	}

	protected void drawLineAbove(Sheet sheet, int row, int colMin, int colMax, short thickness) {
		Row r = sheet.getRow(row);
		if(r==null) r = sheet.createRow(row);
		for (int col = colMin; col <= colMax; col++) {
			Cell c = r.getCell(col);
			if(c==null) c = r.createCell(col);
			CellStyle style = styleWithBordersAbove.get(c.getCellStyle().getIndex()<<4+thickness);
			if(style==null) {
				style = sheet.getWorkbook().createCellStyle();
				style.cloneStyleFrom(c.getCellStyle());
				style.setBorderTop(thickness);
				styleWithBordersAbove.put(c.getCellStyle().getIndex()<<4+thickness, style);
			}
			c.setCellStyle(style);
		}
	}


	protected void createHeadersWithPhase(Sheet sheet, Study study, Phase phase, String subtitle) {
		int line = 0;
		Row row = sheet.createRow(line++);
		row.setHeightInPoints(21f);
		Cell cell = row.createCell(0);
		cell.setCellStyle(styles.get(Style.S_TITLE14));
		cell.setCellValue(study.getStudyId() + (study.getLocalId()!=null? " (" + study.getLocalId() + ")":""));

		row = sheet.createRow(line++);
		row.setHeightInPoints(21f);
		cell = row.createCell(0);
		cell.setCellStyle(styles.get(Style.S_TITLE12));
		cell.setCellValue("Date: " + FormatterUtils.formatDateTime(new Date()));
		if(phase!=null) {
			cell.setCellValue(phase.getShortName() + (phase.getAbsoluteDate()!=null? " - " + FormatterUtils.formatDate(phase.getAbsoluteDate()): ""));
		}
		cell = row.createCell(3);
		cell.setCellStyle(styles.get(Style.S_TITLE12));
		cell.setCellValue(subtitle);

		//		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));
		//		sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));
		//		sheet.addMergedRegion(new CellRangeAddress(1, 1, 3, 10));


	}

	protected void createHeadersWithTitle(Sheet sheet, Study study, String title) {
		sheet.createRow(0).setHeightInPoints(22f);
		set(sheet, 0, 0, study.getStudyId() +  (study.getLocalId()!=null? " / " + study.getLocalId():"") + (title==null?"": ": " + title) , Style.S_TITLE14);
	}

	protected void createHeadersWithTitleSubtitle(Sheet sheet, Study study, String title, String subtitle) {
		sheet.createRow(0).setHeightInPoints(22f);
		sheet.createRow(1).setHeightInPoints(21f);
		sheet.createRow(2).setHeightInPoints(21f);
		set(sheet, 0, 0, study.getStudyId(), Style.S_TITLE14);
		set(sheet, 1, 0, (study.getLocalId()!=null? " (" + study.getLocalId() + ")":""), Style.S_TITLE12);
		set(sheet, 2, 0, FormatterUtils.formatDateTimeShort(new Date()), Style.S_TITLE12);
		set(sheet, 0, 2, title, Style.S_TITLE14);
		set(sheet, 1, 2, subtitle, Style.S_TITLE14BLUE);
	}


	protected static String convertToCell(int line, int col) {
		if(line<0 || col<0) throw new IllegalArgumentException(line+"x"+col+" is an invalid cell");
		if(col>26*20) throw new IllegalArgumentException(line+"x"+col+" is an invalid cell");
		if(col>=26) {
			return ((char)('A'+(col/26-1))) + "" + ((char)('A'+col%26)) + "" + (line+1);
		} else {
			return ((char)('A'+col)) + "" + (line+1);
		}
	}

	protected static String convertLinesToCells(List<Integer> lines, int col) {
		if(lines==null) throw new IllegalArgumentException("Lines cannot be null");
		Collections.sort(lines);

		//Check if lines are in block
		boolean inBlock = lines.size()>1;
		for (int i = 1; inBlock && i < lines.size(); i++) {
			if(lines.get(i)!=lines.get(0)+i) inBlock = false;
		}
		if(inBlock) {
			return convertToCell(lines.get(0), col) + ":" + convertToCell(lines.get(lines.size()-1), col);
		} else {
			StringBuilder sb = new StringBuilder();
			for (Integer line : lines) {
				if(sb.length()>0) sb.append(",");
				sb.append( convertToCell(line, col));
			}
			return sb.toString();
		}

	}


	/**
	 * Export the report to the given file (null will create a tmp file and open it in Excel)
	 * @param reportFile
	 */
	public void export(File reportFile) throws Exception {
		if(wb==null) throw new Exception("You must first generate the report");
		boolean open = false;

		if(reportFile==null) {
			String name = getName();
			if(study!=null) name = study.getStudyId()+"_"+name;
			reportFile = File.createTempFile( name, ".xlsx");
			open = true;
		}

		try(OutputStream out = new BufferedOutputStream(new FileOutputStream(reportFile))) {
			wb.write(out);
		}

		if(open) {
			Desktop.getDesktop().open(reportFile);
		}
	}

	public final void setParameter(ReportParameter parameter, Object value) {
		parameterValues.put(parameter, value);
	}

	public final Object getParameter(ReportParameter parameter) {
		Object res = parameterValues.get(parameter);
		if(res==null) return parameter.getDefaultValue();
		return res;
	}


	public String getName() {
		return name;
	}


	public ReportParameter[] getReportParameters() {
		return parameters;
	}

	/**
	 * To be overriden for custom parametrization
	 * @param study
	 * @return
	 */
	public JPanel getExtraParameterPanel(Study study) {
		return null;
	}

	/**
	 * Creates a new sheet, ensuring that the name is safe and unique
	 * @param workbook
	 * @param sheetName
	 * @return
	 */
	public Sheet createSheet(Workbook workbook, String sheetName) {
		Set<String> names = new HashSet<String>();
		for(int i=0; i<workbook.getNumberOfSheets(); i++) {
			names.add(workbook.getSheetName(i));
		}

		String safe = WorkbookUtil.createSafeSheetName(sheetName);

		String name;
		for(int i=0; ; i++) {
			name = safe + (i==0?"": " ("+i+")");
			if(!names.contains(workbook)) break;
		}
		Sheet sheet = wb.createSheet(name);
		sheet.setAutobreaks(true);
		sheet.setMargin(Sheet.LeftMargin, 1);
		sheet.setMargin(Sheet.RightMargin, 1);
		sheet.setMargin(Sheet.BottomMargin, .5);
		sheet.setMargin(Sheet.TopMargin, .5);

		sheet.setFitToPage(true);
		sheet.getPrintSetup().setLandscape(true);
		sheet.getPrintSetup().setFitWidth((short)1);
		sheet.getPrintSetup().setFitHeight((short)99);

		//		Footer footer = sheet.getFooter();
		//	    footer.setRight( "Page " + HeaderFooter.page() + " of " + HeaderFooter.numPages() );

		return sheet;
	}


	public static JPanel createCompareGroupsPanel(Study study, final Map<Group, Group> populateCompareGroup2Groups) {
		List<Component> comps = new ArrayList<>();
		List<Group> groups = new ArrayList<>(study.getGroups());
		for (int i = 1; i < groups.size(); i++) {
			final Group group = groups.get(i);
			populateCompareGroup2Groups.put(group, groups.get(0));

			final GroupComboBox groupCombobox = new GroupComboBox(groups.subList(0, i));
			groupCombobox.setSelection(populateCompareGroup2Groups.get(group));
			comps.add(UIUtils.createHorizontalBox(new JLabel("Compare "), new GroupLabel(group), new JLabel("to: ")));
			comps.add(groupCombobox);
			groupCombobox.addTextChangeListener(e-> {
				populateCompareGroup2Groups.put(group, groupCombobox.getSelection());
			});
		}
		return UIUtils.createTable(comps.toArray(new Component[0]));
	}

}
