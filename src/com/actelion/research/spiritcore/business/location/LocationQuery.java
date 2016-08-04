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

package com.actelion.research.spiritcore.business.location;

import java.io.Serializable;

import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;


public class LocationQuery implements Serializable {
	
	private String studyId;
	private String name;
	private LocationType locationType;
	private EmployeeGroup employeeGroup;
	private Biotype biotype;
	private Boolean onlyOccupied;
	
	public LocationQuery() {}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param study the study to set
	 */
	public void setStudyId(String studyId) {
		this.studyId = studyId;
	}

	/**
	 * @return the study
	 */
	public String getStudyId() {
		return studyId;
	}
	
	/**
	 * @param locationType the locationType to set
	 */
	public void setLocationType(LocationType locationType) {
		this.locationType = locationType;
	}

	/**
	 * @return the locationType
	 */
	public LocationType getLocationType() {
		return locationType;
	}
		
	public boolean isEmpty() {
		return onlyOccupied==null && getStudyId()==null && getEmployeeGroup()==null && getBiotype()==null && /*getContainerType()==null && */ getLocationType()==null && (getName()==null || getName().length()==0); 
	}

	/**
	 * @param employeeGroup the employeeGroup to set
	 */
	public void setEmployeeGroup(EmployeeGroup employeeGroup) {
		this.employeeGroup = employeeGroup;
	}

	/**
	 * @return the employeeGroup
	 */
	public EmployeeGroup getEmployeeGroup() {
		return employeeGroup;
	}

	/**
	 * @return the biotype
	 */
	public Biotype getBiotype() {
		return biotype;
	}

	/**
	 * @param biotype the biotype to set
	 */
	public void setBiotype(Biotype biotype) {
		this.biotype = biotype;
	}
	
	public void setOnlyOccupied(Boolean onlyOccupied) {
		this.onlyOccupied = onlyOccupied;
	}
	public Boolean getOnlyOccupied() {
		return onlyOccupied;
	}
}
