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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecentChangePojo implements Serializable {
	private StudyPojo study;
	private Date date;
	private String userId;
	private List<BiosamplePojo> biosamples = new ArrayList<BiosamplePojo>();
	private List<ResultPojo> results = new ArrayList<ResultPojo>();
	
	public void setStudy(StudyPojo study) {
		this.study = study;
	}
	
	public StudyPojo getStudy() {
		return study;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<BiosamplePojo> getBiosamples() {
		return biosamples;
	}

	public void setBiosamples(List<BiosamplePojo> biosamples) {
		this.biosamples = biosamples;
	}

	public List<ResultPojo> getResults() {
		return results;
	}

	public void setResults(List<ResultPojo> results) {
		this.results = results;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		return study+" - " +date+" - "+getBiosamples().size() + "/" + getResults().size();
	}
}