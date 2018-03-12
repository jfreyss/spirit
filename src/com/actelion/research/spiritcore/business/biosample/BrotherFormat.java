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

package com.actelion.research.spiritcore.business.biosample;

public enum BrotherFormat {

	_9x24("9mmx24_1", 0),

	_12x33("12mmx33_1", 8),
	_12x33N("12mmx33_1", 0),
	_12x42("12mmx42_1", 8),

	_12x49("12mmx49_1", 10),
	_12x62("12mmx62_1", 10)
	,
	_12x62N("12mmx62_1", 0),

	_18x24("18mmx24_1", 0),

	;

	private final String media;
	private final float lineOffset;

	/**
	 *
	 * @param medias
	 * @param lineOffset - distance of line from the left border in mm
	 * @param barcodeOnRight
	 */
	private BrotherFormat(String media,  float lineOffset) {
		this.media = media;
		this.lineOffset = lineOffset;
	}

	public String getMedia() {
		return media;
	}

	public float getLineOffset() {
		return lineOffset;
	}

}
