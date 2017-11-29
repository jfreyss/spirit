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

public class PrintLabel {

	private String barcodeId;
	private String label;
	
	
	public PrintLabel() {
	}
	
	public PrintLabel(String label) {
		this(null, label);
	}
	
	public PrintLabel(String barcodeId, String label) {
		this.barcodeId = barcodeId;
		this.label = label;
	}
	
	public String getBarcodeId() {
		return barcodeId;
	}
	public void setBarcodeId(String barcodeId) {
		this.barcodeId = barcodeId;
	}
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
}
