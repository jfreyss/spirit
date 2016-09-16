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

package com.actelion.research.spiritlib.pojo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SamplingPojo implements Serializable {

	private int id;
	private String biotype = null;
	private Set<SamplingPojo> children = new HashSet<>();
	
	private boolean weighingRequired = false;
	private boolean commentsRequired = false;
	private boolean lengthRequired = false;
	private MeasurementPojo[] measurements;

	private String sampleName;
	private String containerType;
	private Double amount;
	private Map<String, String> metadata;
	private String comments;
	private Integer blocNo;
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getBiotype() {
		return biotype;
	}
	public void setBiotype(String biotype) {
		this.biotype = biotype;
	}
	public Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	public Set<SamplingPojo> getChildren() {
		return children;
	}
	public void setChildren(Set<SamplingPojo> children) {
		this.children = children;
	}
	public boolean isWeighingRequired() {
		return weighingRequired;
	}
	public void setWeighingRequired(boolean weighingRequired) {
		this.weighingRequired = weighingRequired;
	}
	public boolean isCommentsRequired() {
		return commentsRequired;
	}
	public void setCommentsRequired(boolean commentsRequired) {
		this.commentsRequired = commentsRequired;
	}
	public boolean isLengthRequired() {
		return lengthRequired;
	}
	public void setLengthRequired(boolean lengthRequired) {
		this.lengthRequired = lengthRequired;
	}
	public String getSampleName() {
		return sampleName;
	}
	public void setSampleName(String sampleName) {
		this.sampleName = sampleName;
	}
	public String getContainerType() {
		return containerType;
	}
	public void setContainerType(String containerType) {
		this.containerType = containerType;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public Integer getBlocNo() {
		return blocNo;
	}
	public void setBlocNo(Integer blocNo) {
		this.blocNo = blocNo;
	}
	public MeasurementPojo[] getMeasurements() {
		return measurements;
	}	
	public void setMeasurements(MeasurementPojo[] measurements) {
		this.measurements = measurements;
	}
	
	@Override
	public String toString() {
		return "[SamplingPojo:" + id + "/" + sampleName + " " + (metadata.size()>0?metadata.values():"") + (children.size()>0? "{" + children + "}": "") + "]";
	}
}
