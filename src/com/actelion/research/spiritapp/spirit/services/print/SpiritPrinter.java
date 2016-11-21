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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.standard.Media;

/**
 * The SpiritPrinter util class is responsible for the printer and media's discovery through the Spirit application 
 * @author freyssj
 *
 */
public class SpiritPrinter {

	public static List<Media> loadMedias(PrintService p, final String preferred) {
		Media[] medias = SpiritPrinter.getMedia(p);
		List<Media> res = new ArrayList<Media>();
		if(medias!=null) {
			for (Media media : medias) {
				res.add(media);
			}
			Collections.sort(res, new Comparator<Media>() {
				@Override
				public int compare(Media o1, Media o2) {
					if(preferred!=null) {
						if(o1.toString().toUpperCase().contains(preferred.toUpperCase())) return 1;
						if(o2.toString().toUpperCase().contains(preferred.toUpperCase())) return -1;
					} 					
					return o1.toString().compareToIgnoreCase(o2.toString());
				}
			});
		}
		return res;
	}

	public static PrintService[] getBrotherPrintServices() {
		List<PrintService> printServices = new ArrayList<PrintService>();
		PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService p : services) {
			if(p.getName().toLowerCase().contains("brother") || p.getName().toLowerCase().contains("-br") ) {
				printServices.add(p);
			}
		}
		Collections.sort(printServices, new Comparator<PrintService>() {
			@Override
			public int compare(PrintService o1, PrintService o2) {
				
				if(o1.getName().startsWith("\\\\") && !o2.getName().startsWith("\\\\")) {
					return 1; //Network printer last
				} 
				if(!o1.getName().startsWith("\\\\") && o2.getName().startsWith("\\\\")) {
					return -1;
				} 
				
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		return printServices.toArray(new PrintService[0]);
		
	}
	
	public static PrintService[] getPrintServices() {
		return getPrintServices(null, null);
	}
	
	public static PrintService[] getPrintServices(String name, String media) {
		List<PrintService> printServices = new ArrayList<>();
		PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
		loop: for (PrintService p : services) {
			if(name==null || p.getName().contains(name) ) {
				if(media==null) {
					printServices.add(p);
				} else {
					Media[] mps = (Media[]) p.getSupportedAttributeValues(Media.class, null, null);
					for (Media m : mps) {
						if(m.toString().toUpperCase().contains(media.toUpperCase())) {
							printServices.add(p);
							continue loop;
						}
					}
				}
			}
		}
		Collections.sort(printServices, new Comparator<PrintService>() {
			@Override
			public int compare(PrintService o1, PrintService o2) {
				
				if(o1.getName().startsWith("\\\\") && !o2.getName().startsWith("\\\\")) {
					return 1; //Network printer last
				} 
				if(!o1.getName().startsWith("\\\\") && o2.getName().startsWith("\\\\")) {
					return -1;
				} 				
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		return printServices.toArray(new PrintService[printServices.size()]);
		
	}

	public static PrintService getPrintService(String printer) {
		PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService p : services) {
			if(p.getName().equalsIgnoreCase(printer)) {
				return p;
			}
		}
		return null;		
	}

	public static Media getMedia(PrintService p, String media) {
		Media foundMedia = null;
		Media[] mps = (Media[]) SpiritPrinter.getMedia(p);
		if(mps==null) return null;
		for (Media m : mps) {
			if(m.toString().contains(media)) foundMedia = m;
		}
		return foundMedia;
	}

	public static Media[] getMedia(PrintService p) {
		if(p==null) return null;
		Media[] mps = (Media[]) p.getSupportedAttributeValues(Media.class, null, null);
		return mps;		
	}

	
}
