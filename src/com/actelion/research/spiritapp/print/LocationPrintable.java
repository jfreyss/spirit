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

/**
 * Location are printed on P-Touch printers:
 *
 * The format is the following
 * <pre>
 * 2D 2D |  Name
 * 2D 2D |  Hierarchy
 * </pre>
 *
 * This class is generic to allow the printing of any number of PrintLabel (2D code and multi-line text).
 * The labels are pre-cuts if the printer allows it.
 *
 * @author jfreyss
 *
 */
public class LocationPrintable implements Printable {


	private String jobName = "";

	private List<PrintLabel> labels = new ArrayList<PrintLabel>();

	/**
	 * Creates the Printable Object
	 * @param labels
	 */
	public LocationPrintable(Collection<PrintLabel> labels) {
		this.labels = new ArrayList<PrintLabel>(labels);
		this.jobName = "Locations (" + labels.size() + ")";
	}


	@Override
	public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
		if (labels==null || pageIndex>=labels.size()) return NO_SUCH_PAGE;

		PrintLabel label = labels.get(pageIndex);
		if(label==null) return PAGE_EXISTS;


		//Start drawing
		Graphics2D g = (Graphics2D) graphics;
		//		g.translate(0, 11);

		g.translate(3, 0);

		g.setClip(null);
		g.setColor(Color.BLACK);

		//Split the text by lines
		String[] lines = label.getLabel()==null?new String[0]: label.getLabel().split("\n", -1);

		//Find the longest line
		String maxLine = "";
		for (String line : lines) {
			if(line.length()>maxLine.length()) maxLine = line;
		}

		//Use the biggest font possible (>=5)
		int marginX = label.getBarcodeId()!=null && label.getBarcodeId().length()>0? 25: 2;
		int fontSize = (int) (pf.getHeight()/(lines.length+1));
		while(fontSize>5 && g.getFontMetrics(new Font("Arial", Font.PLAIN, fontSize)).stringWidth(maxLine)>pf.getWidth()-marginX-4) {
			fontSize--;
		}



		//Print the text vertically centered
		float y = (int) (pf.getHeight() -10 )/ (lines.length+1) + 5;
		g.setFont(new Font("Arial", Font.PLAIN, fontSize));
		for (String line : lines) {
			y = PrinterUtil.print(g, line, marginX, y, (int) (pf.getWidth()-marginX-1), (int) (pf.getHeight()-y));
		}


		//Print Barcode on the upper left
		if(label.getBarcodeId()!=null && label.getBarcodeId().length()>0) {
			g.setFont(new Font("Arial", Font.PLAIN, 3));
			g.drawString(label.getBarcodeId(), 4, 6);
			g.translate(4, 8);
			DataMatrixBean bean = new DataMatrixBean();
			bean.setModuleWidth(1);
			bean.doQuietZone(false);
			bean.setShape(SymbolShapeHint.FORCE_SQUARE);
			Java2DCanvasProvider canvas = new Java2DCanvasProvider(g, 0);
			bean.generateBarcode(canvas, label.getBarcodeId());
		}


		return PAGE_EXISTS;
	}

	@Override
	public String toString() {
		return jobName;
	}
}