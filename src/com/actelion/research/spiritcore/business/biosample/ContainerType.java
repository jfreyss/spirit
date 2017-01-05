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

package com.actelion.research.spiritcore.business.biosample;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.persistence.Transient;


/**
 * @author freyssj
 */
public enum ContainerType {
	CRYOTUBE("Cryotube",				"Cryo",		"cryotube", 	BrotherFormat._12x42,		BarcodeType.MATRIX),
	TUBE_0_5PP("Matrix 0.5PP",			"Ma0.5",	"matrix05", 	BrotherFormat._12x33,		BarcodeType.MATRIX),
	TUBE_1PP("Matrix 1.0PP",			"Ma1.0",	"matrix10", 	BrotherFormat._12x33,		BarcodeType.MATRIX),
	TUBE_FX0_5("FluidX 0.5PP",			"Fx0.5", 	"fluidx", 		BrotherFormat._12x33,		BarcodeType.MATRIX),
	TUBE_FX1_4("FluidX 1.4PP",			"Fx1.4", 	"fluidx", 		BrotherFormat._12x33,		BarcodeType.MATRIX),
	TUBE_FX20("FluidX 20ml",			"Fx20", 	"fluidx", 		BrotherFormat._12x62,		BarcodeType.MATRIX),
	TUBE_E0_5("Eppendorf 0.5ml",		"Ep0.5",	"eppendorf", 	BrotherFormat._12x33,		BarcodeType.NOBARCODE),
	TUBE_E1_5("Eppendorf 1.5ml",		"Ep1.5", 	"eppendorf", 	BrotherFormat._12x42,		BarcodeType.NOBARCODE),
	TUBE_E2_0("Eppendorf 2.0ml",		"Ep2.0", 	"eppendorf", 	BrotherFormat._12x42,		BarcodeType.NOBARCODE),
	TUBE_1_4PP("Micronics 1.4PP",		"Mi1.4", 	"micronics", 	BrotherFormat._12x33,		BarcodeType.NOBARCODE),
	TUBE_FA15("Falcon 15ml",			"Fa15", 	"tube_small", 	BrotherFormat._12x49,		BarcodeType.NOBARCODE),
	TUBE_FA50("Falcon 50ml",			"Fa50", 	"tube_large", 	BrotherFormat._12x62N,		BarcodeType.NOBARCODE),
	TUBE_MICRO("Microtainer",			"Mi.", 		"micro", 		BrotherFormat._12x42,		BarcodeType.NOBARCODE),
	TUBE_5("Tube 5ml",					"5ml", 		"tube_5", 		BrotherFormat._12x49,		BarcodeType.NOBARCODE),
	TUBE_9("Tube 9ml",					"9ml", 		"tube_5", 		BrotherFormat._12x49,		BarcodeType.NOBARCODE),
	DNA_WHATMAN("DNA Card Whatman",		"DNA", 		"dna_whatman", 	BrotherFormat._12x33N,		BarcodeType.NOBARCODE),
	K7("Cassette",						"Cass.",	"cassette", 	null,						BarcodeType.GENERATE, 8, "C."),
	BOTTLE("Bottle",					"Bottle", 	"bottle", 		BrotherFormat._12x33N,		BarcodeType.GENERATE, 99, "B."),
	SLIDE("Slide",						"Slide", 	"slide", 		BrotherFormat._18x24,		BarcodeType.GENERATE, 8, "Bl."),
	CAGE("Cage",						"Cage", 	"cage", 		null,						BarcodeType.GENERATE, 8, null),
	UNKNOWN("Other",					"?", 		"unknown", 		BrotherFormat._12x33N,		BarcodeType.NOBARCODE),
	;
	
	
	private final String name;
	private final String shortName;
	private final String img;
	private final BrotherFormat brotherFormat;
	private final BarcodeType barcodeType;
	private final int maxSize;
	private final String blocNoPrefix;

	private ContainerType(String name, String shortName, String img, BrotherFormat brotherFormat, BarcodeType barcodeType) {
		this(name, shortName, img, brotherFormat, barcodeType, 1, null);
				
	}
	private ContainerType(String name, String shortName, String img, BrotherFormat brotherFormat, BarcodeType barcodeType, int maxSize, String blocNoPrefix) {
		this.img = img;
		this.name = name;
		this.shortName = shortName;
		this.brotherFormat = brotherFormat;
		this.barcodeType = barcodeType;
		this.maxSize = maxSize;
		this.blocNoPrefix = blocNoPrefix;
	}
	
	public String getShortName() {
		return shortName;
	}
	
	public boolean isMultiple() {
		return maxSize>1;
	}
	
	@Override
	public String toString() {
		return name;
	}
	public String getName() {
		return name;
	}
	
	
	public BrotherFormat getBrotherFormat() {
		return brotherFormat;
	}
	
	public String[] getAlternativeMedia() {
		return brotherFormat==null? new String[] {}: brotherFormat.getMedias();
	}

	public int getMaxSize() {
		return maxSize;
	}

	@Transient
	public static final Map<String, Image> smallImages = new HashMap<>();
	

	public Image getImage(int dim) {
		Image img = getImage();
		return img==null? null: img.getScaledInstance(dim, dim, Image.SCALE_DEFAULT);		
	}

	public Image getImage() {
		Image image = smallImages.get(name);
		if(image==null) {
			synchronized (smallImages) {		
				image = smallImages.get(name);
				if(image==null) {
					String n = img;
					if(n==null) n = "";
					if(n.indexOf(' ')>0) n = n.substring(0, n.indexOf(' '));
					URL url = getClass().getResource(n+".png");
					if(url!=null) {
						try {
							image = ImageIO.read(url);
//							image = image.getScaledInstance(40, 40, Image.SCALE_DEFAULT);
						} catch (Exception e) {
							e.printStackTrace();
							System.err.println("no image for "+name.toLowerCase()+".png  "+e);
						}
					} 
					if(image==null) {
						image = new BufferedImage(40,40,BufferedImage.TYPE_INT_ARGB);
						Graphics2D g = (Graphics2D) image.getGraphics();
						g.setColor(new Color(255,255,255,255));
						g.dispose();
					}
				}
				smallImages.put(name, image);
			}
		}
		return image;
	}
	
	public static ContainerType get(String s) {
		for (ContainerType ct : values()) {
			if(ct.getName().equalsIgnoreCase(s)) return ct;			
		}
		return null;
	}
	
	public BarcodeType getBarcodeType() {
		return barcodeType;
	}
	
	public static ContainerType[] valuesOfRackable() {
		List<ContainerType> res = new ArrayList<ContainerType>();
		for (ContainerType ct : values()) {
			if(!ct.isMultiple()) {
				res.add(ct);
			}
		}
		return res.toArray(new ContainerType[res.size()]);
	}

	public String getBlocNoPrefix() {
		return blocNoPrefix;
	}
	
	public String[] getMedias() {
		if(getAlternativeMedia()==null) return new String[] {getName()};
		String[] res = new String[getAlternativeMedia().length+1];
		res[0] = getName();
		for (int i = 1; i < res.length; i++) {
			res[i] = getAlternativeMedia()[i-1];
		}
		return res;
	}
}
