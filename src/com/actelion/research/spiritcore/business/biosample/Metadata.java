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

import java.io.Serializable;
import java.util.Arrays;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.util.MiscUtils;

/**
 * Metadata represents a unserialized biosample's metadata. All metadata are serialized in one database column.
 */
public class Metadata implements Serializable {

	public static final String DATEOFDEATH = "DateOfDeath";
	public static final String STAINING = "Staining";

	private BiotypeMetadata biotypeMetadata;
	
	private String value = "";
	
	private Biosample biosample;
	
	private Biosample linkedBiosample;
	
	private Document linkedDocument;
	
	public Metadata() {}
	
	public BiotypeMetadata getBiotypeMetadata() {
		return biotypeMetadata;
	}

	/**
	 * The value is never null but can have a 0 length
	 * @return
	 */
	public String getValue() {
		return value==null?"": value;
	}

	public void setBiotypeMetadata(BiotypeMetadata biotypeMetadata) {
		this.biotypeMetadata = biotypeMetadata;
	}

	public void setValue(String value) {
		this.value = value==null? "": value.trim();
		
		//Clean Value
		if(biotypeMetadata.getDataType()==DataType.MULTI) {
			//Sort values
			String[] v = MiscUtils.split(this.value, ";");
			Arrays.sort(v);
			this.value = MiscUtils.unsplit(v, ";");
		}
	}


	/**
	 * @param biosample the biosample to set
	 */
	public void setBiosample(Biosample biosample) {
		this.biosample = biosample;
	}

	/**
	 * @return the biosample
	 */
	public Biosample getBiosample() {
		return biosample;
	}

	public Biosample getLinkedBiosample() {
		return linkedBiosample;
	}

	public void setLinkedBiosample(Biosample linkedBiosample) {
		this.linkedBiosample = linkedBiosample;
		setValue(linkedBiosample==null?null: linkedBiosample.getSampleId());
	}

	public Document getLinkedDocument() {
		return linkedDocument;
	}

	public void setLinkedDocument(Document linkedDocument) {
		this.linkedDocument = linkedDocument;
		setValue(linkedDocument==null? "": linkedDocument.getFileName());
	}

	@Override
	public String toString() {
		return getValue();
	}
	
	@Override
	public int hashCode() {
		return biotypeMetadata==null?0: biotypeMetadata.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return this==obj;
	}

}
