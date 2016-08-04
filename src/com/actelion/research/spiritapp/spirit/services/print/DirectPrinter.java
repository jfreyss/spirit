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
import java.util.ArrayList;
import java.util.List;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.print.PrintingDlg;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;

public class DirectPrinter  {


	/**
	 * Print a Matrix 2D Barcode and 3 lines of text
	 * The media can be 9mm, Spirit, ... 
	 */
	public String print(PrintService p, Media mediaObj, Printable printable) throws Exception {
		
		if(mediaObj==null) throw new Exception("The media cannot be null");
		
		System.out.println("Print on "+p + " / " + mediaObj);
		
		PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
		attr.add(mediaObj);

		Doc myDoc = new SimpleDoc(printable, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);  
		DocPrintJob job = p.createPrintJob();
	    job.print(myDoc, attr);
	    
	    
		return p.getName();
	}	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws Exception{
		PrintService[] services = SpiritPrinter.getPrintServices("AW-S", null);
		PrintService service = null;
		for (PrintService printService : services) {
			System.out.println("Found printer '"+printService.getName()+"'");
			service = printService;
			break;
		}
		if(service==null) throw new Exception("Could not find any Brother Printer ");
		
		for(Class cat: service.getSupportedAttributeCategories()) {
			try {
				System.out.println("CAT: "+cat  + " = " + service.getDefaultAttributeValue(cat));
				
				Object values = service.getSupportedAttributeValues(cat, null, null);
				
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
		
		List<Container> containers = new ArrayList<>();
		containers.add(DAOBiosample.getContainer("SL011312-1-1")); //Micro
		
		
		Spirit.initUI();
		new PrintingDlg(Container.getBiosamples(containers));		
	}
	
}
