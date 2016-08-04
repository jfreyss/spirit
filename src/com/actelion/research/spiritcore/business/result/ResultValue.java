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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.CompareUtils;

@Entity
@Table(name="assay_result_value", indexes = {		
		@Index(name="value_attribute_idx", columnList = "assay_attribute_id"),
		@Index(name="value_result_idx", columnList = "assay_result_id"),
//		@Index(name="value_compound_idx", columnList = "linked_compound_id"),
		@Index(name="value_detail_idx", columnList = "assay_result_detail_id")})
@SequenceGenerator(name="assay_result_value_seq", sequenceName="assay_result_value_seq", allocationSize=1)
@Audited
@AuditTable(value="assay_result_value_aud")
public class ResultValue implements Comparable<ResultValue> {
	
	private static final String[] validValues = new String[] {"<LOD", ">LOD", "N/A", "NA", "?"};
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="assay_result_value_seq")
	@Column(name="assay_result_value_id")
	private int id = 0;
	
	@Column(name="text_value")
	private String value = "";
	
	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="assay_attribute_id", nullable=false)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	@BatchSize(size=50)	
	private TestAttribute attribute;
	
	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="assay_result_id", nullable=false)
	private Result result;
	

	@ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY, optional=true)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	@JoinColumn(name="assay_result_detail_id", nullable=true)	
	private ResultDetail resultDetail;
	
