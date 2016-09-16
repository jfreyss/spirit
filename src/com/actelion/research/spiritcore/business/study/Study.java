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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SortNatural;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RevisionNumber;

import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Document.DocumentType;
import com.actelion.research.spiritcore.business.IEntity;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.CompareUtils;

/**
 * 
 * @author freyssj
 *
 */
@Entity
@Audited
@Table(name="study", indexes = {
	@Index(name="study_id_index", columnList = "studyId"), @Index(name="study_ivv_index", columnList = "ivv"),
})
@SequenceGenerator(name="study_sequence", sequenceName="study_sequence", allocationSize=1)
@BatchSize(size=8)
public class Study implements Serializable, IEntity, Comparable<Study> {

	@Id
	@RevisionNumber	
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="study_sequence")
	private int id = 0;
	
	/**
	 * Unique Id, system wise, given by the system
	 */
	@Column(name="studyId", nullable=false, unique=true)
	private String studyId = "";

	/**
	 * Id given by the user, may not be unique
	 */
	@Column(name="ivv", nullable=true)
	private String ivv;
	
	@Column(name="description", length=256)
	private String title = "";
		
	@Column(name="write_users", length=512)
	private String adminUsers = "";

	@Column(name="read_users", length=512)
	private String expertUsers = "";
	
	@Column(name="blind_users", length=512)
	private String blindUsers = "";
	
	@Column(name="comments", length=2048)
	private String notes = "";
	
	@Column(name="metadata", length=4000)
	private String serializedMetadata;
	
	private String updUser = "";
	private String creUser = "";

	@Temporal(TemporalType.TIMESTAMP)
	private Date updDate = new Date();
		
	@Temporal(TemporalType.TIMESTAMP)
	private Date creDate = new Date();
	
	@OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval=true, mappedBy="study")
	@SortNatural
	@BatchSize(size=4)
	private Set<Group> groups = new TreeSet<>();
	
	@OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval=true, mappedBy="study")	
	@SortNatural
	@BatchSize(size=4)
	private Set<Phase> phases = new TreeSet<>();

	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval=true, mappedBy="study")
	@SortNatural
	@BatchSize(size=4)
	private Set<NamedTreatment> namedTreatments = new TreeSet<>();
	
	/**
	 * The Named Sampling shows how the samples can be created.
	 * It is saved as a separate object (so cascade=refresh)
	 */
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="study")
	@SortNatural
	@BatchSize(size=4)
	private Set<NamedSampling> namedSamplings = new TreeSet<>();
		
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval=true, mappedBy="study")	
//	@BatchSize(size=64)
	private Set<StudyAction> actions = new HashSet<>();
	
	/**
	 * Starting date (date for d1, or the closest to d1)
	 */
	@Column(name="startingdate", nullable=true)
	private Date startingDate = null;
	
	@Column(name="status", nullable=true, length=24)
	private String state = null;
	
	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY)
	@JoinColumn(name="department_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@BatchSize(size=8)
	private EmployeeGroup employeeGroup1 = null;
	
	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY)
	@JoinColumn(name="department2_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@BatchSize(size=8)
	private EmployeeGroup employeeGroup2 = null;
	
	@Column(name="synchrosamples")
	private Boolean synchronizeSamples = Boolean.TRUE;
	
	
	@Enumerated(EnumType.STRING)
	@Column(name="phaseFormat", nullable=true)
	private PhaseFormat phaseFormat = PhaseFormat.DAY_MINUTES;
	
	
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval=true)
	@Audited(targetAuditMode=RelationTargetAuditMode.AUDITED)	
	private Set<Document> documents = new HashSet<>();

	/**
	 * The animals that are directly attached to one of the group or to the reserve
	 * NB: The relation is still held by the biosample, so to unset a group use biosample.setAttachedStudy(null)
	 *  
	 */
	@OneToMany(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY, mappedBy="attachedStudy")
	@Audited(targetAuditMode=RelationTargetAuditMode.NOT_AUDITED)	
	@BatchSize(size=64)
	private Set<Biosample> attachedBiosamples = new TreeSet<>();
	
	/** Helpful function for faster access groupId_groupName_subgroup->phaseName->action */
	private transient Map<Pair<Group, Integer>, Map<Phase, StudyAction>> mapGroupPhase2Action = null;	
	private transient Map<String, String> metadataMap = null;
	private transient Set<String> adminUsersSet;
	private transient Set<String> expertUsersSet;
	private transient Set<String> blindAllUsersSet;
	private transient Set<String> blindDetailsUsersSet;
	private transient Map<Pair<Group, Integer>, Phase> phaseFirstTreatments = new HashMap<>();

	
	/**
	 * Basic constructor
	 */
	public Study() {
		
	}
	
	@Override
	public String toString() {		
		return getIvvOrStudyId(); 
	}
	
	
	@Override
	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getIvv() {
		return ivv;
	}
	
	/**
	 * Returns the IVV or the StudyId
	 * @return
	 */
	public String getStudyIdIvv() {
		return getStudyId() + (getIvv()!=null && getIvv().length()>0? "(" + getIvv() + ")": "") ;
	}

	/**
	 * Return the IVV number or the studyId if the IVV is empty.
	 * This function never returns null
	 * @return
	 */
	public String getIvvOrStudyId() {
		return getIvv()!=null && getIvv().length()>0? getIvv() : getStudyId();
	}


	public String getAdminUsers() {
		return adminUsers;
	}

	public String getNotes() {
		return notes;
	}

	public String getUpdUser() {
		return updUser;
	}

	public Date getUpdDate() {
		return updDate;
	}

	public Date getCreDate() {
		return creDate;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setTitle(String description) {
		this.title = description;
	}

	public void setIvv(String ivv) {
		this.ivv = ivv;
	}

	public void setAdminUsers(String writeUsers) {
		adminUsersSet = null;
		this.adminUsers = writeUsers;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public void setUpdUser(String updUser) {
		this.updUser = updUser;
	}

	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
	}

	public void setCreDate(Date creDate) {
		this.creDate = creDate;
	}

	public Set<Group> getGroups() {
		return groups;
	}	

	public List<Group> getGroupsHierarchical() {
		List<Group> res = new ArrayList<>();
		for (Group group : getGroups()) {
			if (group.getFromGroup() == null) {
				res.addAll(getGroupsHierarchicalRec(group));
			}
		}
		return res;
	}

	private List<Group> getGroupsHierarchicalRec(Group group) {
		List<Group> res = new ArrayList<>();
		res.add(group);
		List<Group> childrenGroups = new ArrayList<>();
		childrenGroups.addAll(group.getDividingGroups());
		childrenGroups.addAll(group.getToGroups());
		Collections.sort(childrenGroups, new Comparator<Group>() {
			@Override
			public int compare(Group o1, Group o2) {
				return -CompareUtils.compare(o1.getFromPhase(), o2.getFromPhase());
			}
		});
		for (Group child : childrenGroups) {
			res.addAll(getGroupsHierarchicalRec(child));
		}
		return res;
	}
	
	/**
	 * Returns all the groups that are splitted. ie. all G where exists(group G2 where G2.fromGroup = G)
	 * @return
	 */
	public Set<Group> getGroupsWithSplitting() {
		Set<Group> res = new LinkedHashSet<>();
		for (Group group : getGroups()) {
			if(group.getFromGroup()!=null) res.add(group.getFromGroup());
		}
		return res;
	}
	
	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}
	
	public Set<Phase> getPhases() {
		return phases;
	}	
	
	public void setPhases(Set<Phase> phases) {
		this.phases = phases;
	}
	
	public Phase getPhase(int phaseId) {
		for (Phase p : getPhases()) {
			if(p.getId()==phaseId) {
				return p;
			}	
		}
		return null;		
	}
	
	/**
	 * Return the first phase matching the date
	 * @param date
	 * @return
	 */
	public Phase getPhase(Date date) {
		if(getPhaseFormat()==PhaseFormat.NUMBER) return null;
		if(getDayOneDate()==null) return null;
		for (Phase p : getPhases()) {
			if(Phase.isSameDay(p.getAbsoluteDate(), date)) {
				return p;
			}	
		}
		return null;		
	}

	public Phase getPhase(String phaseName) {
		for (Phase p : getPhases()) {
			if(p.getName().equalsIgnoreCase(phaseName)) {
				return p;
			}	
			if(p.getShortName().equalsIgnoreCase(phaseName)) {
				return p;
			}	
		}
		return null;
	}
	
		
	/**
	 * Return the first sampling from this study, that matching the sampling.namedSampling.name and the sampling.detailslong
	 */
	public Sampling getSampling(String namedSamplingName, String samplingDetailsLong) {
		List<Sampling> samplings = getSamplings(namedSamplingName, samplingDetailsLong);
		return samplings.size()>0? samplings.get(0): null;
	}
	
	/**
	 * Return all samplings from this study, matching the sampling.namedSampling.name and the sampling.detailslong
	 */
	public List<Sampling> getSamplings(String namedSamplingName, String samplingDetailsLong) {
		List<Sampling> res = new ArrayList<>();
		for (NamedSampling p : getNamedSamplings()) {
			if(p.getName().equalsIgnoreCase(namedSamplingName)) {
				for (Sampling s : p.getAllSamplings()) {
					if(s.getDetailsLong().equalsIgnoreCase(samplingDetailsLong)) {
						res.add(s);
					}
				}				
			}	
		}
		return res;
	}
	
	/**
	 * Return one sampling from this study, that matches the sampling.id
	 */
	public Sampling getSampling(int id) {
		for (NamedSampling p : getNamedSamplings()) {
			for (Sampling s : p.getAllSamplings()) {
				if(s.getId()==id) {
					return s;
				}
			}			
		}
		return null;
	}
	
	/**
	 * Returns what could be the initial phase (the phase closest to d0)
	 * @return
	 */
	public Phase getReferencePhase() {
		return getReferencePhase(0);		
	}
	
	/**
	 * Returns what could be the initial phase (the phase closest to the given day)
	 * @return
	 */
	public Phase getReferencePhase(int d) {
		Phase sel = null;
		int selDiff = 24*60*2;
		for (Phase p : phases) {
			int diff = Math.abs(p.getDays()-d)*24*60 + Math.abs(p.getHours())*60 + Math.abs(p.getMinutes());  
			if(diff<selDiff) {
				sel = p;
				selDiff = diff;
			}
		}
		return sel;
	}

	@Override
	public int hashCode() {
		return id;
	}

	public Group getGroup(String groupName) {		
		for (Group group : getGroups()) {
			if(group.getName().equals(groupName)) return group;
		}
		return null;
	}
	
	public Group getGroup(Integer id) {		
		if(id==null) return null;
		for (Group g : getGroups()) {
			if(g.getId()==id) {
				return g;
			}			
		}
		return null;
	}

	public Study duplicate() {
		Study study = clone();		
		study.setIvv(null);
		study.setAttachedBiosamples(new TreeSet<Biosample>());
		study.setNotes("Duplicated from "+getStudyId());
		return study;
	}
	
	@Override
	protected Study clone() {
		Study study = new Study();
		study.setState(getState());
		study.setDay1(getDayOneDate());
		study.setIvv(getIvv());
		study.setTitle(getTitle());
		study.setNotes(getNotes());
		study.setAdminUsers(getAdminUsers());
		study.setExpertUsers(getExpertUsers());
		study.expertUsers = this.expertUsers;
		study.setEmployeeGroups(getEmployeeGroups());
		study.setDocuments(new HashSet<Document>());
		study.setSynchronizeSamples(isSynchronizeSamples());
		study.serializedMetadata = serializedMetadata;
		
		//Clone Phases
		IdentityHashMap<Phase, Phase> phaseClones = new IdentityHashMap<>();
		study.setPhases(new TreeSet<Phase>());
		for (Phase o : getPhases()) {
			Phase oc = new Phase(o.getName());
			oc.setStudy(study);
			study.getPhases().add(oc);
			phaseClones.put(o, oc);
		}
		
		//Clone Groups
		IdentityHashMap<Group, Group> groupClones = new IdentityHashMap<>();
		study.setGroups(new TreeSet<Group>());
		for (Group o : getGroups()) {
			Group oc = new Group();
			oc.setName(o.getName());
			oc.setStudy(study);
			oc.setColorRgb(o.getColorRgb());
			oc.setSubgroupSizes(o.getSubgroupSizes());
			oc.setFromGroup(groupClones.get(o.getFromGroup()));
			oc.setFromPhase(phaseClones.get(o.getFromPhase()));
			oc.setDividingSampling(o.getDividingSampling()==null? null: o.getDividingSampling().clone());
			study.getGroups().add(oc);
			groupClones.put(o, oc);
		}

		//Clone Treatments
		IdentityHashMap<NamedTreatment, NamedTreatment> treatmentClones = new IdentityHashMap<>();
		study.setNamedTreatments(new TreeSet<NamedTreatment>());
		for (NamedTreatment o : getNamedTreatments()) {
			NamedTreatment oc = new NamedTreatment();
			oc.setStudy(study);
			oc.setName(o.getName());
			oc.setColorRgb(o.getColorRgb());
			oc.setCompoundName1(o.getCompoundName1());
			oc.setDose1(o.getDose1());
			oc.setApplication1(o.getApplication1());
			oc.setUnit1(o.getUnit1());
			oc.setCompoundName2(o.getCompoundName2());
			oc.setDose2(o.getDose2());
			oc.setUnit2(o.getUnit2());
			oc.setApplication2(o.getApplication2());
			study.getNamedTreatments().add(oc);
			treatmentClones.put(o, oc);
		}		
		

		//Clone Samplings
		IdentityHashMap<NamedSampling, NamedSampling> samplingClones = new IdentityHashMap<>();
		study.setNamedSamplings(new TreeSet<NamedSampling>());

		for (NamedSampling o : getNamedSamplings()) {
			NamedSampling oc = o.duplicate();
			oc.setStudy(study);
			study.getNamedSamplings().add(oc);
			samplingClones.put(o, oc);
		}



		study.setStudyActions(new TreeSet<StudyAction>());
		for (StudyAction o : getStudyActions()) {
			StudyAction oc = new StudyAction(o);
			oc.setStudy(study);
			oc.setGroup(groupClones.get(o.getGroup()));
			oc.setSubGroup(o.getSubGroup());
			
			oc.setPhase(phaseClones.get(o.getPhase()));
			oc.setNamedTreatment(treatmentClones.get(o.getNamedTreatment()));
			oc.setNamedSampling1(samplingClones.get(o.getNamedSampling1()));
			oc.setNamedSampling2(samplingClones.get(o.getNamedSampling2()));
			study.getStudyActions().add(oc);
			
		}		
	
		return study;
	}
		
	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(!(obj instanceof Study)) return false;
		int id2 = ((Study)obj).getId();
		return id == id2;
	}
		
	public Set<NamedTreatment> getNamedTreatments() {
		return namedTreatments;
	}
	public void setNamedTreatments(Set<NamedTreatment> namedTreatments) {
		this.namedTreatments = namedTreatments;
	}
	public Set<NamedSampling> getNamedSamplings() {
		return namedSamplings;
	}
	public void setNamedSamplings(Set<NamedSampling> namedSamplings) {
		this.namedSamplings = namedSamplings;
	}
	
	public void setStudyActions(Set<StudyAction> actions) {
		this.actions = actions;
		mapGroupPhase2Action = null;
	}
	public Set<StudyAction> getStudyActions() {
		return actions;
	}
	public void removeStudyActions(Collection<StudyAction> col) {
		actions.removeAll(col);
		mapGroupPhase2Action = null;
	}
	public void addStudyActions(Collection<StudyAction> col) {
		actions.addAll(col);
		mapGroupPhase2Action = null;
	}
	
	/**
	 * Get the actions for the given group (if group==null, returns all actions)
	 * Does not use the cache
	 * @param group
	 * @return
	 */
	public Set<StudyAction> getStudyActions(Group group) {
		Set<StudyAction> res = new HashSet<>();
		for (StudyAction action : getStudyActions()) {
			if(group==null || group.equals(action.getGroup())) {
				res.add(action);
			}
		}
		return res;	
	}
	

	/**
	 * Cache the actions for faster access.
	 */
	private void cacheGroupPhase2Action() {
		mapGroupPhase2Action = new HashMap<>();
		for (Iterator<StudyAction> iter = getStudyActions().iterator(); iter.hasNext();) {
			StudyAction action = iter.next();
			if(action.getGroup()==null || action.getPhase()==null) {
//				iter.remove();
				continue;
			}
//		for (StudyAction action : getStudyActions()) {
//			assert action.getGroup()!=null;
//			assert action.getPhase()!=null;
			
			Pair<Group, Integer> key1 = new Pair<Group, Integer>(action.getGroup(), action.getSubGroup());			
			Map<Phase, StudyAction> map = mapGroupPhase2Action.get(key1);
			if(map==null) {
				mapGroupPhase2Action.put(key1, map = new HashMap<>());
			}
			map.put(action.getPhase(), action);
		}
	}

	/**
	 * Delete the cache, to be called if the user performed some update operations directly on the collections (getGroups().add, action.setGroup) 
	 */
	public void resetCache() {
		for (Group g : getGroups()) {
			g.resetCache();
		}
		mapGroupPhase2Action = null;
	}

	/**
	 * Return the action for the given group/subgroup (cannot be null)
	 * Use the cache (the first call will initialize the cache)
	 * @param group
	 * @param subgroup
	 * @return
	 */
	public Set<StudyAction> getStudyActions(Group group, int subgroup) {
		assert group!=null;
		assert subgroup>=0;
		if(mapGroupPhase2Action==null) cacheGroupPhase2Action();

		Set<StudyAction> res = new HashSet<>();
		Pair<Group, Integer> key1 = new Pair<Group, Integer>(group, subgroup);	
		Map<Phase, StudyAction> map = mapGroupPhase2Action.get(key1);
		if(map!=null) res.addAll(map.values());
		return res;	
	}
	
	/**
	 * Return the action for the given phase (cannot be null)
	 * Use the cache (the first call will initialize the cache)
	 */
	public Set<StudyAction> getStudyActions(Phase phase) {
		assert phase!=null;
		if(mapGroupPhase2Action==null) cacheGroupPhase2Action();
		Set<StudyAction> res = new HashSet<>();
		for (Map<Phase, StudyAction> map: mapGroupPhase2Action.values()) {
			StudyAction a = map.get(phase);
			if(a!=null) res.add(a);
		}
		return res;	
	}

	
	/**
	 * Gets an Action (this function uses a cache)
	 * @return the action, or null if none was set 
	 */	
	public StudyAction getStudyAction(Group group, int subgroup, Phase phase) {		
		if(group==null || phase==null) return null;

		if(mapGroupPhase2Action==null) cacheGroupPhase2Action();
		Map<Phase, StudyAction> map = mapGroupPhase2Action.get(new Pair<>(group, subgroup));
		return map==null? null: map.get(phase);
	}
	
	/**
	 * Get the studyAction for the given animal and phase
	 * BEFORE: if the group was split, this function will NOT return the studyaction from the originating group
	 * if the group was split, this function will return the studyaction from the originating group
	 */
	public StudyAction getStudyAction(Phase phase, Biosample animal) {
		if(phase==null || animal==null || animal.getInheritedGroup()==null) {
			return null;
		}
		
		Group group = animal.getInheritedGroup();
		int subGroup = animal.getInheritedSubGroup();
		while(group.getFromPhase()!=null && group.getFromGroup()!=null && group.getFromPhase().getTime()>phase.getTime()) {
			group = group.getFromGroup();
			subGroup = 0;
		}

		return getStudyAction(group, subGroup, phase);
	}

	/**
	 * Get or Create an action (this function does not use the cache and is useful when defining a new study)
	 * @param group
	 * @param phase
	 * @return
	 */
	public StudyAction getOrCreateStudyAction(Group group, int subGroup, Phase phase) {
		mapGroupPhase2Action = null;		

		StudyAction action = getStudyAction(group, subGroup, phase);
		
		if(action==null) {		
			action = new StudyAction(this, group, subGroup, phase);			
			actions.add(action);
			mapGroupPhase2Action=null;
		}
		return action;
	}

	public Set<NamedTreatment> getNamedTreatments(Phase phase) {
		if(phase==null) throw new IllegalArgumentException("Cannot be called with a null phase");
		Set<NamedTreatment> res = new TreeSet<>();
		for (StudyAction action : getStudyActions()) {
			if(action.getPhase().equals(phase) && action.getNamedTreatment()!=null) {
				res.add(action.getNamedTreatment());
			}
		}
		return res;
	}
	public Set<NamedSampling> getNamedSamplings(Phase phase) {
		if(phase==null) throw new IllegalArgumentException("Cannot be called with a null phase");
		Set<NamedSampling> res = new TreeSet<>();		
		if(getStudyActions()==null) return res;//Initialization error?
		
		for (StudyAction action : getStudyActions()) {
			if(action.getPhase().equals(phase)) {
				if(action.getNamedSampling1()!=null) res.add(action.getNamedSampling1());
				if(action.getNamedSampling2()!=null) res.add(action.getNamedSampling2());
			}
		}
		return res;
	}
	public void setNamedTreatment(Group group, Phase phase, int subGroup, NamedTreatment nt, boolean set) {
		if(group==null || phase==null) throw new IllegalArgumentException("Cannot be called with a null group or phase");
		StudyAction action = getOrCreateStudyAction(group, subGroup, phase);
		if(set) action.setNamedTreatment(nt);
		else action.setNamedTreatment(null);
		
		phaseFirstTreatments.clear();
	}
	
	public void setNamedSampling(Group group, Phase phase, int subGroup, NamedSampling ns, boolean set) throws Exception {
		if(group==null || phase==null) throw new IllegalArgumentException("Cannot be called with a null group or phase");
		StudyAction action = getOrCreateStudyAction(group, subGroup, phase);
		
		if(ns==null) {
			action.setNamedSampling1(null);
			action.setNamedSampling2(null);
			return;
		}
		
		if(set) {			
			//Returns if it is already done
			if(ns.equals(action.getNamedSampling1()) || ns.equals(action.getNamedSampling2())) return;
			
			if(action.getNamedSampling1()==null) action.setNamedSampling1(ns);
			else if(action.getNamedSampling2()==null) action.setNamedSampling2(ns);
			else throw new Exception("You cannot apply more than 2 samplings"); 
		} else {
			if(ns.equals(action.getNamedSampling1())) action.setNamedSampling1(null);
			if(ns.equals(action.getNamedSampling2())) action.setNamedSampling2(null);
		}
		
		//move sampling2 to sampling1 if sampling1 is not set
		if(action.getNamedSampling1()==null && action.getNamedSampling2()!=null) {
			action.setNamedSampling1(action.getNamedSampling2());
			action.setNamedSampling2(null);
		}
	}
	
	/**
	 * @param creUser the creUser to set
	 */
	public void setCreUser(String creUser) {
		this.creUser = creUser;
	}
	/**
	 * @return the creUser
	 */
	public String getCreUser() {
		return creUser;
	}
	
	@Override
	public int compareTo(Study s) {
		if(s==this) return 0;
		if(s==null) return 1;
		if(getStudyId()==null && s.getStudyId()==null) return 0;
		if(getStudyId()==null) return -1;
		if(s.getStudyId()==null) return 1;
		return -getStudyId().compareTo(s.getStudyId());
	}

	
	/**
	 * @param restrictedUsers the restrictedUsers to set
	 */
	public void setExpertUsers(String expertUsers) {
		this.expertUsers = expertUsers;
		expertUsersSet = null;
	}
	/**
	 * @return the restrictedUsers
	 */
	public String getExpertUsers() {
		return expertUsers;
	}
	
	private void populateUserSets() {
		if(adminUsersSet==null) {
			adminUsersSet = new TreeSet<>(Arrays.asList(MiscUtils.split(getAdminUsers(), MiscUtils.SPLIT_SEPARATORS_WITH_SPACE)));
			expertUsersSet = new TreeSet<>(Arrays.asList(MiscUtils.split(getExpertUsers(), MiscUtils.SPLIT_SEPARATORS_WITH_SPACE)));
			
			blindAllUsersSet = new TreeSet<>();
			blindDetailsUsersSet = new TreeSet<>();
			for(String u: MiscUtils.split(blindUsers, MiscUtils.SPLIT_SEPARATORS_WITH_SPACE)) {
				if(u.startsWith("0#")) {
					//all
					blindAllUsersSet.add(u.substring(2));
				} else if(u.startsWith("1#")) {
					//group/treatments
					blindDetailsUsersSet.add(u.substring(2));				
				} else {
					blindDetailsUsersSet.add(u.indexOf('#')<0? u: u.substring(u.indexOf('#')+1));								
				}
			}			
		}
	}
	
	public Set<String> getAdminUsersAsSet() {
		populateUserSets();
		return Collections.unmodifiableSet(adminUsersSet);
	}
	
	public Set<String> getExpertUsersAsSet() {
		populateUserSets();
		return Collections.unmodifiableSet(expertUsersSet);
	}

	public Set<String> getBlindAllUsersAsSet() {
		populateUserSets();
		return Collections.unmodifiableSet(blindAllUsersSet);
	}
	public Set<String> getBlindDetailsUsersAsSet() {
		populateUserSets();
		return Collections.unmodifiableSet(blindDetailsUsersSet);
	}
	
	public void remove() {
		for(StudyAction action: new ArrayList<>(getStudyActions())) {
			action.remove();
		}
		for(Group group: new ArrayList<>(getGroups())) {
			group.remove();
		}
		for(Phase phase: new ArrayList<>(getPhases())) {
			phase.remove();
		}
		
		for (NamedTreatment n : new ArrayList<>(getNamedTreatments())) {
			n.remove();
		}
		for (NamedSampling n : new ArrayList<>(getNamedSamplings())) {
			n.remove();
		}
	}
	
	/**
	 * @param documents the documents to set
	 */
	public void setDocuments(Set<Document> documents) {
		this.documents = documents;
	}
	
	/**
	 * @return the documents
	 */
	public Set<Document> getDocuments() {
		return documents;
	}
		
	public Date getDayOneDate() {
		return startingDate;
	}
	
	public Date getFirstDate() {
		if(startingDate!=null && getPhases()!=null) {
			Integer days = getPhases().size()==0? null: getPhases().iterator().next().getDays();
			if(days==null) days = 1;
			Calendar cal = Calendar.getInstance();
			cal.setTime(startingDate);
			cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + (days-1));
			return cal.getTime();
		} else {
			return null;
		}
	}
	
	
	public Phase getFirstPhase() {
		return getPhases().size()==0? null: getPhases().iterator().next();
	}
	
	public Phase getLastPhase() {
		Phase lastPhase = null; 
		Iterator<Phase> iterator = getPhases().iterator();
		while(iterator.hasNext()) {
			Phase p = iterator.next();
			if(!iterator.hasNext()) {lastPhase=p;}	
		}
		return lastPhase;
	}
	
	public Date getLastDate() {
		if(startingDate!=null) {
			
			Integer days = getPhases().size()==0? null: getLastPhase().getDays();
			if(days==null) days = 1;
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(startingDate);
			cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + (days-1));
			return cal.getTime();
		} else {
			return null;
		}
	}
	
	/**
	 * @param day1 the startingDate to set
	 */
	public void setDay1(Date day1) {
		this.startingDate = day1;
	}
	
	public void setStartingDate(Date startingDate) {
		if(startingDate!=null) {
			Integer days = getPhases().size()==0? null: getFirstPhase().getDays();
			if(days==null) days = 1;
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(startingDate);
			cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) - (days-1));
			setDay1(cal.getTime());
		} else {
			setDay1(null);
		}
	}
	
	public String getStudyId() {
		return studyId;
	}
	
	public void setStudyId(String name) {
		this.studyId = name;
	}
	
	public String getState() {
		return state;
	}
	public void setState(String status) {
		this.state = status;
	}

	/**
	 * @param department the department to set
	 */
	public void setEmployeeGroups(List<EmployeeGroup> employeeGroups) {
		this.employeeGroup1 = null;
		this.employeeGroup2 = null;
		if(employeeGroups!=null) {
			if(employeeGroups.size()>3) throw new IllegalArgumentException(employeeGroups +".length>3");
			this.employeeGroup1 = employeeGroups.size()>0? employeeGroups.get(0): null;
			this.employeeGroup2 = employeeGroups.size()>1? employeeGroups.get(1): null;
		} 
	}
	
	/**
	 * @return the department
	 */
	public List<EmployeeGroup> getEmployeeGroups() {
		List<EmployeeGroup> res = new ArrayList<>();
		if(employeeGroup1!=null) res.add(employeeGroup1);
		if(employeeGroup2!=null) res.add(employeeGroup2);
		return Collections.unmodifiableList(res);
	}
	
	public String getEmployeeGroupsAsString() {
		List<EmployeeGroup> egs = getEmployeeGroups();
		if(egs.size()==0) return "["+getCreUser()+"]";
		StringBuilder sb = new StringBuilder();
		for (EmployeeGroup eg : egs) {
			if(sb.length()>0) sb.append(", ");
			sb.append(eg.getNameShort());
		}
		return "["+sb.toString()+"]";
	}

	/**
	 * @param attachedBiosamples the attachedBiosamples to set
	 */
	public void setAttachedBiosamples(Set<Biosample> attachedBiosamples) {
		this.attachedBiosamples = attachedBiosamples;
	}
	
	/**
	 * @return the attachedBiosamples
	 */
	public Set<Biosample> getAttachedBiosamples() {
		return attachedBiosamples;
	}
	
	/**
	 * This method is in most case identical to getAttachedBiosamples except when we use the dividing feature, where animals can be divided into tissues, which are still attached to the study
	 * In that case we only return the top biosamples in study
	 * @return the attachedBiosamples
	 */
	public List<Biosample> getTopAttachedBiosamples() {
		List<Biosample> res = new ArrayList<>();		
		for(Biosample b: getAttachedBiosamples()) {
			if(b.getInheritedPhase()!=null) continue; //dividing sample
			res.add(b);
		}
		Collections.sort(res, Biosample.COMPARATOR_ANIMALNO);
		return res;
	}


	/**
	 * Same as getTopAttachedBiosample, but with an imposed filter on the group (if group==null, it means returns all samples with goup =null)(
	 * @param group
	 * @return
	 */
	public List<Biosample> getTopAttachedBiosamples(Group group) {
		List<Biosample> res = new ArrayList<>();		
		for (Biosample b : getTopAttachedBiosamples()) {
			if(group!=null && !group.equals(b.getInheritedGroup())) continue;
			if(group==null && b.getInheritedGroup()!=null) continue;
			res.add(b);			
		}
		return res;
	}
		
	public List<Biosample> getTopAttachedBiosamples(Group group, int subgroup) {
		List<Biosample> res = new ArrayList<>();		
		for (Biosample b : getTopAttachedBiosamples()) {
			if(group!=null && !group.equals(b.getInheritedGroup())) continue;
			if(group==null && b.getInheritedGroup()!=null) continue;
			
			if(b.getInheritedSubGroup()!=subgroup && group.getNSubgroups()>1) continue; 
			res.add(b);			
		}
		return res;
	}
	
	public SortedSet<Phase> getPhasesWithGroupAssignments() {
		SortedSet<Phase> res = new TreeSet<>();
		
		//Iterate through the groups to find the randophases
		for (Group g : getGroups()) {
			if(g.getFromPhase()==null || g.getDividingSampling()!=null) continue;
			res.add(g.getFromPhase());
		}		
		return res;
	}

	public boolean isBlind() {
		return getBlindAllUsersAsSet().size()>0 || getBlindDetailsUsersAsSet().size()>0;
	}
	
	public String getBlindAllUsers() {
		return MiscUtils.flatten(getBlindAllUsersAsSet(), ", ");
	}
	
	public String getBlindDetailsUsers() {
		return MiscUtils.flatten(getBlindDetailsUsersAsSet(), ", ");
	}
	
	public void setBlindUsers(Collection<String> all, Collection<String> group) {
		StringBuilder sb = new StringBuilder();
		for (String u : all) {
			if(sb.length()>0) sb.append(" ");
			sb.append("0#"+u);
		}
		for (String u : group) {
			if(sb.length()>0) sb.append(" ");
			sb.append("1#"+u);
		}

		this.blindUsers = sb.toString();
	}
	
	public void setBlindAllUsers(Collection<String> set) {
		setBlindUsers(set, getBlindDetailsUsersAsSet());				
	}
	
	public void setBlindDetailsUsers(Collection<String> set) {
		setBlindUsers(getBlindAllUsersAsSet(), set);				
	}	
	
	public Document getConsentForm() {
		for(Iterator<Document> iter = getDocuments().iterator(); iter.hasNext(); ) {
			Document d = iter.next();
			if(d.getType()==DocumentType.CONSENT_FORM) {
				return d;
			}
		}
		return null;
	}
	
	public void setSynchronizeSamples(boolean synchronizeSamples) {
		this.synchronizeSamples = synchronizeSamples;
	}
	
	public boolean isSynchronizeSamples() {
		return this.synchronizeSamples == Boolean.TRUE;
	}

	public Set<Biosample> getSamples(StudyAction action, NamedSampling ns) {
		assert ns!=null;
		assert action!=null;
				
		Set<Biosample> res = new HashSet<>();
		for(Sampling s: ns.getAllSamplings()) {
			for(Biosample b : s.getSamples()) {
				if(action.getGroup().equals(b.getInheritedGroup()) && action.getPhase().equals(b.getInheritedPhase()) && action.getSubGroup()==b.getInheritedSubGroup()) {
					res.add(b);
				}
			}
		}
		return res;
	}
		
	/**
	 * Gets all Measurements from studyActions (done on living)
	 * Note the Tests are not loaded, only the id is returned
	 * @return
	 */
	public Set<Measurement> getAllMeasurementsFromActions() {
		Set<Measurement> res = new TreeSet<>();
		for (StudyAction a : getStudyActions()) {
			res.addAll(a.getMeasurements());
		}
		return res;
	}
	
	/**
	 * Gets all Measurements from samplings (done on samples).
	 * Note the Tests are not loaded, only the id is returned
	 * @return
	 */
	public Set<Measurement> getAllMeasurementsFromSamplings() {
		Set<Measurement> res = new TreeSet<>();
		for (NamedSampling a : getNamedSamplings()) {
			for(Sampling s: a.getAllSamplings()) {
				res.addAll(s.getMeasurements());
			}
		}
		return res;
	}
	
	public Set<Phase> getEmptyPhases(){
		Set<Phase> res = new HashSet<>(getPhases());
		
		for (StudyAction a: getStudyActions()) {
			if(!a.isEmpty()) {
				res.remove(a.getPhase());
			}
		}
		
		for (Group group : getGroups()) {
			res.remove(group.getFromPhase());
		}
		
		for(Phase p: getPhases()) {
			if(p.getLabel()!=null && p.getLabel().length()>0) res.remove(p);
		}
		return res;
	}
	
	public PhaseFormat getPhaseFormat() {
		if(phaseFormat==null) phaseFormat = PhaseFormat.DAY_MINUTES;
		return phaseFormat;
	}
	
	public void setPhaseFormat(PhaseFormat phaseFormat) {
		this.phaseFormat = phaseFormat;
	}	

	public Phase getPhaseFirstTreatment(Group group, int subgroup) {
		if(group==null) return null;
		Pair<Group, Integer> key = new Pair<>(group, subgroup);
		Phase phaseFirstTreatment = phaseFirstTreatments.get(key);
		if(phaseFirstTreatment==null) {
			Phase res = null;
			for(StudyAction a: getStudyActions(group, subgroup)) {
				if(a.getNamedTreatment()==null) continue;
				if(res==null || res.compareTo(a.getPhase())>0) {
					res = a.getPhase();
				}
			}
			phaseFirstTreatment = res;
			phaseFirstTreatments.put(key, res);
		}
		return phaseFirstTreatment;
	}
	
	public static Map<String, Study> mapStudyId(Collection<Study> studies){
		Map<String, Study> res = new HashMap<>();
		if(studies==null) return res;
		for (Study s : studies) {
			if(s!=null) res.put(s.getStudyId(), s);
		}
		return res;
	}
	
	public static Map<String, List<Study>> mapIvvAndStudyId(Collection<Study> studies){
		Map<String, List<Study>> res = new HashMap<>();
		if(studies==null) return res;
		for (Study s : studies) {			
			if(s==null) continue;
			if(s.getIvv()!=null && s.getIvv().length()>0) {
				List<Study> l = res.get(s.getIvv());
				if(l==null) {
					res.put(s.getIvv(), l = new ArrayList<>());
				}
				l.add(s);
			}
			List<Study> l = res.get(s.getStudyId());
			if(l==null) {
				res.put(s.getStudyId(), l = new ArrayList<>());
			}
			l.add(s);

			
		}
		return res;
	}
	
	public static Set<String> getIvvOrStudyIds(Collection<Study> studies){
		Set<String> res = new HashSet<>();
		if(studies==null) return res;
		for (Study s : studies) {
			if(s!=null) res.add(s.getIvvOrStudyId());
		}
		return res;
	}

	public static Set<String> getStudyIds(Collection<Study> studies){
		Set<String> res = new HashSet<>();
		if(studies==null) return res;
		for (Study s : studies) {
			if(s!=null) res.add(s.getStudyId());
		}
		return res;
	}

	public Map<String, String> getMetadata() {
		if(metadataMap==null) {
			metadataMap = MiscUtils.deserializeStringMap(this.serializedMetadata);
		}
		return metadataMap; 
	}
	
	public  void setMetadata(Map<String, String> metadataMap) {
		this.metadataMap = metadataMap;
	}
	
	/**
	 * PreSave serializes the data. However this function must be called from within the DAO because setters do not call this function
	 */
	public void preSave() {
		mapGroupPhase2Action = null;
		//Fix documents without docType (Spirit v<=1.9)
		for (Document doc : getDocuments()) {
			if(doc.getType()==null) {
				doc.setType(DocumentType.DESIGN);
			}
		}

		//Serialize Metadata
		if(metadataMap!=null) {
			this.serializedMetadata = MiscUtils.serializeStringMap(metadataMap);
		}
		
		//Serialize Sampling
		for (NamedSampling ns : getNamedSamplings()) {
			for (Sampling s : ns.getAllSamplings()) {
				s.preSave();
			}			
		}
		
		//Update relations
		for (Phase p : new ArrayList<>(getPhases())) {
			p.setStudy(this);
		}
		for (Group g : new ArrayList<>(getGroups())) {
			g.setStudy(this);
		}
	}
	
	public boolean isMentioned(String user) {
		if(getAdminUsersAsSet().contains(user)) return true;
		if(getBlindAllUsersAsSet().contains(user)) return true;
		if(getExpertUsersAsSet().contains(user)) return true;
		if(getBlindDetailsUsers().contains(user)) return true;
		if(getCreUser().equals(user)) return true;
		return false;
	}
	
	public void resetIds() {
		for (Phase a : phases) {
			a.setId(0);
		}
		for (Group a : groups) {
			a.setId(0);
		}
		for (Group a : groups) {
			a.setId(0);
		}
		for (NamedTreatment a : namedTreatments) {
			a.setId(0);
		}
		for (NamedSampling a : namedSamplings) {
			a.setId(0);
			for (Sampling s : a.getAllSamplings()) {
				s.setId(0);
			}
		}
		for (StudyAction a : actions) {
			a.setId(0);
		}
		id = 0;
	}

}
