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
import java.util.Date;

/**
 * Plain POJO representing a Location, used to import/export data
 *  
 * @author freyssj
 */
public class LocationPojo implements Serializable {
	
	private int id;
	
	private String fullName;
	private String description;
	private String locationType;
	private String labeling;
	private int rows;
	private int cols;
	
	private Date updDate;
	private String updUser;
	private Date creDate;
	private String creUser;
	
	
	public LocationPojo() {
	}
	public LocationPojo(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		if(fullName==null) return null;
		int index = fullName.lastIndexOf('/');
		return index>=0?fullName.substring(index+1): fullName;
	}
	public String getParent() {
		if(fullName==null) return null;
		int index = fullName.lastIndexOf('/');
		return index>=0?fullName.substring(0, index): null;
	}
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLocationType() {
		return locationType;
	}
	public void setLocationType(String locationType) {
		this.locationType = locationType;
	}
	public String getLabeling() {
		return labeling;
	}
	public void setLabeling(String labeling) {
		this.labeling = labeling;
	}
	public int getRows() {
		return rows;
	}
	public void setRows(int rows) {
		this.rows = rows;
	}
	public int getCols() {
		return cols;
	}
	public void setCols(int cols) {
		this.cols = cols;
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

	
	
	@Override
	public String toString() {
		return "[BiosamplePOJO:"+id+":"+fullName+"]";
	}
	

}
