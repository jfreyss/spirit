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

package com.actelion.research.spiritcore.business.audit;

import java.util.Date;

public class RevisionQuery {
	private String userIdFilter;
	private int sidFilter;
	private String studyIdFilter;
	private int revId;
	private Date fromDate;
	private Date toDate;
	private boolean studies = true;
	private boolean samples = true;
	private boolean results = true;
	private boolean locations = true;
	private boolean admin = true;

	public RevisionQuery() {
	}

	public RevisionQuery(String userFilter, String studyIdFilter, Date fromDate, Date toDate, boolean studies, boolean samples, boolean results, boolean locations, boolean admin) {
		super();
		this.userIdFilter = userFilter;
		this.studyIdFilter = studyIdFilter;
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.studies = studies;
		this.samples = samples;
		this.results = results;
		this.locations = locations;
		this.admin = admin;
	}

	public int getRevId() {
		return revId;
	}
	public void setRevId(int revId) {
		this.revId = revId;
	}
	public String getUserIdFilter() {
		return userIdFilter;
	}
	public void setUserIdFilter(String userIdFilter) {
		this.userIdFilter = userIdFilter;
	}
	public int getSidFilter() {
		return sidFilter;
	}
	public void setSidFilter(int sidFilter) {
		this.sidFilter = sidFilter;
	}
	public String getStudyIdFilter() {
		return studyIdFilter;
	}
	public void setStudyIdFilter(String studyIdFilter) {
		this.studyIdFilter = studyIdFilter;
	}
	public Date getFromDate() {
		return fromDate;
	}
	/**
	 * FromDate inclusive
	 */
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}
	public Date getToDate() {
		return toDate;
	}
	/**
	 * ToDate exclusive
	 * @param toDate
	 */
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}
	public boolean isStudies() {
		return studies;
	}
	public void setStudies(boolean studies) {
		this.studies = studies;
	}
	public boolean isSamples() {
		return samples;
	}
	public void setSamples(boolean samples) {
		this.samples = samples;
	}
	public boolean isResults() {
		return results;
	}
	public void setResults(boolean results) {
		this.results = results;
	}
	public boolean isLocations() {
		return locations;
	}
	public void setLocations(boolean locations) {
		this.locations = locations;
	}
	public boolean isAdmin() {
		return admin;
	}
	public void setAdmin(boolean admin) {
		this.admin = admin;
	}
}