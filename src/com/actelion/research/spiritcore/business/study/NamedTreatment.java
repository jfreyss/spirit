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

import java.awt.Color;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import com.actelion.research.util.CompareUtils;

@Entity
@Audited
@Table(name="study_treatment")
public class NamedTreatment implements Comparable<NamedTreatment>, Cloneable {
	
	/**
	 * 
	 * @author freyssj
	 *
	 */
	public enum TreatmentUnit {
		//Quantity
		UG("ug", false),
		MG("mg", false),
		ML("ml", false),	
		NMOL("nMoles", false),
		MMOL("uMoles", false),
		
		//Debit
		UL_H("ul/h", false),
		
		//Concentration
		NG_ML("ng/ml", false),		
		NM("nM", false),
		UM("uM", false),
		MM("mM", false),
		PERCENT("%", false),
		CELLS("Millions Cells", false),
		
		//Based on Weight
		UG_KG("ug/kg", true),
		MG_KG ("mg/kg", true),
		ML_KG("ml/kg", true),
		PMOL("pmol/min/kg", true),
		
		;
		
		
		private final String unit;
		private final boolean weightDependant;
		private TreatmentUnit(String unit, boolean weightDependant) {
			this.unit = unit;
			this.weightDependant = weightDependant;
		}
		public String getUnit() {
			return unit;
		}
		public boolean isWeightDependant() {
			return weightDependant;
		}
		public String getNumerator() {
			int index = unit.lastIndexOf('/');
			if(index>=0) return unit.substring(0, index);
			return unit;
		}
		
		@Override
		public String toString() {
			return unit;
		}		
	}
	
	@Id
	@SequenceGenerator(name="treatment_sequence", sequenceName="treatment_sequence", allocationSize=1)
	@GeneratedValue(generator="treatment_sequence")
	private int id = 0;
	
	@ManyToOne(fetch=FetchType.LAZY, cascade={}, optional=false)
	@JoinColumn(name="study_id")
	private Study study = null;

	@Column(name="name", nullable=false)
	private String name;
	
	@Column(name="color")	
	private Integer colorRgb = 0;
	
	@Column(name="compound")
	private String compoundName;	
	
	///////////////////////////////////////////////////////
	@Column(name="dose")
	private Double dose;
	
	@Column(name="unit")
	@Enumerated(EnumType.STRING)
	private TreatmentUnit unit;
		
	@Column(name="application")
	private String application;

	
	///////////////////////////////////////////////////////
	@Column(name="compound2")
	private String compoundName2;	
	
	@Column(name="dose2")
	private Double dose2;
	
	@Column(name="unit2")
	@Enumerated(EnumType.STRING)
	private TreatmentUnit unit2;
		
	@Column(name="application2")
	private String application2;

	
	
	public NamedTreatment() {}

