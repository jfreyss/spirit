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

package com.actelion.research.spiritapp.ui.util;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;

import com.actelion.research.util.FormatterUtils;
import com.lowagie.text.Chapter;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

public class PDFUtils {

	public static final int MIN_COL_IN_TABLE = 3;

	//Add a header
	public static void addHeader(PdfWriter writer, String header) {
		class MyFooter extends PdfPageEventHelper {
			com.lowagie.text.Font ffont;
			@Override
			public void onEndPage(PdfWriter writer, Document document) {
				try {
					ffont = new com.lowagie.text.Font(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED), 6f, com.lowagie.text.Font.ITALIC);
				} catch(Exception e) {
					e.printStackTrace();
					return;
				}

				String date = FormatterUtils.formatDateTime(new Date());
				PdfContentByte cb = writer.getDirectContent();
				ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
						new Phrase(header, ffont),
						document.left(),
						document.top() + 5, 0);
				ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
						new Phrase("Page " + writer.getCurrentPageNumber(), ffont),
						(document.right() - document.left()) / 2 + document.leftMargin(),
						document.bottom() - 5, 0);
				ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
						new Phrase(date, ffont),
						document.right(),
						document.bottom() - 5, 0);
			}
		}
		writer.setPageEvent(new MyFooter());
	}

	public static void convertHSSF2Pdf(Workbook wb, String header, File reportFile) throws Exception {
		assert wb!=null;
		assert reportFile!=null;

		//Precompute formula
		FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
		for(int i=0; i<wb.getNumberOfSheets(); i++) {
			Sheet sheet = wb.getSheetAt(i);

			for(Row r : sheet) {
				for (Cell c : r) {
					if (c.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
						try {
							evaluator.evaluateFormulaCell(c);
						} catch (Exception e) {
							System.err.println(e);
						}
					}
				}
			}
		}

		File tmp = File.createTempFile("tmp_", ".xlsx");
		try(OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
			wb.write(out);
		}

		//Find page orientation
		int maxColumnsGlobal = 0;
		for (int sheetNo = 0; sheetNo < wb.getNumberOfSheets(); sheetNo++) {
			Sheet sheet = wb.getSheetAt(sheetNo);
			for(Iterator<Row> rowIterator = sheet.iterator(); rowIterator.hasNext(); ) {
				Row row = rowIterator.next();
				maxColumnsGlobal = Math.max(maxColumnsGlobal, row.getLastCellNum());
			}
		}

		Rectangle pageSize = maxColumnsGlobal<10? PageSize.A4: PageSize.A4.rotate();
		Document pdfDocument = new Document(pageSize, 10f, 10f, 20f, 20f);

		PdfWriter writer = PdfWriter.getInstance(pdfDocument, new FileOutputStream(reportFile));
		addHeader(writer, header);
		pdfDocument.open();
		//we have two columns in the Excel sheet, so we create a PDF table with two columns
		//Note: There are ways to make this dynamic in nature, if you want to.
		//Loop through sheets
		for (int sheetNo = 0; sheetNo < wb.getNumberOfSheets(); sheetNo++) {
			Sheet sheet = wb.getSheetAt(sheetNo);

			//Loop through rows, to find number of columns
			int minColumns = 1000;
			int maxColumns = 0;
			for(Iterator<Row> rowIterator = sheet.iterator(); rowIterator.hasNext(); ) {
				Row row = rowIterator.next();
				if(row.getFirstCellNum()>=0) minColumns = Math.min(minColumns, row.getFirstCellNum());
				if(row.getLastCellNum()>=0) maxColumns = Math.max(maxColumns, row.getLastCellNum());
			}
			if(maxColumns==0) continue;

			//Loop through first rows, to find relative width
			float[] widths = new float[maxColumns];
			int totalWidth = 0;
			for(int c = 0; c < maxColumns; c++ ) {
				int w = sheet.getColumnWidth(c);
				widths[c] = w;
				totalWidth+=w;
			}

			for(int c = 0; c < maxColumns; c++ ) {
				widths[c]/=totalWidth;
			}

			//Create new page and a new chapter with the sheet's name
			if(sheetNo>0) pdfDocument.newPage();
			Chapter pdfSheet = new Chapter(sheet.getSheetName(), sheetNo+1);

			PdfPTable pdfTable = null;
			PdfPCell pdfCell = null;
			boolean inTable = false;

			//Loop through cells, to create the content
			//			boolean leftBorder = true;
			//			boolean[] topBorder = new boolean[maxColumns+1];
			for(int r = 0; r<=sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);

				//Check if we exited a table (empty line)
				if(row==null) {
					if(pdfTable!=null) {
						addTable(pdfDocument, pdfSheet, totalWidth, widths, pdfTable);
						pdfTable = null;
					}
					inTable = false;
					continue;
				}

				//Check if we start a table (>MIN_COL_IN_TABLE columns)
				if(row.getLastCellNum()>=MIN_COL_IN_TABLE) {
					inTable = true;
				}

				if(!inTable) {
					//Process the data outside table, just add the text
					boolean hasData = false;
					Iterator<Cell> cellIterator = row.cellIterator();
					while(cellIterator.hasNext()) {
						Cell cell = cellIterator.next();
						if(cell.getCellType()==Cell.CELL_TYPE_BLANK) continue;
						Chunk chunk = getChunk(wb, cell);
						pdfSheet.add(chunk);
						pdfSheet.add(new Chunk(" "));
						hasData = true;
					}
					if(hasData) pdfSheet.add(Chunk.NEWLINE);

				} else {
					//Process the data in table
					if(pdfTable==null) {
						//Create table
						pdfTable = new PdfPTable(maxColumns);
						pdfTable.setWidths(widths);
						//						topBorder = new boolean[maxColumns+1];
					}

					int cellNumber = minColumns;
					//					leftBorder = false;
					Iterator<Cell> cellIterator = row.cellIterator();
					while(cellIterator.hasNext()) {

						Cell cell = cellIterator.next();

						for(; cellNumber < cell.getColumnIndex(); cellNumber++){
							pdfCell = new PdfPCell();
							pdfCell.setBorder(0);
							pdfTable.addCell(pdfCell);
						}

						Chunk phrase = getChunk(wb, cell);
						pdfCell = new PdfPCell(new Phrase(phrase));
						pdfCell.setFixedHeight(row.getHeightInPoints()-3);
						pdfCell.setNoWrap(!cell.getCellStyle().getWrapText());
						pdfCell.setPaddingLeft(1);
						pdfCell.setHorizontalAlignment(cell.getCellStyle().getAlignment()==CellStyle.ALIGN_CENTER? PdfPCell.ALIGN_CENTER:
							cell.getCellStyle().getAlignment()==CellStyle.ALIGN_RIGHT? PdfPCell.ALIGN_RIGHT: PdfPCell.ALIGN_LEFT);
						pdfCell.setUseBorderPadding(false);
						pdfCell.setUseVariableBorders(false);
						pdfCell.setBorderWidthRight(cell.getCellStyle().getBorderRight()==0?0:.5f);
						pdfCell.setBorderWidthBottom(cell.getCellStyle().getBorderBottom()==0?0: cell.getCellStyle().getBorderBottom()>1? 1:.5f);
						pdfCell.setBorderWidthLeft(cell.getCellStyle().getBorderLeft()==0?0: cell.getCellStyle().getBorderLeft()>1? 1:.5f);
						pdfCell.setBorderWidthTop(cell.getCellStyle().getBorderTop()==0?0: cell.getCellStyle().getBorderTop()>1? 1:.5f);
						String color = cell.getCellStyle().getFillForegroundColorColor()==null? null: ((XSSFColor) cell.getCellStyle().getFillForegroundColorColor()).getARGBHex();
						if(color!=null) pdfCell.setBackgroundColor(new Color(Integer.decode("0x"+color.substring(2))));
						pdfTable.addCell(pdfCell);
						cellNumber++;
					}
					for(; cellNumber < maxColumns; cellNumber++) {
						pdfCell = new PdfPCell();
						pdfCell.setBorder(0);
						pdfTable.addCell(pdfCell);
					}
				}

				//Custom code to add all images on the first sheet (works for reporting)
				if(sheetNo==0 && row.getRowNum()==0) {
					for(PictureData pd: wb.getAllPictures()) {
						try {
							Image pdfImg = Image.getInstance(pd.getData());
							pdfImg.scaleToFit(pageSize.getWidth()*.8f-pageSize.getBorderWidthLeft()-pageSize.getBorderWidthRight(), pageSize.getHeight()*.8f-pageSize.getBorderWidthTop()-pageSize.getBorderWidthBottom());
							pdfSheet.add(pdfImg);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			if(pdfTable!=null) {
				addTable(pdfDocument, pdfSheet, totalWidth, widths, pdfTable);
			}

			pdfDocument.add(pdfSheet);
		}
		pdfDocument.close();


	}

	private static void addTable(Document pdfDocument, Chapter pdfSheet, int totalWidth, float[] widths, PdfPTable pdfTable) throws Exception {
		final int MAX_WIDTH = (int) (pdfDocument.getPageSize().getWidth()/842*65000);
		//		if(totalWidth>MAX_WIDTH) {
		//Split the tables in subcolumns
		for(int offset=0; offset<pdfTable.getNumberOfColumns(); ) {
			if(offset>0) pdfSheet.add(new Chunk(" "));

			//Calculates the indexes that fit the page: [offset; index2]
			int subTotalWidth = 0;
			int index2;
			for(index2 = offset; index2<pdfTable.getNumberOfColumns() && subTotalWidth<MAX_WIDTH; index2++) {
				subTotalWidth += widths[index2]*totalWidth;
			}
			index2 = Math.min(index2, pdfTable.getNumberOfColumns()-1);

			//Calculates the new sub widths
			float[] subWidths = new float[index2-offset+2];
			for (int i = 0; i < subWidths.length-1; i++) {
				subWidths[i] = widths[offset+i]*totalWidth/MAX_WIDTH;
			}
			subWidths[subWidths.length-1] = Math.max(0, (float)(MAX_WIDTH-subTotalWidth)/MAX_WIDTH);
			System.out.println("PDFUtils.addTable() "+Arrays.toString(subWidths));

			//Creates the new sub widths
			PdfPTable subtable = new PdfPTable(subWidths.length);
			subtable.setWidths(subWidths);
			for (int r = 0; r < pdfTable.getRows().size(); r++) {
				for (int c = 0; c < index2-offset+1; c++) {
					PdfPCell cell = pdfTable.getRow(r).getCells()[c+offset];
					subtable.addCell(cell);
				}
				PdfPCell pdfCell = new PdfPCell();
				pdfCell.setBorder(0);
				subtable.addCell(pdfCell);
			}

			subtable.setWidthPercentage(100);
			collapseBorder(subtable);
			pdfSheet.add(subtable);

			//Go to the next index offset
			offset = index2+1;
		}
		//		} else {
		//			//Calculates the new sub widths
		//			float[] subWidths = new float[widths.length+1];
		//			for (int i = 0; i < subWidths.length-1; i++) {
		//				subWidths[i] = widths[0]*totalWidth/MAX_WIDTH;
		//			}
		//			subWidths[subWidths.length-1] = Math.max(0, (float)(MAX_WIDTH-totalWidth)/MAX_WIDTH);
		//			pdfTable.setWidths(subWidths);
		//			pdfTable.setWidthPercentage(100);
		//			collapseBorder(pdfTable);
		//			pdfSheet.add(pdfTable);
		//		}
	}

	private static void collapseBorder(PdfPTable pdfTable) {
		for (int r = 0; r < pdfTable.getRows().size(); r++) {
			for (int c = 0; c < pdfTable.getNumberOfColumns(); c++) {
				PdfPCell cell = pdfTable.getRow(r).getCells()[c];
				if(cell.getBorderWidthTop()>0 && r>0) {
					PdfPCell cell2 = pdfTable.getRow(r-1).getCells()[c];
					if(cell2.getBorderWidthBottom()>0) cell.setBorderWidthTop(0);
				}
				if(cell.getBorderWidthLeft()>0 && c>0) {
					PdfPCell cell2 = pdfTable.getRow(r).getCells()[c-1];
					if(cell2.getBorderWidthRight()>0) cell.setBorderWidthLeft(0);
				}
			}
		}
	}

	private static  Chunk getChunk(Workbook wb, Cell cell) {
		Chunk phrase = null;

		switch(cell.getCellType()==Cell.CELL_TYPE_FORMULA? cell.getCachedFormulaResultType(): cell.getCellType()) {
		case Cell.CELL_TYPE_STRING:
			phrase = new Chunk("" + cell.getStringCellValue());
			break;
		case Cell.CELL_TYPE_NUMERIC:
			String format = cell.getCellStyle().getDataFormatString();
			if(cell.getCellStyle().getDataFormat()>0) {
				try {
					if(format.contains("0")) {
						//Decimal
						DecimalFormat df =  new DecimalFormat(format);
						phrase = new Chunk(df.format(cell.getNumericCellValue()));
					} else if(format.contains("h:")) {
						phrase = new Chunk(FormatterUtils.formatDateTimeShort(cell.getDateCellValue()));
					} else if(format.contains("yy")) {
						phrase = new Chunk(FormatterUtils.formatDate(cell.getDateCellValue()));
					}
				} catch (Exception e) {
					System.err.println(e);
				}
			}
			if(phrase==null) {
				phrase = new Chunk("" + (int) cell.getNumericCellValue());
			}
			break;
		case Cell.CELL_TYPE_BLANK:
			phrase = new Chunk("");
			break;
		default:
			phrase = new Chunk(""+cell.getCellType());
		}
		Font font = wb.getFontAt(cell.getCellStyle().getFontIndex());
		short[] rgb = HSSFColor.getIndexHash().get((int)font.getColor()).getTriplet();

		phrase.setFont(new com.lowagie.text.Font(phrase.getFont().getBaseFont(),
				font.getFontHeightInPoints()-3,
				(font.getBold()? com.lowagie.text.Font.BOLD: com.lowagie.text.Font.NORMAL),
				new Color(rgb[0], rgb[1], rgb[2])
				));
		return phrase;
	}



	//	public static void main(String[] args)  {
	//		try {
	//			SpiritFrame.setUser(DAOSpiritUser.loadUser("freyssj"));
	//			SamplesLocationReport wg = new SamplesLocationReport();
	//			wg.populateReport(DAOStudy.getStudyByStudyId("S-00932"));
	//			wg.exportPDF(null);
	//		} catch(Exception e) {
	//			e.printStackTrace();
	//		} finally {
	//		}
	//		System.exit(1);
	//	}
}
