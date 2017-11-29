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

package com.actelion.research.spiritlib;

import java.io.Serializable;

public class NamedTreatmentPojo implements Serializable {

	private int id = 0;
	private String name;
	private Integer colorRgb = 0;

	private String compoundName1;	
	private Double dose1;
	private String unit1;
	private String application1;

	private String compoundName2;	
	private Double dose2;
	private String unit2;
	private String application2;
	
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
	public String getCompoundName1() {
		return compoundName1;
	}
	public void setCompoundName1(String compoundName1) {
		this.compoundName1 = compoundName1;
	}
	public Double getDose1() {
		return dose1;
	}
	public void setDose1(Double dose1) {
		this.dose1 = dose1;
	}
	public String getUnit1() {
		return unit1;
	}
	public void setUnit1(String unit1) {
		this.unit1 = unit1;
	}
	public String getApplication1() {
		return application1;
	}
	public void setApplication1(String application1) {
		this.application1 = application1;
	}
	public String getCompoundName2() {
		return compoundName2;
	}
	public void setCompoundName2(String compoundName2) {
		this.compoundName2 = compoundName2;
	}
	public Double getDose2() {
		return dose2;
	}
	public void setDose2(Double dose2) {
		this.dose2 = dose2;
	}
	public String getUnit2() {
		return unit2;
	}
	public void setUnit2(String unit2) {
		this.unit2 = unit2;
	}
	public String getApplication2() {
		return application2;
	}
	public void setApplication2(String application2) {
		this.application2 = application2;
	}

	
}
