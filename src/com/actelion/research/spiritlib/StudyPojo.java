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

package com.actelion.research.spiritlib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * POJO reprensenting a study.
 *
 *
 * @author freyssj
 *
 */
public class StudyPojo implements Serializable {

	private int id;
	private String studyId;
	private String localId;
	private String title;

	private String state;
	private String adminUsers = "";
	private String expertUsers = "";
	private String blindAllUsers = "";
	private String blindDetailsUsers = "";

	private String notes = "";
	private Map<String, String> metadata;

	private String updUser = "";
	private String creUser = "";

	private Date updDate = new Date();
	private Date creDate = new Date();

	private List<GroupPojo> groups = new ArrayList<>();
	private List<PhasePojo> phases = new ArrayList<>();

	private List<NamedTreatmentPojo> namedTreatments = new ArrayList<>();
	private List<NamedSamplingPojo> namedSamplings = new ArrayList<>();
	private List<StudyActionPojo> studyActions = new ArrayList<>();

	private Date day1;

	private boolean synchronizeSamples;
	private String phaseFormat;


	//TODO measurements

	//TODO docs
	//	private Set<Document> documents = new HashSet<>();

	private Set<String> attachedSampleIds = new HashSet<>();

	public StudyPojo() {

	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public String getStudyId() {
		return studyId;
	}

	public void setStudyId(String studyId) {
		this.studyId = studyId;
	}

	public String getLocalId() {
		return localId;
	}

	public void setLocalId(String localId) {
		this.localId = localId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getAdminUsers() {
		return adminUsers;
	}

	public void setAdminUsers(String adminUsers) {
		this.adminUsers = adminUsers;
	}

	public String getExpertUsers() {
		return expertUsers;
	}

	public void setExpertUsers(String expertUsers) {
		this.expertUsers = expertUsers;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public String getUpdUser() {
		return updUser;
	}

	public void setUpdUser(String updUser) {
		this.updUser = updUser;
	}

	public String getCreUser() {
		return creUser;
	}

	public void setCreUser(String creUser) {
		this.creUser = creUser;
	}

	public Date getUpdDate() {
		return updDate;
	}

	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
	}

	public Date getCreDate() {
		return creDate;
	}

	public void setCreDate(Date creDate) {
		this.creDate = creDate;
	}

	public List<GroupPojo> getGroups() {
		return groups;
	}

	public void setGroups(List<GroupPojo> groups) {
		this.groups = groups;
	}

	public List<PhasePojo> getPhases() {
		return phases;
	}

	public void setPhases(List<PhasePojo> phases) {
		this.phases = phases;
	}

	public List<NamedTreatmentPojo> getNamedTreatments() {
		return namedTreatments;
	}

	public void setNamedTreatments(List<NamedTreatmentPojo> namedTreatments) {
		this.namedTreatments = namedTreatments;
	}

	public List<NamedSamplingPojo> getNamedSamplings() {
		return namedSamplings;
	}

	public void setNamedSamplings(List<NamedSamplingPojo> namedSamplings) {
		this.namedSamplings = namedSamplings;
	}

	public List<StudyActionPojo> getStudyActions() {
		return studyActions;
	}

	public void setStudyActions(List<StudyActionPojo> studyActions) {
		this.studyActions = studyActions;
	}

	public Date getDay1() {
		return day1;
	}

	public void setDay1(Date day1) {
		this.day1 = day1;
	}

	public boolean isSynchronizeSamples() {
		return synchronizeSamples;
	}

	public void setSynchronizeSamples(boolean synchronizeSamples) {
		this.synchronizeSamples = synchronizeSamples;
	}

	public String getPhaseFormat() {
		return phaseFormat;
	}

	public void setPhaseFormat(String phaseFormat) {
		this.phaseFormat = phaseFormat;
	}

	public Set<String> getAttachedSampleIds() {
		return attachedSampleIds;
	}

	public void setAttachedSampleIds(Set<String> attachedSampleIds) {
		this.attachedSampleIds = attachedSampleIds;
	}

	public String getBlindAllUsers() {
		return blindAllUsers;
	}

	public void setBlindAllUsers(String blindAllUsers) {
		this.blindAllUsers = blindAllUsers;
	}

	public String getBlindDetailsUsers() {
		return blindDetailsUsers;
	}

	public void setBlindDetailsUsers(String blindDetailsUsers) {
		this.blindDetailsUsers = blindDetailsUsers;
	}

	@Override
	public String toString() {
		return "[Study: " +studyId+"]";
	}


}
