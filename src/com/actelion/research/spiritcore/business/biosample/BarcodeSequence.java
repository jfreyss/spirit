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

import javax.persistence.*;

@Entity
@Table(name="barcode")
@SequenceGenerator(name="barcode_sequence", sequenceName="barcode_sequence", allocationSize=1)
public class BarcodeSequence {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="barcode_sequence")
	private int id;
	
	private String type;
	
	private String lastBarcode;
	
	public static enum Category {
		BIOSAMPLE, LOCATION, CONTAINER
	}
	
	private Category category = Category.BIOSAMPLE;
	
	public BarcodeSequence() {}
	
	public BarcodeSequence(Category cat, String type, String lastBarcode) {
		this.category = cat;
		this.type = type; 
		this.lastBarcode = lastBarcode;
	}
	
	public String getPrefix() {
		return type;
	}
	
	public void setPrefix(String prefix) {
		this.type = prefix;
	}
	public String getLastBarcode() {
		return lastBarcode;
	}
	public void setLastBarcode(String lastBarcode) {
		this.lastBarcode = lastBarcode;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}

	/**
	 * @param cat the cat to set
	 */
	public void setCategory(Category category) {
		this.category = category;
	}

	/**
	 * @return the cat
	 */
	public Category getCategory() {
		return category;
	}
}
