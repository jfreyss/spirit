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

package com.actelion.research.spiritcore.business.slide;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

public class SampleDescriptor implements Comparable<SampleDescriptor>, Serializable, Cloneable {

	private Biotype biotype;
	
	private String name;
	
	private String parameters;
	
	private int animalNo = 1;
		
	private Integer blocNo = null;
	
	private ContainerType containerType = null;
	
	/**
	 * Constructor for a default Sampling
	 */
	public SampleDescriptor() {}	

	@Override
	public SampleDescriptor clone() {
		try {
			SampleDescriptor clone = (SampleDescriptor) super.clone();
			return clone;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Creates a Sampling based on the metadata from the biosample
	 * @param b
	 */
	public SampleDescriptor(int animalNo, Biosample b) {
		this.animalNo = animalNo;
		this.biotype = b.getBiotype();
		this.containerType = b.getContainerType();
		this.blocNo = b.getContainer()==null? null: b.getContainer().getBlocNo();

		Map<Integer, String> map = new HashMap<>();
		for (BiotypeMetadata bm : biotype.getMetadata()) {
			String m = b.getMetadataValue(bm);
			if(m!=null) {
				map.put(bm.getId(), m);
			}
		}
		
		name = b.getBiotype().getSampleNameLabel()==null? null: b.getSampleName();
		parameters = MiscUtils.serializeIntegerMap(map);
	}
	
	public Biotype getBiotype() {
		return biotype;
	}
	public void setBiotype(Biotype biotype) {
		this.biotype = biotype;
	}
	protected String getParameters() {
		return parameters;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		Collection<String> values = MiscUtils.deserializeIntegerMap(parameters).values();
		return biotype.getName() + (name!=null? " " + name: "") + (values.size()>0? ": " + MiscUtils.flatten(values): "");
	}
	

	
	public Biosample createCompatibleBiosample() {
		Biosample biosample = new Biosample(biotype);
		
		biosample.setSampleName(name);
		
		//parameters
		Map<Integer, String> map = MiscUtils.deserializeIntegerMap(parameters);
		for (BiotypeMetadata m : biotype.getMetadata()) {
			String data = map.get(m.getId());
			if(data!=null) biosample.setMetadataValue(m, data); 
		}
		
		return biosample;
	}
	
	@Override
	public int compareTo(SampleDescriptor o) {

		
		int c = CompareUtils.compare(biotype, o.biotype);
		if(c!=0) return c;
				
		c = CompareUtils.compare(name, o.name);
		if(c!=0) return c;

		c = CompareUtils.compare(getParameters(), o.getParameters());
		if(c!=0) return c;
			
		c = CompareUtils.compare(getBlocNo(), o.getBlocNo());
		if(c!=0) return c;

		return c;
	}
	
	@Override
	public boolean equals(Object obj) {
		return compareTo((SampleDescriptor)obj)==0;
	}		
		
	public int getAnimalNo() {
		return animalNo;
	}
	
	public void setAnimalNo(int animalNo) {
		this.animalNo = animalNo;
	}

	public ContainerType getContainerType() {
		return containerType;
	}
	
	public void setContainerType(ContainerType containerType) {
		this.containerType = containerType;
	}

	/**
	 * Check if this sample is compatible to allow the generation of slides with this biosample
	 * ie, the name and params must be identicals
	 * does not check container (Cassette/bottle/...)
	 * @param biosample
	 * @param checkSample
	 * @return
	 */
	public boolean isBiosampleCompatible(Biosample biosample) {

		//check biotype
		if(biosample.getBiotype()==null || !biosample.getBiotype().equals(getBiotype())) return false;
		Map<Integer, String> map = MiscUtils.deserializeIntegerMap(parameters);
		
		//check name
		if(biosample.getBiotype().getSampleNameLabel()!=null) {			
			if(CompareUtils.compare(biosample.getSampleName(), getName())!=0) return false;
		}
		//Check metadata
		for (BiotypeMetadata m : getBiotype().getMetadata()) {
			if(m.getName().equals(Biotype.STAINING)) continue;
			String data = map.get(m.getId());
			String me = biosample.getMetadataValue(m);
			if(data==null || data.length()==0) {
				if((me!=null && me.trim().length()>0)) {
					return false;								
				}
			} else {
				if((me==null || !data.equalsIgnoreCase(me))) {
					return false;
				}
			}
		}
		
		return true;
	}

	public Integer getBlocNo() {
		return blocNo;
	}
//
//	public void setBlocNo(String blocNo) {
//		this.blocNo = blocNo;
//	}
	
		
}