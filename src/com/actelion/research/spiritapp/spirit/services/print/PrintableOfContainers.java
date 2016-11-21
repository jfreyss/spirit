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
import java.util.EnumSet;
import java.util.List;

import org.krysalis.barcode4j.impl.datamatrix.DataMatrixBean;
import org.krysalis.barcode4j.impl.datamatrix.SymbolShapeHint;
import org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.BrotherFormat;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.util.Pair;

public class PrintableOfContainers implements Printable {
	
	public static enum Model {
		FULL,
		HIDEPARENT,
		LINES_BLOCNO,
		LINES_METADATA
	}
	

	private final float MM2PIXELS = 72f/25.4f;
	private static int topMargin = 13;//12;
			
	private List<Container> containers = new ArrayList<Container>(); 
	private String jobName = "";
	private boolean leftHanded = false;
	private Model model = Model.FULL;
	
	public PrintableOfContainers(Collection<Container> containers) {
		this.containers = new ArrayList<Container>(containers);
		this.jobName = "Brother (" + containers.size() + ")";
	}
	
	public void setLeftHanded(boolean leftHanded) {
		this.leftHanded = leftHanded;
	}
	public boolean isLeftHanded() {
		return leftHanded;
	}
	
	public void setModel(Model model) {
		this.model = model;
	}
	public Model getModel() {
		return model;
	}

	@Override
	public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {			
		if (containers==null || pageIndex>=containers.size()) return NO_SUCH_PAGE;

		Container c = containers.get(pageIndex);
		if(c==null) return PAGE_EXISTS;		
		if(c.getBiosamples().size()<=0) throw new PrinterException("Empty container for "+c);
		
		BrotherFormat brotherFormat = c.getContainerType()==null || c.getContainerType().getBrotherFormat()==null? BrotherFormat._12x33N: c.getContainerType().getBrotherFormat();
		
		float lineOffset = brotherFormat.getLineOffset()  * (leftHanded?-1: 1);
		
		String sampleId = c.getContainerId();
		if((sampleId==null || sampleId.length()==0) && c.getFirstBiosample()!=null) sampleId = c.getFirstBiosample().getSampleId();

		//Start drawing
		Graphics2D g = (Graphics2D) graphics;
		g.setClip(null);
		
		boolean wideLabel = pf.getWidth()>75;
		boolean highLabel = pf.getHeight()>50;
		
		
		int fontSizeIncrease = 0;		
		if(highLabel){
			fontSizeIncrease = 1;
		}
		
		int lineOffsetPixel = (int) (lineOffset * MM2PIXELS); //margin where label sticks
		int maxWidth = (int) Math.min((pf.getWidth()<=0? 100: pf.getWidth())-2, (pf.getImageableWidth()<=0? 100: pf.getImageableWidth()))-lineOffsetPixel;
		
		if(lineOffsetPixel>0) {
			g.drawLine(lineOffsetPixel, 0, lineOffsetPixel, (int) pf.getWidth());
			g.translate(lineOffsetPixel, 0);
		} else if(lineOffsetPixel<0) {
			g.drawLine((int) ((pf.getWidth()<=0? 100: pf.getWidth()) + lineOffsetPixel), 0, (int) (pf.getWidth()<=0? 100: pf.getWidth()) + lineOffsetPixel, (int) pf.getHeight());			
		}
		g.translate(3, topMargin+fontSizeIncrease*2); 
		
		int textOffsetPixel;
		int cy = 0;
		if(brotherFormat.isBarcodeOnRight() || sampleId==null || sampleId.length()==0) {
			textOffsetPixel = 0;
			cy+=5+fontSizeIncrease;
		} else {
			textOffsetPixel = Math.max(21, 2 + g.getFontMetrics(new Font("Arial", Font.PLAIN, 3+fontSizeIncrease)).stringWidth(sampleId));
		}
		
		
		//Print study		
		Study study = Biosample.getStudy(c.getBiosamples());
		String string = study==null? null: (wideLabel? study.getStudyIdIvv(): study.getIvvOrStudyId());
		if(string!=null && string.length()>0) {
			cy+=6+fontSizeIncrease;
			g.setFont(new Font("Arial", Font.PLAIN, 6+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxWidth-textOffsetPixel, 2);
			g.drawString(string, textOffsetPixel, cy);	
		}
		
		//Print group
		Group group = Biosample.getGroup(c.getBiosamples());
		if(group!=null) {
			cy+=4+fontSizeIncrease;
			
			string = (group.getStudy().isBlind()?"Gr."+group.getShortName(): group.getName());// + (sub!=null?" '"+(sub+1):"");
			
			g.setFont(new Font("Arial", Font.PLAIN, 5+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxWidth-textOffsetPixel, 2);
			g.drawString(string, textOffsetPixel, cy);	
		}
		
		//Print phaseName
		Phase phase = Biosample.getPhase(c.getBiosamples());
		if(phase!=null) {
			cy+=4+fontSizeIncrease;
			g.setFont(new Font("Arial", Font.PLAIN, 4+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxWidth-textOffsetPixel, 2);
			g.drawString(phase.getAbsoluteDateAndName(), textOffsetPixel, cy);	
		}

		Biosample topBiosample = Biosample.getTopParentInSameStudy(c.getBiosamples());
		if(model!=Model.HIDEPARENT) {
			//Print top SampleId/Name
			if(topBiosample!=null) {
				cy+=4+fontSizeIncrease;
				g.setFont(new Font("Arial", Font.BOLD, 5+fontSizeIncrease));
				string = topBiosample.getSampleIdName();
				reduceSizeIfTooWide(g, string, maxWidth-textOffsetPixel, 2);
				
				int w = g.getFontMetrics().stringWidth(string);
				g.drawString(string, textOffsetPixel, cy);
				Pair<Status, Phase> p = topBiosample.getLastActionStatus();
				if(p.getFirst()!=null && p.getSecond()!=null) {
					string =  " !" + p.getFirst().getName() + "->" + p.getSecond().getShortName() + "!";
					g.setFont(new Font("Arial", Font.PLAIN, 2+fontSizeIncrease));
					g.drawString(string, textOffsetPixel + w + 3, cy);
				}
			}
		}
		
//		cy+=1+fontSizeIncrease;
		System.out.println("PrintableOfContainers.print() cy="+cy);
		if(cy>17 && cy<19) cy = 19;

		//1. decides text to print: blocNo if multiple or biotype
		EnumSet<InfoFormat> nameEnumSet = EnumSet.noneOf(InfoFormat.class);
		if(model==Model.HIDEPARENT || !c.getBiosamples().contains(topBiosample)) nameEnumSet.add(InfoFormat.SAMPLENAME);
		if(model!=Model.HIDEPARENT && !Biosample.getParents(c.getBiosamples()).contains(topBiosample)) nameEnumSet.add(InfoFormat.PARENT_SAMPLENAME);
		
		string = "";
		if(c.getContainerType()==ContainerType.BOTTLE) {
			Integer blocNo = c.getBlocNo();
			if(blocNo!=null && c.getContainerType().getBlocNoPrefix()!=null) string = c.getContainerType().getBlocNoPrefix() + blocNo;			
		} else {		
			string = Biosample.getInfos(c.getBiosamples(), nameEnumSet, InfoSize.ONELINE);
			if(string.length()==0 && c.getContainerType().isMultiple()) {
				//Add blocNo
				Integer blocNo = c.getBlocNo();
				if(blocNo!=null && c.getContainerType().getBlocNoPrefix()!=null) string = c.getContainerType().getBlocNoPrefix() + blocNo;
			}
			if(string.length()==0) string = Biosample.getInfos(c.getBiosamples(), EnumSet.of(InfoFormat.TYPE), InfoSize.ONELINE);
		}
		
		//2. Print text: blocNo if multiple or biotype (BOLD)
		cy+=4+fontSizeIncrease;
		int cx = cy<19? textOffsetPixel: 0;
		int maxCx = (int) ((pf.getWidth()<=0? 100: pf.getWidth()) - Math.abs(lineOffsetPixel) - 2);
		int maxCy = (int) ((pf.getHeight()<=0? 100: pf.getHeight()) - 2);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 5+fontSizeIncrease));
		reduceSizeIfTooWide(g, string, maxCx - cx, 2);
		int cy2 = PrinterUtil.print(g, string, cx, cy, maxCx - cx, 0);
		int wName = g.getFontMetrics().stringWidth(string);

		//3. Print metadata, comments, amount (PLAIN)
		string = Biosample.getInfos(c.getBiosamples(), EnumSet.of(InfoFormat.METATADATA), InfoSize.ONELINE);
		int wMeta = g.getFontMetrics().stringWidth(string);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 4+fontSizeIncrease));
		if(wName + wMeta < (maxCx-cx)) {
			cy = PrinterUtil.print(g, string, cx + (wName+4), cy, maxCx - cx - (wName+4), 0);
		} else {
			cx = 0;
			System.out.println("PrintableOfContainers.print() h="+(maxCy-cy2-5));
			cy = PrinterUtil.print(g, string, cx, cy2, maxCx - cx, Math.max(0, maxCy-cy2-5));
		}

