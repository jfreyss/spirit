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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.util.CompareUtils;

/**
 * StudyAction is the container used to describe the actions, that can take place at a given group, subgroup, phase.
 * Those actions are the treatment, the sampling1, the sampling2 and the different measurements
 *
 * @author freyssj
 *
 */
@Audited
@Entity
@Table(name="study_action")
@SequenceGenerator(name="action_sequence", sequenceName="action_sequence", allocationSize=1)
public class StudyAction implements Cloneable, Comparable<StudyAction> {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="action_sequence")
	private int id = 0;


	@JoinColumn(name="study_id")
	@ManyToOne(fetch=FetchType.LAZY, optional=true, cascade={})
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.CASCADE)
	private Study study;

	/** (null possible only for deletion) */
	@JoinColumn(name="phase_id")
	@ManyToOne(fetch=FetchType.LAZY, optional=true, cascade=CascadeType.PERSIST)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@BatchSize(size=64)
	private Phase phase;

	/** (null possible only for deletion) */
	@JoinColumn(name="group_id")
	@ManyToOne(fetch=FetchType.LAZY, optional=true, cascade=CascadeType.PERSIST)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@BatchSize(size=16)
	private Group group;

	@Column(name="stratifiedgroup")
	private Integer subGroup = 0;

	/**Some description*/
	@Column(name="label")
	private String label;

	@JoinColumn(name="namedtreatment_id")
	@ManyToOne(fetch=FetchType.LAZY, optional=true, cascade=CascadeType.PERSIST)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@BatchSize(size=16)
	private NamedTreatment namedTreatment;

	@JoinColumn(name="namedsampling_id")
	@ManyToOne(fetch=FetchType.LAZY, optional=true, cascade=CascadeType.PERSIST)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@BatchSize(size=16)
	private NamedSampling namedSampling1;

	@JoinColumn(name="namedsampling2_id")
	@ManyToOne(fetch=FetchType.LAZY, optional=true, cascade=CascadeType.PERSIST)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@BatchSize(size=16)
	private NamedSampling namedSampling2;


	@Column(name="measurefood")
	private Boolean measureFood = false;

	@Column(name="measurewater")
	private Boolean measureWater = false;

	@Column(name="measureweight")
	private Boolean measureWeight = false;

	/**
	 * Extra Measurements, serialized as:
	 * testId1#Input1_1#Input1_2, testId2#Input1_1#Input1_2,
	 */
	@Column(name="extrameasurement", length=256)
	private String extraMeasurement;


	private transient List<Measurement> extraMeasurementList;



	public StudyAction() {
	}

	public StudyAction(Study study, Group group, int subGroupNo, Phase phase) {
		if(study == null || group==null || phase==null) throw new IllegalArgumentException("Group and phase are required");
		if(group.getStudy()!=null && !group.getStudy().equals(study)) throw new IllegalArgumentException("The study does not match: "+group.getStudy()+" vs "+phase.getStudy());
		if(phase .getStudy()!=null && !phase .getStudy().equals(study)) throw new IllegalArgumentException("The study does not match: "+group.getStudy()+" vs "+phase.getStudy());
		this.study = study;
		this.group = group;
		this.phase = phase;
		this.subGroup = subGroupNo;
	}

	public StudyAction(StudyAction copy) {
		this(copy.getStudy(), copy.getGroup(), copy.getSubGroup(), copy.getPhase());
		setLabel(copy.getLabel());
		setNamedSampling1(copy.getNamedSampling1());
		setNamedSampling2(copy.getNamedSampling2());
		setNamedTreatment(copy.getNamedTreatment());
		setMeasureFood(copy.isMeasureFood());
		setMeasureWater(copy.isMeasureWater());
		setMeasureWeight(copy.isMeasureWeight());
		setMeasurements(new ArrayList<>(copy.getMeasurements()));
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public Phase getPhase() {
		return phase;
	}


	public void setPhase(Phase phase) {
		this.phase = phase;
	}


	public Group getGroup() {
		return group;
	}


	public void setGroup(Group group) {
		assert group!=null;
		this.group = group;
		this.study = group.getStudy();
	}

	public NamedTreatment getNamedTreatment() {
		return namedTreatment;
	}


	public void setNamedTreatment(NamedTreatment namedTreatment) {
		this.namedTreatment = namedTreatment;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(!(obj instanceof StudyAction)) return false;
		StudyAction a = (StudyAction) obj;
		if(id>0 && id==a.id) return true;

		if((getStudy()==null && a.getStudy()!=null) || (getStudy()!=null && !getStudy().equals(a.getStudy()))) return false;
		if((getGroup()==null && a.getGroup()!=null) || (getGroup()!=null && !getGroup().equals(a.getGroup()))) return false;
		if((getPhase()==null && a.getPhase()!=null) || (getPhase()!=null && !getPhase().equals(a.getPhase()))) return false;
		if(getSubGroup()!=a.getSubGroup()) return false;
		return true;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public int compareTo(StudyAction o) {
		int c = CompareUtils.compare(getGroup(), o.getGroup());
		if(c!=0) return c;
		c = CompareUtils.compare(getPhase(), o.getPhase());
		if(c!=0) return c;
		return getSubGroup() - o.getSubGroup();
	}

	public NamedTreatment getLastNamedTreatment() {
		NamedTreatment res = null;
		for(Phase p: getStudy().getPhases()) {
			if(p.equals(getPhase())) break;
			StudyAction a = getStudy().getStudyAction(group, subGroup, p);
			if(a!=null && a.getNamedTreatment()!=null) res = a.getNamedTreatment();

		}
		return res;
	}



	public String getNamedSamplingString() {
		String s = (getNamedSampling1()==null? "": getNamedSampling1().getName() + " ") +
				(getNamedSampling2()==null? "": getNamedSampling2().getName() + " ");
		return s.trim();
	}

	public Set<NamedSampling> getNamedSamplings() {
		Set<NamedSampling> res = new LinkedHashSet<>();
		if(getNamedSampling1()!=null) res.add(getNamedSampling1());
		if(getNamedSampling2()!=null) res.add(getNamedSampling2());
		return res;
	}

	public NamedSampling getNamedSampling1() {
		return namedSampling1;
	}

	public void setNamedSampling1(NamedSampling namedSampling) {
		this.namedSampling1 = namedSampling;
	}

	public NamedSampling getNamedSampling2() {
		return namedSampling2;
	}


	public void setNamedSampling2(NamedSampling namedSampling) {
		this.namedSampling2 = namedSampling;
	}



	public void setStudy(Study study) {
		this.study = study;
	}


	public Study getStudy() {
		return study;
	}

	public void remove() {
		if(getStudy()==null || getStudy().getStudyActions()==null) return;
		getStudy().getStudyActions().remove(this);
		study = null;
		group = null;
		phase = null;
		namedTreatment = null;
		namedSampling1 = null;
		namedSampling2 = null;
	}

	@Override
	public StudyAction clone() {
		try {
			return (StudyAction) super.clone();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean isEmpty() {
		return !hasMeasurements() &&
				getNamedTreatment()==null && getNamedSampling1()==null && getNamedSampling2()==null && (getLabel()==null || getLabel().length()==0);
	}

	@Override
	public String toString() {
		return "[Action:"+(getGroup()==null?"":getGroup().getShortName())+"'"+getSubGroup()+"/"+getPhase()+"]";
	}
	public boolean isMeasureWeight() {
		return measureWeight == Boolean.TRUE;
	}
	public void setMeasureWeight(boolean measureWeight) {
		this.measureWeight = measureWeight;
	}
	public boolean isMeasureFood() {
		return measureFood == Boolean.TRUE;
	}
	public void setMeasureFood(boolean measureFood) {
		this.measureFood = measureFood;
	}
	public boolean isMeasureWater() {
		return measureWater == Boolean.TRUE;
	}
	public void setMeasureWater(boolean measureWater) {
		this.measureWater = measureWater;
	}

	public int getSubGroup() {
		return subGroup==null? 0: subGroup;
	}
	public void setSubGroup(int subGroup) {
		this.subGroup = subGroup;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean hasMeasurements() {
		return isMeasureFood() || getMeasurements().size()>0 || isMeasureWater() || isMeasureWeight();
	}

	/**
	 * Returns a list of Measurement (never null), after deserializing it from the DB, the Measurement's Test is not populated by this function.
	 * However we populate it in DAOStudy.postLoad
	 * @return
	 */
	public List<Measurement> getMeasurements() {
		if(extraMeasurementList==null) {
			extraMeasurementList = Measurement.deserialize(extraMeasurement);
		}
		return Collections.unmodifiableList(extraMeasurementList);
	}

	public String getMeasurementAbbreviations() {
		StringBuilder sb = new StringBuilder();
		for(Measurement m : getMeasurements()) {
			Test t = m.getTest();
			String s = t==null || t.getName()==null || t.getName().length()==0? "M" : t.getName().substring(0, 1);
			sb.append(s);
		}
		if (isMeasureFood()) {
			sb.append("F");
		}
		if (isMeasureWater()) {
			sb.append("O");
		}
		if (isMeasureWeight()) {
			sb.append("w");
		}
		return sb.toString();
	}

	/**
	 * Sets the measurements, it is assumed that each measurement's test is not null
	 * @param list
	 */
	public void setMeasurements(List<Measurement> list) {
		this.extraMeasurementList = list;
		extraMeasurement = Measurement.serialize(extraMeasurementList);
	}

	public void setMeasurementString(String extraMeasurement) {
		this.extraMeasurementList = null;
		this.extraMeasurement = extraMeasurement;
	}

	public String getMeasurementString() {
		return extraMeasurement;
	}

	public static Set<Measurement> getMeasurements(Collection<StudyAction> actions) {
		Set<Measurement> res = new HashSet<>();
		for (StudyAction a: actions) {
			res.addAll(a.getMeasurements());
		}
		return res;
	}


	/**
	 * Comparator that compare all fields, to check if a modification occured
	 */
	public static Comparator<StudyAction> EXACT_COMPARATOR = new Comparator<StudyAction>() {
		@Override
		public int compare(StudyAction o1, StudyAction o2) {
			int c;
			c = CompareUtils.compare(o1.isMeasureFood(), o2.isMeasureFood());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.isMeasureWater(), o2.isMeasureWater());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.isMeasureWeight(), o2.isMeasureWeight());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.getMeasurementString(), o2.getMeasurementString());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.getNamedTreatment(), o2.getNamedTreatment());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.getNamedSamplingString(), o2.getNamedSamplingString());
			if(c!=0) return c;

			return 0;
		}
	};

}
