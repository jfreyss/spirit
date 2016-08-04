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

import java.awt.Graphics2D;

import javax.print.PrintService;

import org.krysalis.barcode4j.impl.datamatrix.DataMatrixBean;
import org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider;

public abstract class PrinterUtil {

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void getSupportedAttributes(PrintService p) {
		for(Class c: p.getSupportedAttributeCategories()) {
			try {
				Object val = p.getSupportedAttributeValues(c, null, null);
				if(val.getClass().isArray()) {
					for(Object o: (Object[]) val) {
						System.out.println("  - " + o);
					}
				} else {
					System.out.println("  = " + val);				
				}
			} catch (Exception e) {
				System.err.println(e);
			}
		}		
	}
	
	public static void printBarcode(Graphics2D g, double Xmm, double Ymm, String barcode) {
		double moduleWidth = 1.15;
		g.translate(Xmm*(72/25.4), Ymm*(72/25.4));
		DataMatrixBean bean = new DataMatrixBean();
        bean.setModuleWidth(moduleWidth);
        bean.doQuietZone(false);
        Java2DCanvasProvider canvas = new Java2DCanvasProvider(g, 0);
        bean.generateBarcode(canvas, barcode);
        g.translate(-Xmm*(72/25.4), -Ymm*(72/25.4));

	}
	
	public static int print(Graphics2D g, String s, int x, int y, int width, int height) {
		float current = y;
		for(String line: s.split("\n")) {
			int offset = 0;
			int len = 4;
			float oldy = current;
			while(offset+len<line.length() && current-oldy<=height) {
				String t = line.substring(offset, offset+len);			
				if(g.getFontMetrics().stringWidth(t)<width) {
					len++;
				} else {
					len--;
					g.drawString(line.substring(offset, offset+len), x, current);
					offset+=len;
					len = 4;
					current+=g.getFontMetrics().getHeight()-1.2;
				}
			}
			if(current-oldy<=height) {
				g.drawString(line.substring(offset), x, current);
				current+=g.getFontMetrics().getHeight()-1.2;
			}
		}

		return (int)current;
	}
}