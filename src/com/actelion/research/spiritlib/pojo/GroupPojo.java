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
import java.util.Arrays;

public class GroupPojo implements Serializable {

	private int id = 0;
	private String name = "";
	private Integer colorRgb = null;
	
	/**
	 * Uses when this group comes from another group, either when:
	 * - a "sick" group is splitted into "sick treated" and "sick untreated"
	 * - samples are created from a group and assigned to this new subgroup  
	 */
	private String fromGroup;
	
	/**
	 * Used when this group comes from another group, and when the animal is splitted into samples.
	 * For example, at d7, we treat 4 fragments of skin and extract cells over several days
	 */
	private SamplingPojo dividingSampling;

	private String fromPhase;
	
	private int[] subgroupSizes;	

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getColorRgb() {
		return colorRgb;
	}

	public void setColorRgb(Integer colorRgb) {
		this.colorRgb = colorRgb;
	}

	public String getFromGroup() {
		return fromGroup;
	}

	public void setFromGroup(String fromGroup) {
		this.fromGroup = fromGroup;
	}

	public SamplingPojo getDividingSampling() {
		return dividingSampling;
	}

	public void setDividingSampling(SamplingPojo dividingSampling) {
		this.dividingSampling = dividingSampling;
	}

	public String getFromPhase() {
		return fromPhase;
	}

	public void setFromPhase(String fromPhase) {
		this.fromPhase = fromPhase;
	}

	public int[] getSubgroupSizes() {
		return subgroupSizes;
	}

	/**
	 * Set the sizes of the subgroups (comma separated)
	 * @param subgroupSizes
	 */
	public void setSubgroupSizes(int[] subgroupSizes) {
		this.subgroupSizes = subgroupSizes;
	}
	
	@Override
	public String toString() {
		return "[GroupPOJO:"+id+":"+name+"'"+Arrays.toString(subgroupSizes)+"]";
	}
}
