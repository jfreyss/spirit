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
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

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
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.FormatterUtils;

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

	@Column(length=64)
	private String name;

	@ManyToOne(fetch=FetchType.LAZY, cascade={})
	private Study study = null;

	@Column(name="rnd_allsamples", length=10000)
	private String serializedRandomization = "";

	private transient Randomization randomization = null;
	private transient boolean parsed = false;
	private transient int days = 0;
	private transient int hours = 0;
	private transient int minutes = -1;
	private transient String label = "";

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

	@Override
	public void setId(int id) {
		this.id = id;
	}

	public void setStudy(Study study) {
		if(this.study==study) return;

		if(this.study!=null) {
			this.study.getPhases().remove(this);
		}

		this.study = study;

		if(this.study!=null) {
			this.study.getPhases().add(this);
		}
	}

	/**
	 * The short name is the prefix
	 * @return
	 */
	public String getShortName() {
		if(name==null) return "";
		if(!parsed) parseNameLabel();
		return name.substring(0, name.length()-label.length()).trim();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name==null?"": name.trim();
		this.parsed = false;
	}

	public String getLabel() {
		if(!parsed) parseNameLabel();
		return label;
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
		return getName()==null?"??":getName();
	}

	public String getAbsoluteDateAndName() {
		String phaseName = toString();
		if(getAbsoluteDate()!=null) {
			phaseName = FormatterUtils.formatDate(getAbsoluteDate()) + " - " + phaseName;
		}
		return phaseName;
	}

	@Override
	public boolean equals(Object obj) {
		if(this==obj) return true;
		if(! (obj instanceof Phase)) return false;
		Phase p = (Phase) obj;
		if(getId()>0 || p.getId()>0) return getId() == p.getId();
		return (getName()==null? "": getName()).equals(p.getName()==null? "": p.getName());
	}

	@Override
	public int compareTo(Phase o) {
		if(o==null) return -1;
		if(this==o) return 0;

		int c = getStudy()==null? (o.getStudy()==null?0:1): getStudy().compareTo(o.getStudy());
		if(c!=0) return c;

		c = getDays() - o.getDays();
		if(c!=0) return c;
		c = getHours() - o.getHours();
		if(c!=0) return c;
		c = getMinutes() - o.getMinutes();
		if(c!=0) return c;
		c = (getName()==null? "": getName()).compareTo(o.getName()==null? "": o.getName());
		if(c!=0) return c;

		return 0;
	}


	@Override
	public int hashCode() {
		return id;
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


		//Remove the links from groups
		for(Group gr: study.getGroups()) {
			if(this.equals(gr.getFromPhase())) gr.setFromPhase(null);
		}

		//Remove actions
		for (StudyAction a: new ArrayList<>(study.getStudyActions())) {
			if(this.equals(a.getPhase())) {
				a.remove();
			}
		}

		//Remove from the study
		boolean removed = study.getPhases().remove(this);
		if(!removed) {
			//Be sure to loop through all phases, the phases.remove(this) may fail if the phase is not sorted (bug from v2.0)
			int n = study.getPhases().size();
			for (Iterator<Phase> iterator = study.getPhases().iterator(); iterator.hasNext();) {
				Phase p = iterator.next();
				if(this.equals(p)) {
					iterator.remove();
					assert study.getPhases().size()==n-1;
					study = null;
					return;
				}
			}
		}

		if(removed) {
			study = null;
		}
	}

	/**
	 * Parses day, hours, minutes, seconds of the phase name formatted like "d1_5h05 Label" or "1 Name"
	 */
	private void parseNameLabel() {
		parsed = true;
		days = 0;
		hours = 0;
		minutes = 0;

		String s = getName();
		if(s==null) return;
		int index = 0;

		if(s.startsWith("d")) {
			//The phase is formatted like "d1_5h05 Label"
			Pair<Integer,Integer> d = parseNumberStartingAt(s, 1);
			if(d!=null) {
				days = d.getFirst();
				index = d.getSecond()+1;
				Pair<Integer,Integer> h = parseNumberStartingAt(s, index);;
				if(h!=null) {
					hours = h.getFirst();
					index = h.getSecond()+1;
					Pair<Integer,Integer> m = parseNumberStartingAt(s, index);
					if(m!=null) {
						minutes = m.getFirst();
						index = m.getSecond()+1;
					}
				}
			}
		} else {
			//The phase is formatted like "1. Label", "2. Label"
			Pair<Integer,Integer> d = parseNumberStartingAt(s, 0);
			days = d==null? Integer.MAX_VALUE: d.getFirst();
			index = d==null? s.length(): d.getSecond();
		}
		label = index>=s.length()?"": trim(s.substring(index));

	}

	public static String cleanName(String name, PhaseFormat phaseFormat) {
		if(name==null || name.trim().length()==0) return "";
		Phase p = new Phase(name);
		if(phaseFormat==PhaseFormat.DAY_MINUTES) {
			return name = "d" + p.getDays() +
					(p.getHours()!=0 || p.getMinutes()!=0? "_" + p.getHours() + "h" + (p.getMinutes()!=0?p.getMinutes():""):"") +
					(p.getLabel()!=null && p.getLabel().length()>0? " "+p.getLabel(): "");
		} else if(phaseFormat==PhaseFormat.NUMBER) {
			return name = p.getDays() + "." +
					(p.getLabel()!=null && p.getLabel().length()>0? " "+p.getLabel(): "");
		} else {
			return name;
		}

	}

	/**
	 * Special trim function, which remove all non alphanumerical characters from the string
	 * (ie. '. label()' returns 'label'
	 * @param s
	 * @return
	 */
	private String trim(String s) {
		int i1 = 0;
		int i2 = s.length();
		for(;i1<s.length() && !Character.isLetterOrDigit(s.charAt(i1)); i1++) {}
		for(;i2>i1 && !Character.isLetterOrDigit(s.charAt(i2-1)); i2--) {}

		return s.substring(i1, i2);
	}

	/**
	 * Return the number of days as specified in d-1, d1, d2
	 * or the phasenumber as specified in 1., 2.
	 */
	public int getDays() {
		if(!parsed) parseNameLabel();
		return days;
	}

	public int getHours() {
		if(!parsed) parseNameLabel();
		return hours;
	}

	public int getMinutes() {
		if(!parsed) parseNameLabel();
		return minutes;
	}


	/**
	 * Extract the number starting at offset
	 * extractNumber(d0, 1) ->0
	 * extractNumber(d1_??, 1) ->1
	 * extractNumber(d-2 ??, 1) ->-2
	 * @return Pair<Integer, Integer>(found number, new index) or null if not found
	 */
	private static Pair<Integer, Integer> parseNumberStartingAt(String s, int index) {
		if(s==null) return null;
		if(index>=s.length()) return null;

		//Check for '-' sign
		boolean negative;
		if(s.charAt(index)=='-') {
			negative = true;
			index++;
			if(index>=s.length()) return null;
		} else {
			negative = false;
		}

		if(!Character.isDigit(s.charAt(index))) return null;

		int n = 0;
		while(index<s.length() && Character.isDigit(s.charAt(index))) {
			n = n*10 + s.charAt(index)-'0';
			index++;
		}
		return new Pair<Integer, Integer>((negative?-1:1) * n, index);
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
		if(this.randomization!=null) {
			randomization.reset();
		}

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
			description.append(treatments.iterator().next().getName());
		} else if(treatments.size()>0) {
			if(description.length()>0) description.append(" + ");
			description.append("Treatments");
		}

		if(nss.size()==1) {
			if(description.length()>0) description.append(" + ");
			description.append(nss.iterator().next().getName());
		} else if(nss.size()==2) {
			if(description.length()>0) description.append(" + ");
			description.append("Samplings");
		}
		return description.toString();
	}

	/**
	 * Comparator that compare all fields, to check if a modification occured
	 */
	public static Comparator<Phase> EXACT_COMPARATOR = new Comparator<Phase>() {
		@Override
		public int compare(Phase o1, Phase o2) {
			int c = o1.getName().compareTo(o2.getName());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.getSerializedRandomization(), o2.getSerializedRandomization());
			if(c!=0) return c;

			return 0;
		}
	};

}
