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

import java.awt.print.Printable;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;

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
}
