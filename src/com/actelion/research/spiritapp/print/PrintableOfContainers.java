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
import java.util.EnumSet;
import java.util.List;

import org.krysalis.barcode4j.impl.datamatrix.DataMatrixBean;
import org.krysalis.barcode4j.impl.datamatrix.SymbolShapeHint;
import org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;

public class PrintableOfContainers implements Printable {

	private final float MM2PIXELS = 72f/25.4f;

	private List<Container> containers = new ArrayList<>();
	private String jobName = "";
	private PrintTemplate printTemplate = new PrintTemplate();

	public PrintableOfContainers(Collection<Container> containers) {
		this.containers = new ArrayList<Container>(containers);
		this.jobName = "Brother (" + containers.size() + ")";
	}

	public void setPrintTemplate(PrintTemplate printTemplate) {
		this.printTemplate = printTemplate;
	}

	public PrintTemplate getPrintTemplate() {
		return printTemplate;
	}

	@Override
	public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
		if (containers==null || pageIndex>=containers.size()) return NO_SUCH_PAGE;

		Container c = containers.get(pageIndex);
		if(c==null) return PAGE_EXISTS;
		if(c.getBiosamples().size()<=0) throw new PrinterException("Empty container for "+c);
		String barcode = c.getContainerType().isMultiple() || c.getFirstBiosample()==null? c.getContainerId(): c.getFirstBiosample().getSampleId();
		if(barcode==null) barcode = "";
		//Start drawing
		Graphics2D g = (Graphics2D) graphics;
		g.setClip(null);

		boolean highLabel = pf.getHeight()>50;
		int fontSizeIncrease = highLabel?1: 0;

		//Draw Overlap
		float lineOffset = (pf.getWidth()<130? 8: 10)  * printTemplate.getOverlapPosition();
		int lineOffsetPixel = (int) (lineOffset * MM2PIXELS); //margin where label sticks
		int maxWidth = (int) Math.min((pf.getWidth()<=0? 100: pf.getWidth())-2, (pf.getImageableWidth()<=0? 100: pf.getImageableWidth())) - Math.abs(lineOffsetPixel);
		int maxWidthTop = maxWidth - 21;
		if(lineOffsetPixel>0) {
			g.drawLine(lineOffsetPixel, 0, lineOffsetPixel, (int) pf.getWidth());
			g.translate(lineOffsetPixel, 0);
		} else if(lineOffsetPixel<0) {
			g.drawLine((int) ((pf.getWidth()<=0? 100: pf.getWidth()) + lineOffsetPixel), 0, (int) (pf.getWidth()<=0? 100: pf.getWidth()) + lineOffsetPixel, (int) pf.getHeight());
		}
		g.translate(2, 0);

