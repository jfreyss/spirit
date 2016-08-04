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

public enum BrotherFormat {

	_12x33(new String[] {"12mmx33_1"}, 8, false),
	_12x33N(new String[] {"12mmx33_1"}, 0, false),
	
	_12x42(new String[] {"12mmx42_1"}, 8, false),
	
	_12x49(new String[] {"12mmx49_1"}, 10, false),
//	_12x49N(new String[] {"12mmx49_1"}, 0, false),
	
	_12x62(new String[] {"12mmx62_1"}, 10, false),
	_12x62N(new String[] {"12mmx62_1"}, 0, false),
	
	_18x24(new String[] {"18mmx24_1"}, 0, true);
	
	private final String[] medias;
	private final float lineOffset;
	private final boolean barcodeOnRight;
	
	/**
	 * 
	 * @param medias
	 * @param lineOffset - distance of line from the left border in mm
	 * @param barcodeOnRight 
	 */
	private BrotherFormat(String[] medias,  float lineOffset, boolean barcodeOnRight){
		this.medias = medias;
		this.lineOffset = lineOffset;
		this.barcodeOnRight = barcodeOnRight;		
	}
	
	public String[] getMedias() {
		return medias;
	}
	
	public float getLineOffset() {
		return lineOffset;
	}
	
	public boolean isBarcodeOnRight() {
		return barcodeOnRight;
	}
//
//	MarginConfig config;
//	switch(c.getContainerType()) {
//		case CRYOTUBE:
//		case TUBE_0_5PP:
//		case TUBE_1PP:
//		case TUBE_E0_5:
//		case TUBE_E1_5:
//		case TUBE_E2_0:
//		case TUBE_1_4PP:
//			config = new MarginConfig(8 * (leftHanded?-1: 1), 22);		
//			break;
//		case UNKNOWN:
//		case DNA_WHATMAN:
//			config = new MarginConfig(0, 22);		
//			break;
//		case TUBE_5:
//		case TUBE_9:
//		case TUBE_FA15:
//		case TUBE_FA50:
//		case TUBE_FX20:
//		case BOTTLE:
//			config = new MarginConfig(10 * (leftHanded?-1: 1), 27);
//			break;
//		case SLIDE:
//			config = new MarginConfig(0, 2.8f);
//			break;
//		default:
//			config = new MarginConfig(8 * (leftHanded?-1: 1), 22);
//	}
}