//	@OneToOne(cascade=CascadeType.REFRESH, optional=true, fetch=FetchType.LAZY)
//	@JoinColumn(name="linked_compound_id")
//	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
//	@BatchSize(size=20)
//	private Compound linkedCompound;
	
	private transient Double calculatedValue = null;
	
	private transient Biosample linkedBiosample;

	
	public ResultValue() {
	}

	protected ResultValue(Result r, TestAttribute att, String value) {
		this.result = r;
		this.attribute = att;
		this.value = value;
	}
	
	
	public void setAttribute(TestAttribute attribute) {
		this.attribute = attribute;
	}

	public TestAttribute getAttribute() {
		return attribute;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
	
	@Override
	public int hashCode() {
		return attribute==null? 0: attribute.hashCode();
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value==null? null: value.trim();
	}
	
	public Double getDoubleValue() {
		//For numeric value, add the double value 
		if(attribute.getDataType()==DataType.NUMBER) {
			if(this.value==null || this.value.length()==0) {
				return null;
			} else {
				try {				
					int offset = 0;
					while(offset<this.value.length() && "<>= ".indexOf(this.value.charAt(offset))>=0) {
						offset++;
					}
					int index2 = this.value.length()-1;
					while(index2>=offset && "% ".indexOf(this.value.charAt(index2))>=0) {
						index2--;
					}

					return Double.parseDouble(this.value.substring(offset, index2+1));
				} catch (NumberFormatException e) {
					return null;
				}
			}
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		return getValue()==null?"": getValue();
	}

	@Override
	public int compareTo(ResultValue o) {
		return getComparator().compare(this, o);
	}
	
	public static Comparator<ResultValue> getComparator() {
		return new Comparator<ResultValue>() {
			@Override
			public int compare(ResultValue o1, ResultValue o2) {
				int c = CompareUtils.compare(o1.getAttribute(), o2.getAttribute());
				if(c!=0) return c;
				return CompareUtils.compare(o1.getValue(), o2.getValue());
			}
		};
		
	}

	public void setResult(Result result) {
		if(result==null) throw new IllegalArgumentException("Result cannot be null");
		this.result = result;
	}

	public Result getResult() {
		if(result==null) throw new IllegalArgumentException("Result cannot be null");
		return result;
	}
	
	
	/**
	 * In some cases, the unit can be written on the input value (ex: CCL3 [pg/ml])
	 * The associated output parameter has a value in this unit.
	 * 
	 * getDelegateInputValue() will return the delegated input value if any (or null)
	 * 
	 * @return
	 */
	private ResultValue getDelegateInputValue() {
		Result r = getResult();
		List<ResultValue> output = r.getOutputResultValues(); 
		List<ResultValue> input = r.getInputResultValues(); 
		int index = output.indexOf(this);
		if(index<0 || index>=input.size()) return null;
		
		ResultValue in = input.get(index);
		if(in.getUnitToDelegate()==null) return null;
		return in;
	}
	
	
	private String getUnitToDelegate() {
		if(value==null) return null;
		int index1 = value.lastIndexOf('[');
		int index2 = value.lastIndexOf(']');
		if(index1>0 && index1<index2) {
			return value.substring(index1+1, index2);
		}
		
		index1 = value.lastIndexOf('(');
		index2 = value.lastIndexOf(')');
		if(index1>0 && index1<index2) {
			return value.substring(index1+1, index2);
		}

		return null;
	}
	
	/**
	 * In some cases, the unit can be written on the input value (ex: CCL3 [pg/ml])
	 * <br>
	 * getDelegateUnit() can be called on an output value to return the delegated unit if any [pg/ml] (or null)
	 * 
	 * @return
	 */
	public String getDelegateUnit() {
		String s = getUnitToDelegate();
		if(s!=null) return s;
		ResultValue v = getDelegateInputValue();
		if(v!=null) return v.getUnitToDelegate();
		return null;
	}
	
	public String getUnit() {
		ResultValue v = getDelegateInputValue();
		if(v!=null) return v.getUnitToDelegate();
		return getAttribute().getUnit();
	}

	
	/**
	 * In some cases, the unit can be written on the input value (ex: CCL3 [pg/ml])
	 * <br> 
	 * getValueWithoutDelegateUnit() can be called on an input value to return the value without the delegated unit [CCL3] (or null)
	 * 
	 * @return
	 */
	public String getValueWithoutDelegateUnit() {
		if(value==null) return "";
		int index1 = value.lastIndexOf('(');
		int index2 = value.lastIndexOf(')');
		if(index1>0 && index1<index2) {
			return value.substring(0, index1).trim();
		}

		index1 = value.lastIndexOf('[');
		index2 = value.lastIndexOf(']');
		if(index1>0 && index1<index2) {
			return value.substring(0, index1).trim();
		}
		return value;
	}

	public void setResultDetail(ResultDetail resultDetail) {
		this.resultDetail = resultDetail;
	}

	public ResultDetail getResultDetail() {
		return resultDetail;
	}
	
	public Double getCalculatedValue() {
		return calculatedValue;
	}
	public void setCalculatedValue(Double calculatedValue) {
		this.calculatedValue = calculatedValue;
	}
	
	
	public Biosample getLinkedBiosample() {
		return linkedBiosample;
	}
	public void setLinkedBiosample(Biosample linkedBiosample) {
		this.linkedBiosample = linkedBiosample;
	}
	

	public static boolean isValidDouble(String value) {
		if(value==null || value.length()==0) return true;
		return value.equals(convertToValidDouble(value));
		
		

	}

	public static String convertToValidDoubles(String values) {
		if(values==null || values.length()==0) return null;
		
		String[] vals = values.split("; ", -1);
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<vals.length; i++) {
			if(i>0) sb.append("; ");
			
			String s = convertToValidDouble(vals[i]);
			sb.append(s==null?"": s);			
		}
		
		return sb.toString();
	}
	
	public static String convertToValidDouble(String value) {
		if(value==null || value.length()==0) return null;

		for(String s: validValues) {
			if(s.equals(value)) return value;
		}
		

		String[] allowedModifiers = new String[] {"<", "<=", ">", ">="};
		String doubleValue = value.trim();
		for(String mod: allowedModifiers) {
			if(value.startsWith(mod)) {
				doubleValue = value.substring(mod.length()).trim();
			}
		}
		
		try {
			Double.parseDouble(doubleValue);
			return value;
		} catch(Exception e) {
			if(value.length()>6) return value.replace(" ", " ");
			return "NA";
		}
	}
	
	public static List<String> getValues(Collection<ResultValue> resultValues) {
		List<String> res = new ArrayList<String>();
		for (ResultValue rv : resultValues) {
			res.add(rv.getValue());
		}
		return res;		
	}
	
//	public Compound getLinkedCompound() {
//		return linkedCompound;
//	}
//	public void setLinkedCompound(Compound linkedCompound) {
//		this.linkedCompound = linkedCompound;
//	}
}