		float cy = 0;
		int textOffsetPixel = printTemplate.getBarcodePosition()==1? Math.max(21, 2 + g.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 3+fontSizeIncrease)).stringWidth(barcode)): 0;

		//Print study
		Study study = Biosample.getStudy(c.getBiosamples());
		String string = study==null? null: printTemplate.isShowInternalIdFirst()? study.getLocalIdOrStudyId(): study.getStudyIdAndInternalId();
		if(string!=null && string.length()>0) {
			cy+=6+fontSizeIncrease;
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 6+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxWidthTop, 2);
			g.drawString(string, textOffsetPixel, cy);
		}

		//Print group
		if(study!=null && !study.isBlind()) {
			Group group = Biosample.getGroup(c.getBiosamples());
			if(group!=null) {
				cy+=4+fontSizeIncrease;
				string = group.getName();
				g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 4+fontSizeIncrease));
				g.drawString(string, textOffsetPixel, cy);
			}
		}

		//Print phaseName
		Phase phase = Biosample.getPhase(c.getBiosamples());
		if(phase!=null) {
			cy+=4+fontSizeIncrease;
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 4+fontSizeIncrease));
			g.drawString(phase.getAbsoluteDateAndName(), textOffsetPixel, cy);
		}

		Biosample topBiosample = Biosample.getTopParentInSameStudy(c.getBiosamples());
		if(printTemplate.isShowParent() && topBiosample!=null) {
			if(study!=null) {
				//Sample in study: print topSampleId/topSampleName/Status
				cy+=5+fontSizeIncrease;
				g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 5+fontSizeIncrease));
				string = topBiosample.getSampleIdName();
				reduceSizeIfTooWide(g, string, maxWidthTop, 2);
				g.drawString(string, textOffsetPixel, cy);
			} else {
				//Sample not is study but has a parent: print topSampleId/topSampleName
				if(!topBiosample.getBiotype().isHideSampleId()) {
					cy+=5+fontSizeIncrease;
					g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 5+fontSizeIncrease));
					string = topBiosample.getSampleId();
					reduceSizeIfTooWide(g, string, maxWidthTop, 2);
					g.drawString(string, textOffsetPixel, cy);
				}
				if(topBiosample.getBiotype().getSampleNameLabel()!=null) {
					cy+=5+fontSizeIncrease;
					g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 5+fontSizeIncrease));
					string = topBiosample.getSampleName();
					reduceSizeIfTooWide(g, string, maxWidthTop, 2);
					g.drawString(string, textOffsetPixel, cy);
				}
			}
		}

		//1. prepares text to print: blocNo or samplename, parent.samplename
		string = "";

		if(c.getContainerType().isMultiple() && printTemplate.isShowBlocNo()) {
			//Add blocNo
			Integer blocNo = c.getBlocNo();
			if(blocNo!=null && c.getContainerType().getBlocNoPrefix()!=null) string = (string.length()>0?"\n":"") + c.getContainerType().getBlocNoPrefix() + blocNo;
		}
		{
			EnumSet<InfoFormat> nameEnumSet = EnumSet.noneOf(InfoFormat.class);
			if(!printTemplate.isShowParent() || !c.getBiosamples().contains(topBiosample)) nameEnumSet.add(InfoFormat.SAMPLENAME);
			if(printTemplate.isShowParent() && !Biosample.getParents(c.getBiosamples()).contains(topBiosample)) nameEnumSet.add(InfoFormat.PARENT_SAMPLENAME);
			string += (string.length()>0?"\n":"") + Biosample.getInfos(c.getBiosamples(), nameEnumSet, InfoSize.ONELINE);

			Biotype biotype = Biosample.getBiotype(c.getBiosamples());
			if(string.length()==0 && biotype!=null && biotype.getCategory()!=BiotypeCategory.PURIFIED) {
				string += (string.length()>0?"\n":"") +  biotype.getName();
			}
		}



		float cy2;
		int cx = cy<18? textOffsetPixel: 0;
		int maxCx = (int) ((pf.getWidth()<=0? 100: pf.getWidth()) - Math.abs(lineOffsetPixel) - 1*MM2PIXELS);
		int maxCy = (int) (pf.getHeight()<=0? 100: pf.getHeight());
		if(cy>=18 && cy<=19) {cy=19;}
		if(cy>=19) {cx=0;}
		if(string!=null && string.length()>0) {
			cy+=4+fontSizeIncrease;
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 4+fontSizeIncrease));
			reduceSizeIfTooWide(g, string, maxCx - cx, 2);
			cy2 = PrinterUtil.print(g, string, cx, cy, maxCx - cx, 10);
		} else {
			cy2 = cy+5;
		}
		int wName = string.indexOf('\n')>0? -1: g.getFontMetrics().stringWidth(string);

		//3. Print metadata
		if(printTemplate.isShowMetadata()) {
			if(study!=null) {
				g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 4+fontSizeIncrease));
				string = Biosample.getInfos(c.getBiosamples(), EnumSet.of(InfoFormat.METATADATA), InfoSize.ONELINE);
				int wMeta = g.getFontMetrics().stringWidth(string);
				if(wName>0 && wName + wMeta <= maxCx-cx) {
					cy = PrinterUtil.print(g, string, cx + (wName+(int)MM2PIXELS), cy, maxCx, 0);
				} else {
					if(cy>=18 && cy<=20) {cy=20;cx=0;} else if(cy>20) {cx=0;}
					cx=0;
					cy = PrinterUtil.print(g, string, cx, cy2, maxCx - cx, Math.max(0, maxCy-cy2-2));
				}
			} else {
				g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 4+fontSizeIncrease));
				string = Biosample.getInfos(c.getBiosamples(), EnumSet.of(InfoFormat.METATADATA), InfoSize.EXPANDED);
				cy = PrinterUtil.print(g, string, cx, cy2, maxCx - cx, Math.max(0, maxCy-cy-2));
			}
		} else {
			cy = cy2;
		}

		//4. Print comments amount (ITALIC)
		if(printTemplate.isShowComments()) {
			cy += fontSizeIncrease;
			string = Biosample.getInfos(c.getBiosamples(), EnumSet.of(InfoFormat.COMMENTS, InfoFormat.AMOUNT), InfoSize.ONELINE);
			g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 4+fontSizeIncrease));
			if(cy>24) cx = 0;
			cy = PrinterUtil.print(g, string, cx, cy, maxCx - cx, 30);
		}

		//Barcode
		if(barcode!=null && barcode.length()>0 && printTemplate.getBarcodePosition()!=0) {


			//Print Barcode
			if(printTemplate.getBarcodePosition()==-1) {
				//barcode on the top right
				g.translate(maxWidth - 7*MM2PIXELS, 1*MM2PIXELS);
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, (int)(6*MM2PIXELS), (int)(6*MM2PIXELS));
				g.translate(.5f*MM2PIXELS, .5f*MM2PIXELS);
				g.setColor(Color.BLACK);
			} else {
				//barcode on the upper left
				g.translate(1*MM2PIXELS, 2.1*MM2PIXELS);
			}

			//			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 3+fontSizeIncrease));
			//			g.drawString(barcode, -1f*MM2PIXELS, -.5f*MM2PIXELS);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 2+fontSizeIncrease));
			g.drawString(barcode, 0, -.5f*MM2PIXELS);

			DataMatrixBean bean = new DataMatrixBean();
			bean.setModuleWidth(.9);
			bean.doQuietZone(false);
			bean.setShape(SymbolShapeHint.FORCE_SQUARE);
			Java2DCanvasProvider canvas = new Java2DCanvasProvider(g, 0);
			bean.generateBarcode(canvas, barcode);
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

	//	public static void setTopMargin(int topMargin) {
	//		PrintableOfContainers.topMargin = topMargin;
	//	}
	//	public static int getTopMargin() {
	//		return topMargin;
	//	}
}