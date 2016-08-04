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

package com.actelion.research.spiritcore.business.slide;

import java.util.ArrayList;
import java.util.List;

public class ContainerTemplate {

	private int blocNo = 1;
	
	/**
	 * Describes the samples that should be in this container
	 */
	private List<SampleDescriptor> sampleDescriptors = new ArrayList<>();
	
	/**
	 * Describes the number of items for each staining (only for slides)
	 */
	private List<Duplicate> duplicates = new ArrayList<>();
	
	public static class Duplicate {
		private int nDuplicates;
		private String staining;
		private String sectionNo;
		
		public int getNDuplicates() {
			return nDuplicates;
		}
		public void setNDuplicates(int nDuplicates) {
			this.nDuplicates = nDuplicates;
		}
		public String getStaining() {
			return staining;
		}
		public void setStaining(String staining) {
			this.staining = staining;
		}
		public String getSectionNo() {
			return sectionNo;
		}
		public void setSectionNo(String sectionNo) {
			this.sectionNo = sectionNo;
		}
	}

	
	/**
	 * Gets list of duplicates->n, staining, sectionNo to be applied on this  template
	 * Only used for slides
	 * @return
	 */
	public List<Duplicate> getDuplicates() {
		return duplicates;
	}

	public void setDuplicates(List<Duplicate> duplicates) {
		this.duplicates = duplicates;
	}

	/**
	 * Gets the description of the sample that should be in the container 
	 * @return
	 */
	public List<SampleDescriptor> getSampleDescriptors() {
		return sampleDescriptors;
	}


	public void setSampleDescriptors(List<SampleDescriptor> samples) {
		this.sampleDescriptors = samples;
	}

	
	
	public int getBlocNo() {
		return blocNo;
	}

	public void setBlocNo(int blocNo) {
		this.blocNo = blocNo;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[ContainerTemplate  x"+getDuplicates().size()+":");
		for (SampleDescriptor s : sampleDescriptors) {
			sb.append(" "+s.toString());
		}
		sb.append("]");
		return sb.toString();
	}

}
