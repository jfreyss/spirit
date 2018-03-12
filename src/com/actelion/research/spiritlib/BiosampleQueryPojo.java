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

package com.actelion.research.spiritlib;

import java.io.Serializable;

public class BiosampleQueryPojo implements Serializable {
	
	private String containerIds;
	private String sampleIds;
	private String studyIds;
	private String topSampleIds;
	private String parentSampleIds;
	private String keywords;
	
	public String getContainerIds() {
		return containerIds;
	}
	public void setContainerIds(String containerIds) {
		this.containerIds = containerIds;
	}
	public String getSampleIds() {
		return sampleIds;
	}
	public void setSampleIds(String sampleIds) {
		this.sampleIds = sampleIds;
	}
	public String getStudyIds() {
		return studyIds;
	}
	public void setStudyIds(String studyIds) {
		this.studyIds = studyIds;
	}
	public String getTopSampleIds() {
		return topSampleIds;
	}
	public void setTopSampleIds(String topSampleIds) {
		this.topSampleIds = topSampleIds;
	}
	public String getParentSampleIds() {
		return parentSampleIds;
	}
	public void setParentSampleIds(String parentSampleIds) {
		this.parentSampleIds = parentSampleIds;
	}
	public String getKeywords() {
		return keywords;
	}
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	
	
}