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
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.util.Formatter;

/**
 * The phase can be formatted in 2 different ways:
 * - d12_5h25 label 
 * - 1. label
 */
@Audited
@Entity
@Table(name="study_phase", uniqueConstraints= {@UniqueConstraint(columnNames= {"study_id", "name"})}, indexes = {@Index(name="phase_study_index", columnList = "study_id")})
@SequenceGenerator(name="phase_sequence", sequenceName="phase_sequence", allocationSize=1)
@BatchSize(size=64)
public class Phase implements IObject, Comparable<Phase>, Cloneable {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="phase_sequence")
	private int id = 0;

	@Column(name="name", length=64, nullable=false)
	private String name;
	
	private transient int days = 0; //any positive or negative, or null value is allowed
	private transient int hours = 0; // should always be >0
	private transient int minutes = -1; //-1 means not yet calculated, should always be >0
	
	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY)
	private Study study = null;
	
	
	@Column(name="rnd_allsamples", length=10000)
	private String serializedRandomization = "";

	private transient Randomization randomization = null;
	
	public Phase() {}
	
	public Phase(String name) {
		setName(name);
	}	


	@Override
	public int getId() {
		return id;
	}
	
	public Study getStudy() {
		return study;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public void setStudy(Study study) {
		this.study = study;
	}
	
	public String getShortName() {
		if(name==null) return "";
		int index = name.indexOf(' ');
		return index<0? name: name.substring(0, index).trim();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name==null?"": name.trim();
		this.minutes = -1;
	}

	public String getLabel() {
		if(name==null) return "";
		int index = name.indexOf(' ');
		return index<0?"": name.substring(index+1).trim();
	}
		
	public Date getAbsoluteDate() {
		if(study==null) return null;
		if(study.getPhaseFormat()==PhaseFormat.DAY_MINUTES) {
			Date d = getStudy().getDayOneDate();
			if(d==null) return null;
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + getDays()-1);
			cal.set(Calendar.HOUR_OF_DAY, getHours());
			cal.set(Calendar.MINUTE, getMinutes());

			return cal.getTime();
		} else {
			return null;
		}	
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public String getAbsoluteDateAndName() {
		String phaseName = toString();
		if(getAbsoluteDate()!=null) {
			phaseName = Formatter.formatDate(getAbsoluteDate()) + " - " + phaseName;
		}
		return phaseName;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(! (obj instanceof Phase)) return false;
		return this.compareTo((Phase) obj)==0;		
	}
	
	@Override
	public int compareTo(Phase o) {
		if(o==null) return -1;
		int c = getDays() - o.getDays(); 
		if(c!=0) return c;
		c = getHours() - o.getHours();
		if(c!=0) return c;
		c = getMinutes() - o.getMinutes();
		if(c!=0) return c;
		return getName()==null? (o.getName()==null?0: 1):  (o.getName()==null?-1: getName().compareTo(o.getName()));
	}
	

	@Override
	public int hashCode() {
		return name==null?0: name.hashCode();
	}
	
	
	@Override
	public Phase clone() {
		try {
			return (Phase) super.clone();	
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
		
	public void remove() {
		if(study==null || study.getPhases()==null) return;

		study.getPhases().remove(this);
		
		for(Group gr: study.getGroups()) {
			if(gr.getFromPhase()==this) gr.setFromPhase(null);
		}
		
		//Remove actions of this group
		for (StudyAction a: new ArrayList<StudyAction>(study.getStudyActions())) {
			if(this.equals(a.getPhase())) {
				a.remove();
			}
		}

		study = null;
	}
	
	private void parseDayHoursMinutes() {
		String s = getShortName();
		int index = s.indexOf("d");
		if(index<0) {			
			Integer d = extractNumber(s, 0);
			days = d==null? 0: d;
			hours = 0;
			minutes = 0;
		} else {
			Integer d = extractNumber(s, index+1);
			if(d==null) {
				days = 0;
				hours = 0;
				minutes = 0;				
			} else {
				days = d;				
				index = s.indexOf("_", index);
				if(index<0) {
					hours = 0;
					minutes = 0;
				} else {
					Integer h = extractNumber(s, index+1);;
					if(h==0) {
						hours = 0;
						minutes = 0;
					} else {
						hours = h;
						index = s.indexOf("h", index);
						if(index<0) {
							minutes = 0;
						} else {
							Integer m = extractNumber(s, index+1);
							minutes = m==null? 0: m;
						}
							
					}
				}
			}
		}
	}
	/**
	 * Return the number of days as specified in d-1, d1, d2
	 * or the phasenumber as specified in 1., 2.
	 */
	public int getDays() {
		if(minutes<0) parseDayHoursMinutes();
		return days;
	}
	
	public int getHours() {
		if(minutes<0) parseDayHoursMinutes();
		return hours;
	}
	
	
	public int getMinutes() {
		if(minutes<0) parseDayHoursMinutes();
		return minutes;
	}

	
	private static Integer extractNumber(String s, int offset) {
		int index = offset;
		if(index>=s.length()) return null;
		boolean negative;
		if(s.charAt(index)=='-') {
			negative = true; 
			index++;
			if(index>=s.length()) return null;
		} else {
			negative = false;
		}
		
		int n = 0;
		while(index<s.length() && Character.isDigit(s.charAt(index))) {
			n = n*10 + s.charAt(index)-'0';
			index++;
		}		
		return (negative?-1:1) * n;
	}
	
	/**
	 * Get relative time in minutes
	 * @return
	 */
	public int getTime() {
		return (getDays() * 24 + getHours()) * 60 + getMinutes(); 
	}

	public Phase getNextPhase() {
		if(study==null) return null;
		boolean found = false;
		
		for (Phase p : study.getPhases()) {
			if(found) return p;
			if(p.equals(this)) found=true;
		}
		return null;
	}

	public static boolean isSameDay(Date d1, Date d2) {
		if(d1==null || d2==null) return false;
		Calendar cal = Calendar.getInstance();
		cal.setTime(d1);
		int t1 = cal.get(Calendar.YEAR) * 366 + cal.get(Calendar.DAY_OF_YEAR);
		cal.setTime(d2);
		int t2 = cal.get(Calendar.YEAR) * 366 + cal.get(Calendar.DAY_OF_YEAR);
		return t1==t2;
	}
	
	public void resetRandomization() {
		randomization.setSamples(new ArrayList<AttachedBiosample>());
		
	}
	public Randomization getRandomization() {
		if(this.randomization==null) {
			this.randomization = new Randomization(this, getSerializedRandomization());
		}
		return this.randomization;
	}
	
	public void serializeRandomization() {
		if(randomization!=null) this.randomization.serialize();
	}
	
	public boolean hasRandomization() {
		for (Group group : study.getGroups()) {
			if(this.equals(group.getFromPhase())) return true;
		}
		return false;
	}
	
	public void setSerializedRandomization(String serializedRandomization) {
		this.serializedRandomization = serializedRandomization;
	}
	
	public String getSerializedRandomization() {
		return serializedRandomization;
	}
	
	
	public String getDescription() {
		StringBuilder description = new StringBuilder();
		Set<NamedSampling> nss = study.getNamedSamplings(this);
		Set<NamedTreatment> treatments = study.getNamedTreatments(this);
//		if(study.getPhasesWithGroupAssignments().contains(this)) {
//			description.append("<span style='color:#0000AA'>Group Assignment</span>"); 
//		} 
		boolean weighing = false;
		boolean food = false;
		boolean water = false;
		boolean measurements = false;
		Set<StudyAction> actions = study.getStudyActions(this);
		for(StudyAction a: actions) {
			if(a.isMeasureWeight()) weighing = true; 
			if(a.isMeasureFood()) food = true; 
			if(a.isMeasureWater()) water = true;
			if(a.getMeasurements().size()>0) measurements = true;
		}
		if(weighing) description.append((description.length()>0?" + ":"") + "Weighing");
		if(food) description.append((description.length()>0?" + ":"") + "Food");
		if(water) description.append((description.length()>0?" + ":"") + "Water");
		if(measurements) description.append((description.length()>0?" + ":"") + "Meas.");
		
		if(treatments.size()==1) {		
			if(description.length()>0) description.append(" + ");
			description.append("<span style='color:#"+ Integer.toHexString(treatments.iterator().next().getColor().getRGB()).substring(2).toUpperCase() + "'>" + treatments.iterator().next().getName()+"</span>");
		} else if(treatments.size()>0) {		
			if(description.length()>0) description.append(" + ");
			description.append("<span style='color:#0088AA'>Treatments</span>");
		}

		if(nss.size()==1) {		
			if(description.length()>0) description.append(" + ");
			description.append("<span style='color:#AA0000'>" + nss.iterator().next().getName()+"</span>");
		} else if(nss.size()==2) {		
			if(description.length()>0) description.append(" + ");
			description.append("<span style='color:#AA0000'>Samplings</span>");
		}
		return description.toString();
	}

}