	public NamedTreatment(String name) {
		this.name = name;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCompoundName1() {
		return compoundName;
	}
	
	public void setCompoundName1(String compoundName) {
		this.compoundName = compoundName;
	}

	public Double getDose1() {
		return dose;
	}

	public void setDose1(Double dose) {
		this.dose = dose;
	}

	public TreatmentUnit getUnit1() {
		return unit;
	}

	public void setUnit1(TreatmentUnit unit) {
		this.unit = unit;
	}

	public String getApplication1() {
		return application;
	}

	public void setApplication1(String application) {
		this.application = application;
	}

	public void remove() {		
		if(study==null || study.getNamedTreatments()==null) return;
		
		for (StudyAction a : study.getStudyActions()) {
			if(this.equals(a.getNamedTreatment())) {
				a.setNamedTreatment(null);
			}
		}
		
		boolean success = study.getNamedTreatments().remove(this);
		if(!success) {
			System.err.println("Could not delete "+this+" "+this.getId()+" from "+study.getNamedTreatments());
		} else {
			study = null;
		}
	}

	@Override
	public int compareTo(NamedTreatment o) {
		if(this.equals(o)) return 0;
		return CompareUtils.compare(name, o.name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	public String getNameWithCompoundAndUnit() {
		String c = getCompoundAndUnits();
		return name + (c.length()>0?": "+c:"");
	}
	public void setStudy(Study study) {
		this.study = study;
	}

	public Study getStudy() {
		return study;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public String getCompoundAndUnits() {
		return getCompoundAndUnit(false, true, 0);
	}

	public String getCompoundAndUnit1() {
		return getCompoundAndUnit(false, true, 1);
	}

	public String getCompoundAndUnit2() {
		return getCompoundAndUnit(false, true, 2);
	}

	public String getCompoundAndUnits(boolean html, boolean blindMode) {
		return getCompoundAndUnit(html, blindMode, 0);
	}
	private String getCompoundAndUnit(boolean html, boolean blindMode, int n) {
		if(study==null) return "";
		boolean blind = study.isBlind();
		
		StringBuilder sb = new StringBuilder();
		if((n<=0 || n==1) && getCompoundName1()!=null && getCompoundName1().length()>0) {
			if(html) sb.append("<br>&nbsp;&nbsp;&nbsp;");
			if(blind && blindMode) sb.append(getCompoundName2()!=null && getCompoundName2().length()>0? "C1 ":"");
			else sb.append(getCompoundName1()+" ");
			if(getDose1()!=null && getUnit1()!=null) sb.append("("+getDose1() + getUnit1().getUnit() + (getApplication1()!=null? " " + getApplication1(): "") + ") ");
		} else if(html) {
			sb.append("<br>");
		}
		if((n<=0 || n==2) && getCompoundName2()!=null && getCompoundName2().length()>0) {
			if(html) sb.append("<br>&nbsp;&nbsp;&nbsp;");
			if(blind && blindMode) sb.append("C2 ");
			else sb.append(getCompoundName2()+" ");
			if(getDose2()!=null && getUnit2()!=null) sb.append("("+getDose2() + getUnit2().getUnit() + (getApplication2()!=null? " " + getApplication2(): "") + ")");
		} else if(html) {
			sb.append("<br>");
		}
		return sb.toString().trim();
	}

	public Double getCalculatedDose1(Double weight) {
		Double calculatedDose;
		if(getUnit1()!=null && getUnit1().isWeightDependant()) {			
			if(weight==null || getDose1()==null) return null;
			calculatedDose = getDose1() * weight / 1000.0;			
		} else {
			calculatedDose = getDose1();
		}			
		if(calculatedDose!=null) calculatedDose = ((int)(calculatedDose*1000))/1000.0; //round to 3 digits
		return calculatedDose;
	}
	
	public Double getCalculatedDose2(Double weight) {
		Double calculatedDose;
		if(getUnit2()!=null && getUnit2().isWeightDependant()) {			
			if(weight==null || getDose2()==null) return null;
			calculatedDose = getDose2() * weight / 1000.0;			
		} else {
			calculatedDose = getDose2();
		}			
		if(calculatedDose!=null) calculatedDose = ((int)(calculatedDose*1000))/1000.0; //round to 3 digits
		return calculatedDose;
	}
	
	public boolean isWeightDependant() {
		return (getUnit1()!=null && getUnit1().isWeightDependant()) || (getUnit2()!=null && getUnit2().isWeightDependant());
	}
	
//	public void setCompound(Compound compound) {
//		this.compound = compound;
//	}
//	public Compound getCompound() {
//		if(getCompoundName1().startsWith("ACT-")) {
//			return new Compound()
//		}
//		
//		return compound;
//	}

	private transient Color color = null;
	
	public Color getColor() {
		if(color==null) {
			if(colorRgb==null || colorRgb==0) {
				return Color.BLACK;
			}
			
			//Make the color darker if needed
			int value = colorRgb;
			int r = (value & 0xFF0000)>>16;
			int g = (value & 0xFF00)>>8;
			int b = (value & 0xFF);
			
			color = new Color(r, g, b);
		}
		return color;
	}

	
	public Integer getColorRgb() {
		return colorRgb==null?0: colorRgb;
	}
	
	public void setColorRgb(Integer colorRgb) {
		this.color = null;
		this.colorRgb = colorRgb;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NamedTreatment)) return false;
		NamedTreatment ns = (NamedTreatment) obj;
		if(this==ns) return true;
		if (this.getId() > 0 && ns.getId() > 0) return this.getId() == ns.getId();
		return false;
	}

	@Override
	public int hashCode() {
		return (int)(id%Integer.MAX_VALUE);
	}

	public String getCompoundName2() {
		return compoundName2;
	}

	public void setCompoundName2(String compoundName2) {
		this.compoundName2 = compoundName2;
	}

	public Double getDose2() {
		return dose2;
	}

	public void setDose2(Double dose2) {
		this.dose2 = dose2;
	}

	public TreatmentUnit getUnit2() {
		return unit2;
	}

	public void setUnit2(TreatmentUnit unit2) {
		this.unit2 = unit2;
	}

	public String getApplication2() {
		return application2;
	}

	public void setApplication2(String application2) {
		this.application2 = application2;
	}

	
	
}