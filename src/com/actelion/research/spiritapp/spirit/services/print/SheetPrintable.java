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

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.krysalis.barcode4j.impl.datamatrix.DataMatrixBean;
import org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;

public class SheetPrintable implements Printable {
			
	private List<Container> containers = new ArrayList<Container>(); 
	private String jobName = "";
	
	public SheetPrintable(Collection<Container> containers) {
		this.containers = new ArrayList<Container>(containers);
		this.jobName = "A4 Sheet (" + (containers!=null?containers.size():"null") + ")";
	}
	
	@Override
	public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {			
		Graphics2D g = (Graphics2D) graphics;

		int nCols = 7;
		int nRows = 16;
		int offset = pageIndex * nCols * nRows; 
		

		if (containers==null || offset>=containers.size()) return NO_SUCH_PAGE;

		
		System.out.println("Page: " + pageIndex + " Height: "+pf.getHeight()+" Width: "+pf.getWidth());
		System.out.println("ImgHeight: "+pf.getImageableHeight()+" ImgWidth: "+pf.getImageableWidth());
		System.out.println("Paper: "+pf.getPaper());
		System.out.println("Orientation: "+pf.getOrientation());
		
//		g.setClip(2, 2, (int)pf.getWidth()-4, (int)pf.getHeight()-4);
//		g.drawRect(0, 0, (int)pf.getWidth(), (int)pf.getHeight());
//		g.drawRect((int) pf.getImageableX(), (int) pf.getImageableY(), (int)pf.getImageableWidth(), (int)pf.getImageableHeight());

		final double MM2PIXELS = 72/25.4;
		for (int i = offset; i < offset + nCols * nRows && i<containers.size(); i++) {
			Container c = containers.get(i);
			Set<Biosample> set = c.getBiosamples();
			if(set.size()==0) throw new PrinterException("The container "+c+" is empty");
			
			int col = (i-offset)%nCols;
			int row = (i-offset)/nCols;
			double width = 25.4;
			double height = 16.93;
			double x = ((col * (width+2.54) + 8.48)*MM2PIXELS+SheetPrinter.getPaddingX());
			double y = ((row * height + 13.06+3)*MM2PIXELS+SheetPrinter.getPaddingY());
				
			double cy = y;
			g.setClip((int)(x+2), (int)(y+2), (int)(width*MM2PIXELS-4), (int)(height*MM2PIXELS-4));
			

			String containerId = c.getContainerOrBiosampleId();
			Study study = Biosample.getStudy(set);
			String studyId = study==null? null: study.getStudyIdIvv();
			
			Group group = Biosample.getGroup(set);
			Phase phase = Biosample.getPhase(c.getBiosamples());
			


			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 5));
			if(studyId!=null && studyId.length()>0) {
				cy+=6;
				g.drawString(studyId, (int)(x + 7.5*MM2PIXELS), (int)(cy));
			}

			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 5));
			if(group!=null) {
				cy+=5;
				g.drawString(group.getName(), (int)(x + 7.5*MM2PIXELS), (int)(cy));
			}
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 5));
			if(phase!=null) {
				cy+=5;
				g.drawString(phase.getAbsoluteDateAndName(), (int)(x + 7.5*MM2PIXELS), (int)(cy));
			}

			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 5));
			Biosample topParent = Biosample.getTopParentInSameStudy(set);
			if(topParent!=null) {
				cy+=5;
				g.drawString(topParent.getSampleIdName(), (int)(x + 7.5*MM2PIXELS), (int)(cy));
			}

			cy = y + 9.5*MM2PIXELS;
			
				
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 5));
			PrinterUtil.print(g, c.getPrintMetadataLabel(), (int)(x + 1*MM2PIXELS), (int)(cy), (int)(width*MM2PIXELS-4), (int)(5*MM2PIXELS));
				
			//Print BarcodeId
			g.setFont(new Font("Arial", Font.PLAIN, 3));
			g.drawString(containerId, (int)(x + 1*MM2PIXELS), (int)(y + 2*MM2PIXELS));	

			
			//Print Barcode
			g.translate((int)(x + 1*MM2PIXELS), (int)(y + 3*MM2PIXELS));
			if(containerId!=null && containerId.length()>0) {
				DataMatrixBean bean = new DataMatrixBean();
		        bean.setModuleWidth(1);
		        bean.doQuietZone(false);
		        Java2DCanvasProvider canvas = new Java2DCanvasProvider(g, 0);
		        bean.generateBarcode(canvas, containerId);
			}
			g.translate(-(int)(x + 1*MM2PIXELS), -(int)(y + 3*MM2PIXELS));
		}
		
		return PAGE_EXISTS;
	}
	

	
	@Override
	public String toString() {
		return jobName;
	}
}