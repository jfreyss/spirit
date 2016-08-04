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

import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.List;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.OrientationRequested;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;

public class SheetPrinter extends PrinterUtil {


	static int paddingX = 0; 
	static int paddingY = 0;
	public static int getPaddingX() {
		return paddingX;
	}

	public static void setPaddingX(int paddingX) {
		SheetPrinter.paddingX = paddingX;
	}

	public static int getPaddingY() {
		return paddingY;
	}

	public static void setPaddingY(int paddingY) {
		SheetPrinter.paddingY = paddingY;
	}
	
	/**
	 * Print a Matrix 2D Barcode and 3 lines of text
	 * The media can be 9mm, Spirit, ... 
	 */
	public String print(PrintService p, Media mediaObj, Printable printable, boolean openDialog) throws Exception {
		
		System.out.println("Print on "+p + " / " + mediaObj);
		
//		AbstractPrinter.getSupportedAttributes(p);
		
		PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
		attr.add(new Copies(1));
		if(mediaObj!=null) attr.add(mediaObj);
		attr.add(OrientationRequested.PORTRAIT);
//		attr.add(PageFormat.PORTRAIT);
//		attr.
		
		PrinterJob job = PrinterJob.getPrinterJob();
		job.setJobName("Spirit - " + printable.toString());
		job.setPrintable(printable);
		job.setPrintService(p);
		
//		if(openDialog) {			
//			if(!job.printDialog()) return null;
//		}
		
		job.print();
		return p.getName();
	}	
	
	
	public static void main(String[] args) throws Exception{
		PrintService[] services = SpiritPrinter.getPrintServices(null, null);
		PrintService pt = null;
		for (PrintService p : services) {
			System.out.println("Found printer '"+p.getName()+"'");
			if(p.getName().contains("LJ42")) pt = p;
			
		}
		if(pt==null) throw new Exception("Could not find any Printer");
		BiosampleQuery q = new BiosampleQuery();
		q.setContainerType(ContainerType.BOTTLE);
		List<Biosample> biosamples = DAOBiosample.queryBiosamples(q, null);
		if(biosamples.size()>80) {
			biosamples = biosamples.subList(0, 80);
		}

		SheetPrintable printable = new SheetPrintable(Biosample.getContainers(biosamples));
		new PrintPreviewDlg(printable);
		new SheetPrinter().print(pt, null, printable, true);		
		System.out.println("Printed on "+ pt);
	}
	
}
