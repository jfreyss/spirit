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

package com.actelion.research.spiritapp.print;

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

	public PrintableOfLines(Collection<String[]> lines) {
		this.lines = new ArrayList<String[]>(lines);
		this.jobName = "Brother (" + lines.size() + ")";
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

		int fontSizeIncrease = 0;
		if(highLabel){
			fontSizeIncrease = 1;
		}

		g.translate(3, 0);

		float cy = 4+fontSizeIncrease;


		//Print study
		System.out.println("PrintableOfLines.print() "+pf.getImageableWidth());
		int maxWidth = (int) Math.min((pf.getImageableWidth()<=0?100: pf.getImageableWidth()), (pf.getWidth()<=0? 100: pf.getWidth()));
		String string = c.length>1? c[1]:"";
		if(string.length()>0) {
			g.setFont(new Font("Arial", Font.PLAIN, 5+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxWidth, 2);
			cy+=g.getFont().getSize();
			cy = PrinterUtil.print(g, string, 0, cy, maxWidth, 0);
		}

		for (int i = 2; i < c.length; i++) {
			string = c[i];
			if(!hasLetters(string) || string.startsWith("Bl.")) {
				g.setFont(new Font("Arial", Font.BOLD, 4+fontSizeIncrease));
			} else {
				g.setFont(new Font("Arial", Font.PLAIN, 4+fontSizeIncrease));
			}
			reduceSizeIfTooWide(g, string, maxWidth, 2);
			cy = PrinterUtil.print(g, string, 0, cy, maxWidth, 30);
		}

		//Barcode
		if(sampleId!=null && sampleId.length()>0) {
			g.setFont(new Font("Arial", Font.PLAIN, 2+fontSizeIncrease));
			g.drawString(sampleId, 0, 4f);

			g.translate(maxWidth - 6.5*MM2PIXELS, 0.5*MM2PIXELS);
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, (int)(6.5*MM2PIXELS), (int)(6.5*MM2PIXELS));
			g.translate(.5,.5);
			g.setColor(Color.BLACK);

			DataMatrixBean bean = new DataMatrixBean();
			bean.setModuleWidth(1);
			bean.doQuietZone(false);
			bean.setShape(SymbolShapeHint.FORCE_SQUARE);
			Java2DCanvasProvider canvas = new Java2DCanvasProvider(g, 0);
			bean.generateBarcode(canvas, sampleId);
		}


		return PAGE_EXISTS;
	}

	private static boolean hasLetters(String s) {
		for (int i=0; i<s.length(); i++) {
			if(Character.isLetter(s.charAt(i))) return true;
		}
		return false;
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