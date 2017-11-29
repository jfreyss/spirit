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

package com.actelion.research.spiritapp.print;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;


public class CagePrinterPDF {


	public static void printCages(List<Container> cages, boolean printGroupsTreatments, boolean printTreatmentDesc, boolean whiteBackground) throws Exception {
		final int margin = 15;
		File f = File.createTempFile("cages__", ".pdf");
		Document doc = new Document(PageSize.A4.rotate());
		Image maleImg, femaleImg, nosexImg;
		try {
			maleImg = Image.getInstance(CagePrinterPDF.class.getResource("male.png"));
			femaleImg = Image.getInstance(CagePrinterPDF.class.getResource("female.png"));
			nosexImg = Image.getInstance(CagePrinterPDF.class.getResource("nosex.png"));
		} catch (Exception e) {
			throw new Exception("Could not find images in the classpath: "+e);
		}

		FileOutputStream os = new FileOutputStream(f);
		PdfWriter writer = PdfWriter.getInstance(doc, os);
		doc.open();


		PdfContentByte canvas = writer.getDirectContentUnder();

		float tileW = (doc.getPageSize().getWidth()) /4;
		float tileH = (doc.getPageSize().getHeight()) /2;
		for (int i = 0; i < cages.size(); i++) {
			Container cage = cages.get(i);

			Study study = cage.getStudy();
			Set<Group> groups = cage.getGroups();
			Group group = cage.getGroup();
			Set<Biosample> animals = new TreeSet<>(Biosample.COMPARATOR_NAME);
			animals.addAll(cage.getBiosamples());

			//Find the treatments applied to this group
			Set<NamedTreatment> allTreatments = new LinkedHashSet<>();
			for(Group gr: groups) {
				allTreatments.addAll(gr.getAllTreatments(-1));
			}

			//Draw
			if(i%8==0) {
				if(i>0)doc.newPage();
				drawCageSeparation(doc, canvas);
			}

			int col = (i%8)%4;
			int row = (i%8)/4;


			float x = margin + tileW*col;
			float x2 = x + tileW - margin;
			float baseY = tileH*row;
			float y;

			//Display Sex
			canvas.moveTo(tileW*col - 50, doc.getPageSize().getHeight() - (tileH*row+50));
			Image img = null;
			String sex = getMetadata(animals, "Sex");
			if(study.isBlindAll()) {
				img = nosexImg;
			} else if(sex.equals("M")) {
				img = maleImg;
			} else if(sex.equals("F")) {
				img = femaleImg;
			} else if(sex.length()>0) {
				img = nosexImg;
			}
			if(img!=null) {
				img.scaleToFit(20, 20);
				img.setAbsolutePosition(tileW*(col+1) - 33, doc.getPageSize().getHeight() - (tileH*row+12+margin));
				doc.add(img);
			}
			if(group!=null && group.getColor()!=null && (study!=null && !study.isBlind()) && !whiteBackground) {
				Color c = group.getColor();
				canvas.saveState();
				canvas.setRGBColorFill(c.getRed()/3+170, c.getGreen()/3+170, c.getBlue()/3+170);
				canvas.rectangle(x-margin+1, doc.getPageSize().getHeight() - baseY - tileH+1, tileW-2, tileH-2);
				canvas.fill();
				canvas.restoreState();
			}
			BaseColor treatmentColor = BaseColor.BLACK;
			if(allTreatments.size()>0 && !study.isBlind() && printGroupsTreatments) {
				int offset = 0;
				for (NamedTreatment t : allTreatments) {
					if(t.getColor()!=null) {
						if(allTreatments.size()==1) treatmentColor = new BaseColor(t.getColor().getRed()/2, t.getColor().getGreen()/2, t.getColor().getBlue()/2);
						canvas.saveState();
						canvas.setColorStroke(BaseColor.BLACK);
						canvas.setRGBColorFill(t.getColor().getRed(), t.getColor().getGreen(), t.getColor().getBlue());
						y = baseY+42+15+86+13+22+14+23+28;
						canvas.rectangle(x+20 + offset*5, doc.getPageSize().getHeight() - y - (offset%2)*4, 22, 22);
						canvas.fillStroke();
						canvas.restoreState();
						offset++;
					}
				}
			}


			canvas.beginText();
			canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, "Cp1252", false), 16f);
			canvas.showTextAligned(Element.ALIGN_LEFT, cage.getContainerId(), x, doc.getPageSize().getHeight() - (baseY + 23), 0);

			canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, "Cp1252", false), 11f);
			y =42 + baseY; canvas.showTextAligned(Element.ALIGN_LEFT, "Type: ", x, doc.getPageSize().getHeight() - y , 0);
			y+=15; canvas.showTextAligned(Element.ALIGN_LEFT, "Animals_ID: ", x, doc.getPageSize().getHeight() - y , 0);
			y+=86; canvas.showTextAligned(Element.ALIGN_LEFT, "Delivery date: ", x, doc.getPageSize().getHeight() - y , 0);
			y+=13; canvas.showTextAligned(Element.ALIGN_LEFT, "PO Number: ", x, doc.getPageSize().getHeight() - y , 0);
			y+=22; canvas.showTextAligned(Element.ALIGN_LEFT, "Study: ", x, doc.getPageSize().getHeight() - y , 0);
			y+=14; if(study!=null && !study.isBlind() && printGroupsTreatments)canvas.showTextAligned(Element.ALIGN_LEFT, "Group: ", x, doc.getPageSize().getHeight() - y , 0);
			y+=23; if(study!=null && !study.isBlind() && printGroupsTreatments)canvas.showTextAligned(Element.ALIGN_LEFT, "Treatment: ", x, doc.getPageSize().getHeight() - y , 0);
			y+=50; canvas.showTextAligned(Element.ALIGN_LEFT, "License: ", x, doc.getPageSize().getHeight() - y , 0);
			y+=13; canvas.showTextAligned(Element.ALIGN_LEFT, "Experimenter: ", x, doc.getPageSize().getHeight() - y , 0);
			canvas.endText();

			y = 42 + baseY; print(canvas, study.isBlindAll()? "Blinded": getMetadata(animals, "Type"), x+65, doc.getPageSize().getHeight() - y , x2, 11, 11, FontFactory.HELVETICA, BaseColor.BLACK, 11f);
			y+=15;

			if(animals.size()<=6) {
				int n = 0;
				for (Biosample animal: animals) {
					print(canvas, animal.getSampleId(), x+75, doc.getPageSize().getHeight() - y - 12*n, x2-50, 12, 12, FontFactory.HELVETICA, BaseColor.BLACK, 11f);
					if(animal.getSampleName()!=null && animal.getSampleName().length()>0) {
						print(canvas, "[ " + animal.getSampleName() + " ]", x2-60, doc.getPageSize().getHeight() - y - 12*n, x2, 12, 12, FontFactory.HELVETICA_BOLD, BaseColor.BLACK, 11f);
					}
					n++;
				}
			} else {
				int nPerRow = animals.size()/2;
				int n = 0;
				for (Biosample animal: animals) {
					int nx = n / nPerRow;
					int ny = n%nPerRow;
					print(canvas, animal.getSampleId(), x+nx*(x2-x)/2, doc.getPageSize().getHeight() - y - 12*ny-12, x+(1+nx)*(x2-x)/2, 12, 12, FontFactory.HELVETICA, BaseColor.BLACK, 10f);
					if(animal.getSampleName()!=null && animal.getSampleName().length()>0) {
						print(canvas, "[ " + animal.getSampleName() + " ]", x+(1+nx)*(x2-x)/2-35, doc.getPageSize().getHeight() - y - 12*ny-12, x+(1+nx)*(x2-x)/2, 12, 12, FontFactory.HELVETICA_BOLD, BaseColor.BLACK, 10f);
					}
					n++;
				}
			}

			y+=86; print(canvas, getMetadata(animals, "Delivery Date"), x+75, doc.getPageSize().getHeight() - y, x2, 0, 10, FontFactory.HELVETICA, BaseColor.BLACK, 10f);
			y+=13; print(canvas, getMetadata(animals, "PO Number"), x+75, doc.getPageSize().getHeight() - y, x2, 0, 10, FontFactory.HELVETICA, BaseColor.BLACK, 10f);
			y+=22; print(canvas, study.getStudyIdAndInternalId(), x+40, doc.getPageSize().getHeight() - y, x2, 0, 11, BaseFont.HELVETICA_BOLD, BaseColor.BLACK, 11f);
			y+=14; if(!study.isBlind() && printGroupsTreatments) print(canvas, getGroups(animals), x+40, doc.getPageSize().getHeight() - y, x2, 22, 11, BaseFont.HELVETICA_BOLD, BaseColor.BLACK, 11f);
			y+=23; if(!study.isBlind() && printGroupsTreatments) print(canvas, getTreatments(animals, printTreatmentDesc), x+62, doc.getPageSize().getHeight() - y, x2, 50, printTreatmentDesc?9: 12, FontFactory.HELVETICA, treatmentColor, printTreatmentDesc?9f: 10f);
			y+=50; print(canvas, study.getMetadataMap().get("LICENSENO"), x+74, doc.getPageSize().getHeight() - y, x2, 15, 10, FontFactory.HELVETICA, BaseColor.BLACK, 10f);
			y+=13; print(canvas, study.getMetadataMap().get("EXPERIMENTER"), x+74, doc.getPageSize().getHeight() - y, x2, 20, 10, FontFactory.HELVETICA, BaseColor.BLACK, 10f);
		}

		doc.close();
		os.close();
		Desktop.getDesktop().open(f);
		try {Thread.sleep(500);}catch (Exception e) {}

	}

	private static void print(PdfContentByte canvas, String txt, float x, float y, float maxX, int maxHeight, int lineHeight, String fontName, BaseColor color, float fontSize) throws Exception {
		float width = maxX - x - 5;
		float minY = Math.min(y, y - maxHeight + lineHeight - 3);
		if(txt==null) return;

		canvas.setFontAndSize(BaseFont.createFont(fontName, "Cp1252", false), fontSize);
		while(txt.length()>0 && y>=minY) {
			int index = 0;
			while(true) {
				int index2 = index+1;
				if(index2>=txt.length()) {index=index2;break;}
				float w = canvas.getEffectiveStringWidth(txt.substring(0, index2), true);
				System.out.println("CagePrinterPDF.print()   "+ txt.substring(0, index2)+" "+w+","+width);
				if(w>width) break;
				index = index2;
			}
			if(index<0) index = txt.length();
			String t = txt.substring(0, index);
			System.out.println("CagePrinterPDF.print() "+t+" at " + x+"x"+y+ " (of "+txt+") minY="+minY);
			txt = txt.substring(index).trim();
			Phrase phrase = new Paragraph(t, FontFactory.getFont(fontName, fontSize, color));
			ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, phrase, x, y, 0);
			y-=lineHeight;
		}
	}

	private static String getGroups(Collection<Biosample> animals) {
		Set<String> res = new LinkedHashSet<>();
		for (Group g : Biosample.getGroups(animals)) {
			res.add(g==null? "N/A": g.getName());
		}
		return MiscUtils.flatten(res);
	}

	private static String getTreatments(Collection<Biosample> animals, boolean printTreatmentDesc) {
		Set<String> res = new LinkedHashSet<>();
		Set<Group> groups = Biosample.getGroups(animals);
		for(Group gr: groups) {
			String s = gr==null? "": gr.getTreatmentDescription(-1, groups.size()>1? false: printTreatmentDesc);
			res.add(s==null || s.length()==0? "N/A": s);
		}
		return MiscUtils.flatten(res);
	}

	private static String getMetadata(Collection<Biosample> animals, String metadata) {
		Set<String> res = new HashSet<>();
		for (Biosample a : animals) {
			BiotypeMetadata bm = a.getBiotype().getMetadata(metadata);
			String m = bm==null || a.getMetadataValue(bm)==null? "N/A": a.getMetadataValue(bm).replaceAll("/", " ");
			res.add(m);
		}
		return MiscUtils.flatten(res);
	}

	private static void drawCageSeparation(Document doc, PdfContentByte canvas) {
		canvas.setLineWidth(1f);
		canvas.setColorStroke(BaseColor.BLACK);
		canvas.moveTo(0, doc.getPageSize().getHeight()/2);
		canvas.lineTo(doc.getPageSize().getWidth(), doc.getPageSize().getHeight()/2);
		canvas.moveTo(doc.getPageSize().getWidth()/4, 0);
		canvas.lineTo(doc.getPageSize().getWidth()/4, doc.getPageSize().getHeight());
		canvas.moveTo(2*doc.getPageSize().getWidth()/4, doc.getPageSize().getBottom());
		canvas.lineTo(2*doc.getPageSize().getWidth()/4, doc.getPageSize().getHeight());
		canvas.moveTo(3*doc.getPageSize().getWidth()/4, 0);
		canvas.lineTo(3*doc.getPageSize().getWidth()/4, doc.getPageSize().getHeight());
		canvas.stroke();
	}
}
