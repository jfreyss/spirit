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

package com.actelion.research.spiritcore.business.study;

public class StudyQuery {
	private String studyIds = "";
	private String localIds = "";
	private String keywords = "";
	private String state = "";
	private String user = "";
	private String updDays = "";
	private String creDays = "";
	
	private int recentStartDays = -1;

	public StudyQuery() {
		
	}
	public static StudyQuery createForStudyIds(String studyIds) {
		StudyQuery q = new StudyQuery();
		q.setStudyIds(studyIds);
		return q;
	}
	public static StudyQuery createForLocalId(String localIds) {
		StudyQuery q = new StudyQuery();
		q.setLocalIds(localIds);
		return q;
	}
	public static StudyQuery createForState(String state) {
		StudyQuery q = new StudyQuery();
		q.setState(state);
		return q;
	}
		
	public String getStudyIds() {
		return studyIds;
	}
	public void setStudyIds(String studyIds) {
		this.studyIds = studyIds;
	}
	public String getLocalIds() {
		return localIds;
	}
	public void setLocalIds(String localIds) {
		this.localIds = localIds;
	}
	public String getKeywords() {
		return keywords;
	}
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}	
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	public void copyFrom(StudyQuery query) {
		this.studyIds = query.studyIds;
		this.keywords = query.keywords;
		this.user = query.user;
		this.state = query.state;
		this.updDays = query.updDays;
		this.creDays = query.creDays;
		this.recentStartDays = query.recentStartDays;			
	}
	

//	public boolean isEmpty() {
//		return (getStudyIds()==null || getStudyIds().length()==0) && 
//				(getKeywords()==null || getKeywords().length()==0) &&
//				(getUpdDays()==null || getUpdDays().length()==0) &&
//				(getCreDays()==null || getCreDays().length()==0) &&
//				(getUser()==null || getUser().length()==0) && 
//				(getClinicalStatus()==null);
//	}
	
	public String getUpdDays() {
		return updDays;
	}
	public void setUpdDays(String updDays) {
		this.updDays = updDays;
	}
	
	public String getCreDays() {
		return creDays;
	}
	public void setCreDays(String creDays) {
		this.creDays = creDays;
	}
	
	public void setCreDays(int days) {
		this.creDays = days<=0? null: days+" days";
	}
	
	public void setUpdDays(int days) {
		this.updDays = days<=0? null: days+" days";
	}
	
	
	public int getRecentStartDays() {
		return recentStartDays;
	}
	public void setRecentStartDays(int recentStartDays) {
		this.recentStartDays = recentStartDays;
	}
	
	
}
