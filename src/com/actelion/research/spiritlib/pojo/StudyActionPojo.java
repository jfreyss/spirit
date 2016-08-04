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

public class StudyActionPojo implements Serializable {

	private String phase;
	private String group;
	private int subGroup = 0; 
	private String namedTreatment;	
	private String namedSampling1;
	private String namedSampling2;

	private boolean measureFood = false;
	private boolean measureWater = false;
	private boolean measureWeight = false;
	private MeasurementPojo[] measurements;
	
	private String label;

	public String getPhase() {
		return phase;
	}

	public void setPhase(String phase) {
		this.phase = phase;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public int getSubGroup() {
		return subGroup;
	}

	public void setSubGroup(int subGroup) {
		this.subGroup = subGroup;
	}

	public String getNamedTreatment() {
		return namedTreatment;
	}

	public void setNamedTreatment(String namedTreatment) {
		this.namedTreatment = namedTreatment;
	}

	public String getNamedSampling1() {
		return namedSampling1;
	}

	public void setNamedSampling1(String namedSampling1) {
		this.namedSampling1 = namedSampling1;
	}

	public String getNamedSampling2() {
		return namedSampling2;
	}

	public void setNamedSampling2(String namedSampling2) {
		this.namedSampling2 = namedSampling2;
	}

	public boolean isMeasureFood() {
		return measureFood;
	}

	public void setMeasureFood(boolean measureFood) {
		this.measureFood = measureFood;
	}

	public boolean isMeasureWater() {
		return measureWater;
	}

	public void setMeasureWater(boolean measureWater) {
		this.measureWater = measureWater;
	}

	public boolean isMeasureWeight() {
		return measureWeight;
	}

	public void setMeasureWeight(boolean measureWeight) {
		this.measureWeight = measureWeight;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public MeasurementPojo[] getMeasurements() {
		return measurements;
	}
	
	public void setMeasurements(MeasurementPojo[] measurements) {
		this.measurements = measurements;
	}
		
}
