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
import java.util.Set;

public class NamedSamplingPojo implements Serializable {
	
	private int id = 0;
	private String name;
	private boolean necropsy;
	private Set<SamplingPojo> samplings = new HashSet<>();
	
	
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
	public boolean isNecropsy() {
		return necropsy;
	}
	public void setNecropsy(boolean necropsy) {
		this.necropsy = necropsy;
	}
	public Set<SamplingPojo> getSamplings() {
		return samplings;
	}
	public void setSamplings(Set<SamplingPojo> samplings) {
		this.samplings = samplings;
	}
	@Override
	public String toString() {
		return "[NamedSampling: " + id + "/" + name + (samplings.size()>0?" {" + samplings + "}":"") + "]";
	} 
	
}
