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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Media;

public class AWSTest {
	public static void main(String[] args) throws Exception{
		PrintService service = null;
		Media mediaObj = null;
		PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
		loop: for (PrintService p : services) {
			if(p.getName().contains("AW-S") ) {
				Media[] mps = (Media[]) p.getSupportedAttributeValues(Media.class, null, null);
				for (Media m : mps) {
					System.out.println("AWSTest.main() "+p+"."+m.toString());
					if(!m.toString().contains("Mag")) continue;
					service = p;
					mediaObj = m;
					break loop;
				}
			}
		}
		if(service==null || mediaObj==null) throw new Exception("Could not find Printer ");
		System.out.println("Print on "+service+"/"+mediaObj);
		
		for(Object cat: service.getSupportedAttributeCategories()) {
			try {
				System.out.println("CAT: "+cat  + " = " + service.getDefaultAttributeValue((Class) cat));
				
				Object values = service.getSupportedAttributeValues((Class) cat, null, null);
				
				if(values instanceof Object[]) {
					for (Object o2 : (Object[]) values) {
						System.out.println("->"+o2);
					}
				} else {
					System.out.println("->"+values);				
				}
			} catch(Exception e) {
				
			}
		}
		
		PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
		attr.add(new Copies(1));
		attr.add(mediaObj);
//		attr.add(OrientationRequested.LANDSCAPE);		
//		attr.add(SheetCollate.UNCOLLATED);
//		attr.add(Chromaticity.MONOCHROME);

		
		Printable printable = new Printable() {
			private final float MM2PIXELS = 72f/25.4f;
			@Override
			public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
				if(pageIndex>0) return NO_SUCH_PAGE;
				//Start drawing
				Graphics2D g = (Graphics2D) graphics;
				g.setClip(null);
				g.setColor(Color.BLACK);
				
//				if(pf.getHeight()>pf.getHeight()){
//				pf.setOrientation(PageFormat.PORTRAIT);
//			}
	//
//				g.fillRect(-200, -200, 200+(int)pf.getWidth()/2, 500);

				
				System.out.println("BrotherPrintable.print() "+pf.getImageableX()+" "+pf.getImageableWidth()+" x "+pf.getImageableY()+" "+pf.getImageableY()+" / "+pf.getWidth()+"x"+pf.getHeight()+" orient="+pf.getOrientation());
				
				return PAGE_EXISTS;
			}
		};
		
		
		Doc myDoc = new SimpleDoc(printable, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);  
		DocPrintJob job = service.createPrintJob();
		System.out.println("AWSTest.main() ");
		for(Attribute obj :job.getAttributes().toArray()) {
			System.out.println("AWSTest.main() "+obj);
		}
//	    job.print(myDoc, attr);
	    
	    
	    
//		PrinterJob job = PrinterJob.getPrinterJob();
//		job.setJobName("TEST - " + printable.toString());
//		job.setPrintable(printable);
//		job.setPrintService(service);
		
//		job.print(attr);
	}
}