		//2. Print comments amount (ITALIC)
		cy+=1+fontSizeIncrease;
		string = Biosample.getInfos(c.getBiosamples(), EnumSet.of(InfoFormat.COMMENTS, InfoFormat.AMOUNT), InfoSize.ONELINE);
		g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 4+fontSizeIncrease));
		cx = 0;
		cy = PrinterUtil.print(g, string, cx, cy, maxCx - cx, 30);
				
		//Barcode
		if(sampleId!=null && sampleId.length()>0) {
			g.setFont(new Font("Arial", Font.PLAIN, 3+fontSizeIncrease));
			g.drawString(sampleId, 0, 4.5f);
		
	
			//Print Barcode
			if(brotherFormat.isBarcodeOnRight()) {
				System.out.println("BrotherPrintable.print() "+brotherFormat);
				//barcode on the top right
				g.translate(maxWidth - 7*MM2PIXELS, 1*MM2PIXELS);
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, (int)(6*MM2PIXELS), (int)(6*MM2PIXELS));
				g.translate(.5,.5);
				g.setColor(Color.BLACK);
			} else {
				//barcode on the upper left
				g.translate(1*MM2PIXELS, 2.1*MM2PIXELS);
			}
			DataMatrixBean bean = new DataMatrixBean();
	        bean.setModuleWidth(.9);
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
	
	public static void setTopMargin(int topMargin) {
		PrintableOfContainers.topMargin = topMargin;
	}
	public static int getTopMargin() {
		return topMargin;
	}
}