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

/**
 * The LocationQuery is used to perform queries on Locations
 *
 * @author Joel Freyss
 */
public class LocationQuery implements Serializable {

	private String studyId;
	private String name;
	private LocationType locationType;
	private EmployeeGroup employeeGroup;
	private Biotype biotype;
	private Boolean onlyOccupied;
	private boolean filterAdminLocation;

	public LocationQuery() {}

	/**
	 * Query by location Name
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Query by location Name
	 * @param name
	 */
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
		return getOnlyOccupied()==null && (getStudyId()==null || getStudyId().length()==0)
				&& getEmployeeGroup()==null && getBiotype()==null
				&& getLocationType()==null && (getName()==null || getName().length()==0);
	}

	/**
	 * Query location restristed to the given group
	 * @param employeeGroup the employeeGroup to set
	 */
	public void setEmployeeGroup(EmployeeGroup employeeGroup) {
		this.employeeGroup = employeeGroup;
	}

	/**
	 * Query location restristed to the given group
	 * @return the employeeGroup
	 */
	public EmployeeGroup getEmployeeGroup() {
		return employeeGroup;
	}

	/**
	 * Query location containing the give biotype
	 * @return the biotype
	 */
	public Biotype getBiotype() {
		return biotype;
	}

	/**
	 * Query location containing the give biotype
	 * @param biotype the biotype to set
	 */
	public void setBiotype(Biotype biotype) {
		this.biotype = biotype;
	}

	/**
	 * Do we query only occupied boxes (true), empty boxes (false) or all (null)
	 * @param onlyOccupied
	 */
	public void setOnlyOccupied(Boolean onlyOccupied) {
		this.onlyOccupied = onlyOccupied;
	}

	/**
	 * Do we query only occupied boxes (true), empty boxes (false) or all (null)
	 * @return
	 */
	public Boolean getOnlyOccupied() {
		return onlyOccupied;
	}

	/**
	 * Do we query only pure location (building, freezer)
	 * @return
	 */
	public boolean isFilterAdminLocation() {
		return filterAdminLocation;
	}
	/**
	 * Set to true to return only pure location (building, freezer)
	 * @param filterAdminLocation
	 */
	public void setFilterAdminLocation(boolean filterAdminLocation) {
		this.filterAdminLocation = filterAdminLocation;
	}
}
