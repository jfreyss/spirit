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

package com.actelion.research.spiritapp.ui.util.icons;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.util.ui.FastFont;

public class ImageFactory {

	private static final Map<String, BufferedImage> images = Collections.synchronizedMap(new HashMap<String, BufferedImage>());
	private static final BufferedImage emptyImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
	
	public static void clearCache() {
		images.clear();
	}
	public static BufferedImage createImage() {
		return emptyImage;
	}
	
	public static BufferedImage createImage(int width, boolean hasAlpha) {
		BufferedImage image = images.get("empty_"+width+"_"+hasAlpha);
		if(image==null) {
			image = new BufferedImage(width, width, hasAlpha? BufferedImage.TYPE_INT_ARGB: BufferedImage.TYPE_INT_RGB);
			if(!hasAlpha) {
				Graphics2D g = (Graphics2D) image.getGraphics();
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, width, width);
				g.dispose();
			}
			images.put("empty_"+width+"_"+hasAlpha, image);
		}
		return image;
	}
	
	public static BufferedImage getImage(String key) {
		if(key==null) return null;
		key = key.toLowerCase();

		if(images.containsKey(key)) {
			return images.get(key);
		} else {
			BufferedImage image = null;
			URL url = ImageFactory.class.getResource(key+".png");
			if(url!=null) {
				try {
					image = ImageIO.read(url);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("no image for "+key+".png  "+e);
				}
			}
			
			if(image==null) {
				int index = key.indexOf(' ');
				int index2 = key.indexOf('/');
				int index3 = key.indexOf('(');
				
				if(index2>0 && (index<=0 || index2<index)) index = index2;
				if(index3>0 && (index<=0 || index3<index)) index = index3;
				
				if(index>0) image = getImage(key.substring(0, index).trim());
			}
			images.put(key, image);
		}
		BufferedImage img = images.get(key);
		return img;
	}
	
	public static BufferedImage getImage(String key, int width) {
		key = key==null?"": key.toLowerCase();
		if(!images.containsKey(key+"_"+width)) {
			BufferedImage img = getImage(key);
			BufferedImage img2;
			if(img!=null && width>0) {
				img2 = new BufferedImage(width, img.getHeight() * width / img.getHeight(), img.getType());
				Graphics2D g2 = (Graphics2D) img2.getGraphics();
				g2.drawImage(img, 0, 0, img2.getWidth(), img2.getHeight(), null);
				g2.dispose();
			} else {
				img2 = null;
			}
			images.put(key+"_"+width, img2);
		}
		return images.get(key+"_"+width);
	}
	
	
	
	
	public static String getImageKey(Biosample biosample) {
		if(biosample==null) return null;
		String imageKey = null;
		try {
			if(biosample.getBiotype()==null) return null;
			Image img = null;
			
			//Find the image if the biosample is living with a type 
			if(biosample.getBiotype().getCategory()==BiotypeCategory.LIVING) {
				String m = biosample.getMetadataValue("Type");
				if(m!=null && m.length()>0) {
					img = ImageFactory.getImage(m);
					imageKey = m;
				}
			}
			
			//Find the image if the biosample has a name, labeled Type
			if(img==null && biosample.getBiotype().getSampleNameLabel()!=null && biosample.getBiotype().getSampleNameLabel().startsWith("Type")) {
				String s = biosample.getSampleName();
				if(s!=null) {
					img = ImageFactory.getImage(s);
					imageKey = s;
				}
			}
			
			//Find the image based on the first metadata
			if(img==null && biosample.getBiotype().getMetadata().size()>0) {
				for (BiotypeMetadata m : biosample.getBiotype().getMetadata()) {
					String s = biosample.getMetadataValue(m);
					if(s!=null && s.length()>0) {
						img = ImageFactory.getImage(s);
						imageKey = s;
					}
					break;					
				}
			}
			

			//By default, Use the image of the biotype
			if(img==null) {
				img = ImageFactory.getImage(biosample.getBiotype().getName());
				imageKey = biosample.getBiotype().getName();
			}
			if(img==null && biosample.getBiotype().getParent()!=null && biosample.getParent()!=null && biosample.getBiotype().getParent().equals(biosample.getParent().getBiotype())) {
				return getImageKey(biosample.getParent());
			}
						
			
			if(img!=null) {
				return imageKey==null? null: imageKey.replaceAll("'", "");
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	public static BufferedImage getImage(Biosample biosample, int width) {
		String imageKey = getImageKey(biosample);
		if(imageKey!=null && imageKey.length()>0) return ImageFactory.getImage(imageKey, width);
		else return ImageFactory.createImage(width, true);
	}
	
	

	public static Image getImage(Biotype type) {
		Image img = ImageFactory.getImage(type.getName());
		if(img!=null) return img;
		else return ImageFactory.createImage();
	}
	
	public static Image getImage(Biotype type, int width) {
		Image img = ImageFactory.getImage(type.getName(), width);
		if(img!=null) return img;
		else return ImageFactory.createImage(width, true);
	}
	
	public static Image getImageThumbnail(Biotype type) {
		int size = FastFont.getAdaptedSize(22);
		if(type==null) return ImageFactory.createImage(size, true);
		Image img =  ImageFactory.getImage(type.getName(), size);
		if(img!=null) return img;
		if(type.getParent()!=null) return getImageThumbnail(type.getParent());
		return ImageFactory.createImage(22, true);
	}
		
}
