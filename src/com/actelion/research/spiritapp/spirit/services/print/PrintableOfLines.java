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

package com.actelion.research.spiritapp.spirit.services.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.krysalis.barcode4j.impl.datamatrix.DataMatrixBean;
import org.krysalis.barcode4j.impl.datamatrix.SymbolShapeHint;
import org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider;

public class PrintableOfLines implements Printable {
	
	

	private final float MM2PIXELS = 72f/25.4f;
			
	private List<String[]> lines = new ArrayList<String[]>(); 
	private String jobName = "";
	
	public PrintableOfLines(Collection<String[]> containers) {
		this.lines = new ArrayList<String[]>(containers);
		this.jobName = "Brother (" + containers.size() + ")";
	}
	
	
	@Override
	public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {			
		if (lines==null || pageIndex>=lines.size()) return NO_SUCH_PAGE;

		String[] c = lines.get(pageIndex);
		if(c==null || c.length<1) return PAGE_EXISTS;		
		
		String sampleId = c[0];

		//Start drawing
		Graphics2D g = (Graphics2D) graphics;
		g.setClip(null);
		
		boolean highLabel = pf.getImageableHeight()>50 || pf.getHeight()>50;
		
		System.out.println("BrotherPrintable.print() "+pf.getImageableX()+" "+pf.getImageableWidth()+" x "+pf.getImageableY()+" "+pf.getImageableHeight()+" / "+pf.getWidth()+"x"+pf.getHeight()+" orient="+pf.getOrientation());
		int fontSizeIncrease = 0;
		if(highLabel){
			fontSizeIncrease = 1;
		}
				
		g.translate(3, 3);
		
		int cy = 5+fontSizeIncrease;
		
		
		//Print study		
		int maxWidth = (int) Math.min((pf.getImageableWidth()<=0?100: pf.getImageableWidth())-2, (pf.getWidth()<=0? 100: pf.getWidth()));
		String string = c.length>1? c[1]:"";
		if(string.length()>0) {
			g.setFont(new Font("Arial", Font.PLAIN, 6+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxWidth, 2);
			cy+=g.getFont().getSize();
			g.drawString(string, 0, cy);	
		}
		
		//TopId
		string = c.length>2? c[2]:"";
		if(string.length()>0 ) {
			g.setFont(new Font("Arial", Font.BOLD, 5+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxWidth, 2);
			cy+=g.getFont().getSize();
			g.drawString(string, 0, cy);
		}
		

		
		//Print group
		string = c.length>3? c[3]:"";
		if(string.length()>0 ) {
			g.setFont(new Font("Arial", Font.PLAIN, 4+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxWidth, 2);
			cy+=1+g.getFont().getSize();
			g.drawString(string, 0, cy);	
		}
		
		//Print phaseName
		string = c.length>4? c[4]:"";
		if(string.length()>0 ) {
			g.setFont(new Font("Arial", Font.PLAIN, 4+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxWidth, 2);
			cy+=g.getFont().getSize();
			g.drawString(string, 0, cy);	
		}

		//Type
		string = c.length>5? c[5]:"";
		g.setFont(new Font("Arial", Font.BOLD, 5+fontSizeIncrease));		
		cy+=1+g.getFont().getSize();
		cy = PrinterUtil.print(g, string, 0, cy, maxWidth, 0);
		
		//metadata/Amount/Comments
		for (int i = 6; i < c.length; i++) {
			cy+=1;
			string = c[i];
			g.setFont(new Font("Arial", Font.PLAIN, 4+fontSizeIncrease));
			cy = PrinterUtil.print(g, string, 0, cy, maxWidth, 30);			
		}

		//Barcode
		if(sampleId!=null && sampleId.length()>0) {
			g.setFont(new Font("Arial", Font.PLAIN, 3+fontSizeIncrease));
			g.drawString(sampleId, 0, 4.5f);
		
	
			//Print Barcode
//			if(brotherFormat.isBarcodeOnRight()) {
//				System.out.println("BrotherPrintable.print() "+brotherFormat);
				//barcode on the top right: use Math.min(pf.getImageableWidth(), pf.getWidth()) to fix SlidePrinter Bug 
				g.translate(maxWidth - 7.5*MM2PIXELS, 1*MM2PIXELS);
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, (int)(6.5*MM2PIXELS), (int)(6.5*MM2PIXELS));
				g.translate(.5,.5);
				g.setColor(Color.BLACK);
//			} else {
//				//barcode on the upper left
//				g.translate(1*MM2PIXELS, 2.1*MM2PIXELS);
//			}
			DataMatrixBean bean = new DataMatrixBean();
	        bean.setModuleWidth(1);
	        bean.doQuietZone(false);
	        bean.setShape(SymbolShapeHint.FORCE_SQUARE);
	        Java2DCanvasProvider canvas = new Java2DCanvasProvider(g, 0);
	        bean.generateBarcode(canvas, sampleId);
		}

		
		return PAGE_EXISTS;
	}
	
	private static void reduceSizeIfTooWide(Graphics2D g, String string, double maxWidth, int maxReduce) { 
		for (int i = 0; i < maxReduce; i++) {
			if(g.getFontMetrics().stringWidth(string) > maxWidth) g.setFont(new Font(g.getFont().getFontName(), g.getFont().getStyle(), g.getFont().getSize()-1));			
		}
	}
	
	@Override
	public String toString() {
		return jobName;
	}
}