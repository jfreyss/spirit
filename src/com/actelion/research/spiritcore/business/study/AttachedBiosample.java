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

package com.actelion.research.spiritcore.business.study;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.actelion.research.spiritcore.business.biosample.Biosample;

/**
 * AttachedSample is the class to describe how samples have to be attached in the study/groups
 * 
 * It is used by the randomization procedure or by the manual assignment class to describe how sampleIds have to be linked.
 * 
 * The transient biosample and the transient skipRando are used internally by the algorithm. 
 * 
 *  
 * @author freyssj
 */
public class AttachedBiosample implements Comparable<AttachedBiosample> {

	private int no;
	private String containerId;
	private String sampleId;
	private String sampleName;
	private Double weight;
	private List<Double> dataList = new ArrayList<>();
	private Group group;
	private int subGroup;
	
	
	private transient Biosample biosample;
	private transient boolean skipRando;
	
	public int getNo() {
		return no;
	}
		
	public void setNo(int no) {
		this.no = no;
	}
	
	public String getSampleId() {
		return sampleId;
	}
	public void setSampleId(String sampleId) {
		this.sampleId = sampleId;
	}
	public String getSampleName() {
		return sampleName;
	}
	public void setSampleName(String sampleName) {
		this.sampleName = sampleName;
	}
	public Double getWeight() {
		return weight;
	}
	public void setWeight(Double weight) {
		this.weight = weight;
	}
	
	public int getSubGroup() {
		return subGroup;
	}
	
	public void setSubGroup(int subGroup) {
		if(group==null) subGroup = 0;
		if(group!=null && (subGroup<0 || subGroup>=group.getNSubgroups())) subGroup = 0;
		this.subGroup = subGroup;
	}

	public Group getGroup() {
		return group;
	}
	public void setGroup(Group group) {
		this.group = group;
		setSubGroup(subGroup);
	}
	public String getContainerId() {
		return containerId==null? "": containerId;
	}
	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}	
	public void setBiosample(Biosample biosample) {
		this.biosample = biosample;
	}
	public Biosample getBiosample() {
		return biosample;
	}	
	public List<Double> getDataList() {
		return dataList;
	}
	public boolean isSkipRando() {
		return skipRando || (biosample!=null && !biosample.getStatus().isAvailable());
	}
	public void setSkipRando(boolean skipRando) {
		this.skipRando = skipRando;
	}
	

	@Override
	public int hashCode() {
		return no;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;		
		return no>0 && no==((AttachedBiosample)obj).no;
	}
	
	@Override
	public int compareTo(AttachedBiosample o) {
		boolean thisIsNull = getSampleId()==null || getSampleId().length()==0;
		boolean oIsNull = o.getSampleId()==null || o.getSampleId().length()==0;
		
		if(thisIsNull && !oIsNull) return 1;
		if(!thisIsNull && oIsNull) return -1;
		
		
		return no - o.no;
	}
	
	/**
	 * Extract the the data from a list of rows, indexed by the index (index<0 -> weight)
	 * 
	 * @param rows
	 * @param index
	 * @return
	 */
	public static List<Double> getData(List<AttachedBiosample> rows, int index) {
		List<Double> res = new ArrayList<Double>();
		for (AttachedBiosample s : rows) {
			res.add(index<0? s.getWeight(): index<s.getDataList().size()? s.getDataList().get(index): null);
		}		
		return res;

	}
	


	@Override
	public String toString() {
		return sampleId + " ["+sampleName+"] - "+ group;
	}
	

	public String serialize() {		
		StringBuilder sb = new StringBuilder();
		sb.append(no + ":" + 
			(sampleName==null?"":sampleName) + ":" +
			(weight==null?"":weight) + ":" +
			(sampleId==null?"":sampleId) + ":" +
			(group==null?"":(int)group.getId()) + ":" +
			(containerId==null?"":containerId) + ":" +
			subGroup);
		
		for (Double d : dataList) {
			sb.append(":"+(d==null?"":d));
		}
		return sb.toString();
	}		
	
	
	
	public static List<Biosample> getBiosamples(Collection<AttachedBiosample> attachedBiosamples) {
		if(attachedBiosamples==null) return null;
		List<Biosample> biosamples = new ArrayList<>();
		for (AttachedBiosample b : attachedBiosamples) {
			if(b!=null && b.getBiosample()!=null) biosamples.add(b.getBiosample());
		}
		return biosamples;
	}

}

