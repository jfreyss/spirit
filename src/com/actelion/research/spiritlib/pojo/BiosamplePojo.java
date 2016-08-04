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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain POJO representing a Biosample, used to import/export data
 *  
 * @author freyssj
 */
public class BiosamplePojo implements Serializable {

	private int id;
	private String sampleId;
	private String sampleName;
	private String parentSampleId;
	private String topSampleId;

	private boolean attached;
	private String studyId;
	private String studyPhase;
	private String studyGroup;
	private int studySubGroup;
	private int attachedSamplingId;
	
	private String biotype;
	private String containerId;
	private String containerType;
	private String fullLocation;
	
	private Map<String, String> metadata = new LinkedHashMap<>();
	
	private Date updDate;
	private String updUser;
	private Date creDate;
	private String creUser;
	
	public BiosamplePojo() {
	}	
	public BiosamplePojo(int id) {
		this.id = id;
	}
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
	
	public Date getUpdDate() {
		return updDate;
	}
	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
	}
	public String getUpdUser() {
		return updUser;
	}
	public void setUpdUser(String updUser) {
		this.updUser = updUser;
	}
	public String getSampleName() {
		return sampleName;
	}
	public void setSampleName(String sampleName) {
		this.sampleName = sampleName;
	}
	public String getParentSampleId() {
		return parentSampleId;
	}
	public void setParentSampleId(String parentId) {
		this.parentSampleId = parentId;
	}
	public String getTopId() {
		return topSampleId;
	}
	public void setTopSampleId(String topId) {
		this.topSampleId = topId;
	}
	public String getStudyId() {
		return studyId;
	}
	public void setStudyId(String studyId) {
		this.studyId = studyId;
	}
	public String getStudyPhase() {
		return studyPhase;
	}
	public void setStudyPhase(String phase) {
		this.studyPhase = phase;
	}
	public String getStudyGroup() {
		return studyGroup;
	}
	public void setStudyGroup(String group) {
		this.studyGroup = group;
	}
	public int getStudySubGroup() {
		return studySubGroup;
	}
	public void setStudySubGroup(int subGroup) {
		this.studySubGroup = subGroup;
	}
	public String getContainerId() {
		return containerId;
	}
	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}
	public String getContainerType() {
		return containerType;
	}
	public void setContainerType(String containerType) {
		this.containerType = containerType;
	}
	public String getFullLocation() {
		return fullLocation;
	}
	public void setFullLocation(String fullLocation) {
		this.fullLocation = fullLocation;
	}
	public Date getCreDate() {
		return creDate;
	}
	public void setCreDate(Date creDate) {
		this.creDate = creDate;
	}
	public String getCreUser() {
		return creUser;
	}
	public void setCreUser(String creUser) {
		this.creUser = creUser;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	public String getSampleId() {
		return sampleId;
	}
	public void setSampleId(String sampleId) {
		this.sampleId = sampleId;
	}
	public void setAttached(boolean attached) {
		this.attached = attached;
	}
	public boolean isAttached() {
		return attached;
	}
	public int getAttachedSamplingId() {
		return attachedSamplingId;
	}
	public void setAttachedSamplingId(int attachedSamplingId) {
		this.attachedSamplingId = attachedSamplingId;
	}
	
	@Override
	public String toString() {
		return "[BiosamplePojo:"+id+":"+sampleId+"-"+sampleName+"]";
	}
	
}
