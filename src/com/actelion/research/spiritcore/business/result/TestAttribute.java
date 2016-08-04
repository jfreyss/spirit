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

package com.actelion.research.spiritcore.business.result;

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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.util.MiscUtils;

@Entity
@Table(name="assay_attribute", indexes = {		
		@Index(name="assay_attribute_assay_idx", columnList = "assay_id")})
@SequenceGenerator(name="assay_attribute_seq", sequenceName="assay_attribute_seq", allocationSize=1)
@Audited
@AuditTable(value="assay_attribute_aud")
public class TestAttribute implements Comparable<TestAttribute> {

	public static enum OutputType {
		INPUT, OUTPUT, INFO
	}

	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="assay_attribute_seq")
	@Column(name="assay_attribute_id")
	private int id = 0;

	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY)
	@JoinColumn(name="assay_id")
	private Test test;
	
	@Column(name="assay_attribute_name", nullable=false)
	private String name;
	
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	private DataType dataType;

	@Column(nullable=false, name="is_output")
	@Enumerated(EnumType.ORDINAL)
	private OutputType outputType = OutputType.OUTPUT;
	
	@Column(nullable=false, name="is_required")
	private boolean isRequired = false;

	@Column(nullable=true, length=1000)
	private String parameters;
	
	@Column(name="idx", nullable=false)
	private int index;

	public TestAttribute() {}
	
	public TestAttribute(Test test, String name) {
		this.test = test;
		this.name = name;
		this.dataType = DataType.NUMBER;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DataType getDataType() {
		return dataType;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	public OutputType getOutputType() {
		return outputType;
	}
	public void setOutputType(OutputType outputType) {
		this.outputType = outputType;
	}
	
//	public boolean isOutput() {
//		return isOutput;
//	}
//
//	public void setOutput(boolean isOutput) {
//		this.isOutput = isOutput;
//	}

	public boolean isRequired() {
		return isRequired;
	}

	public void setRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	/**
	 * Extract the unit from the name (in brackets)
	 * @return
	 */
	public String getUnit() {
		return extractUnit(name);
	}
	
	@Override
	public int compareTo(TestAttribute o) {
		int c = getTest().compareTo(o.getTest());
		if(c!=0) return c;
		
		c = getOutputType().compareTo(o.getOutputType());
		if(c!=0) return c;
		
		c = getIndex() - o.getIndex();
		if(c!=0) return c;
		
		return (int) (id - o.id);
	}

	@Override
	public int hashCode() {
		return (int)(id%Integer.MAX_VALUE);
	}
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof TestAttribute)) return false;		
		if(this==obj) return true;		
		TestAttribute a2 = (TestAttribute) obj;
//		if(getId()>0 && a2.getId()>0) return getId()==a2.getId();
		return this.compareTo(a2)==0;
	}
	
	@Override
	public String toString() {
		return getName() + "("+getId()+")";
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public int getIndex() {
		return index;
	}
	public void setTest(Test assay) {
		this.test = assay;
	}
	public Test getTest() {
		return test;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	/**
	 * @return the parameters
	 */
	public String getParameters() {
		return parameters;
	}
	
	public String[] getParametersArray() {
		if(this.parameters==null) return new String[0];
		return MiscUtils.split(this.parameters);
	}
	public void setParametersArray(String[] parameters) {
		this.parameters = MiscUtils.unsplit(parameters);
	}
	
	public static String extractUnit(String name) {
		if(name==null) return null;
		int index1 = name.lastIndexOf('[');
		int index2 = name.lastIndexOf(']');
		if(index1>0 && index1<index2) {
			return name.substring(index1+1, index2).trim();
		}
		
		index1 = name.lastIndexOf('(');
		index2 = name.lastIndexOf(')');
		if(index1>0 && index1<index2) {
			return name.substring(index1+1, index2).trim();
		}

		return null;
	}
	public static String extractNameWithoutUnit(String name) {
		if(name==null) return null;
		int index1 = name.lastIndexOf('[');
		int index2 = name.lastIndexOf(']');
		if(index1>0 && index1<index2) {
			return (name.substring(0, index1) + name.substring(index2+1)).trim();
		}
		
		index1 = name.lastIndexOf('(');
		index2 = name.lastIndexOf(')');
		if(index1>0 && index1<index2) {
			return (name.substring(0, index1) + name.substring(index2+1)).trim();
		}

		return name;
		
	}

	@Override
	public TestAttribute clone() {
		TestAttribute res = new TestAttribute();
		res.setDataType(dataType);
		res.setId(id);
		res.setIndex(index);
		res.setName(name);
		res.setOutputType(outputType);
		res.setParameters(parameters);
		res.setRequired(isRequired);
		res.setTest(test);
		return res;
	}

}
