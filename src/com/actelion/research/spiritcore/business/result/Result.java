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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RevisionNumber;

import com.actelion.research.spiritcore.business.IEntity;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.util.CompareUtils;

@Entity
@Table(name="assay_result", indexes = {		
		@Index(name="assay_result_elb_idx", columnList = "elb"),
		@Index(name="assay_result_biosample_idx", columnList = "biosample_id"),
		@Index(name="result_test_idx", columnList = "assay_id"),
		@Index(name="assay_result_phase_idx", columnList = "phase_id")})
@SequenceGenerator(name="assay_result_seq", sequenceName="assay_result_seq", allocationSize=1)
@Audited
public class Result implements Comparable<Result>, IEntity, Cloneable {
		
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="assay_result_seq")
	@RevisionNumber
	@Column(name="assay_result_id")
	private int id = 0;
	
	private String elb;
	
	@Column(length=1000)
	private String comments;

	@Column(name="upd_user", nullable=false)
	private String updUser;
	
	@Column(name="cre_user", nullable=false)
	private String creUser;
	
	@Column(name="upd_date", nullable=false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date updDate;
	
	@Column(name="cre_date", nullable=false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date creDate;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="biosample_id", nullable=true)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	@BatchSize(size=100)
	private Biosample biosample;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="assay_id", nullable=false)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	private Test test;
	
	/**
	 * The phase is only used if the biosample has no phase
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="phase_id", nullable=true)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	@BatchSize(size=100)
	private Phase phase;

	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="result", orphanRemoval=true)	
	@MapKey(name="attribute")
	@BatchSize(size=500)
	private Map<TestAttribute, ResultValue> values = new HashMap<>();
	
	private Quality quality = Quality.VALID;
	
	public Result() {
	}
	public Result(Test test) {
		this.test = test;
	}
	
	
	/**
	 * sets the uppDate to the current datetime automatically before persist or update
	 */
	@PrePersist @PreUpdate
	private void updateUpdDate() throws Exception {
		
		//Check if samples are present
		if(biosample==null) throw new Exception("Biosample is required");
		if(test==null) throw new Exception("The test cannot be null");
		
		//Check if values have an appropriate id
		Set<Integer> attIds = new TreeSet<Integer>();
		for (TestAttribute att : test.getAttributes()) {
			attIds.add(att.getId());
		}
		
		Set<Integer> seen = new TreeSet<Integer>();
		for (ResultValue v : getResultValues()) {
			if(!attIds.contains(v.getAttribute().getId())) {
				throw new Exception("The attribute "+v.getAttribute()+" is not attached to the test "+test+": "+test.getAttributes()+ "!!");
			}
			if(seen.contains(v.getAttribute().getId())) {
				throw new Exception("The attribute "+v.getAttribute()+" is present 2 times!!");
			}
		}		
		
	}
	
	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getElb() {
		return elb;
	}

	public void setElb(String elb) {
		this.elb = elb;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments==null? null: comments.trim();
	}

	public String getUpdUser() {
		return updUser;
	}

	public void setUpdUser(String updUser) {
		this.updUser = updUser;
	}

	public Date getUpdDate() {
		return updDate;
	}

	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
	}

	public Biosample getBiosample() {
		return biosample;
	}

	public void setBiosample(Biosample biosample) {
		this.biosample = biosample;
	}

	public Test getTest() {
		return test;
	}

	public void setTest(Test test) {		
		if(test==null) {
			values.clear();			
		} else {
			Map<TestAttribute, ResultValue> newValues = new HashMap<TestAttribute, ResultValue>();
			if(this.test!=null && !this.test.equals(test)) {
				
				//Update the test, but keep the same input/output values if possible
				for (int i = 0; i < test.getInputAttributes().size(); i++) {
					TestAttribute ta = test.getInputAttributes().get(i);
					if(i<this.getInputResultValues().size()) {
						ResultValue rv = this.getInputResultValues().get(i);
						newValues.put(ta, new ResultValue(this, ta, rv.getValue()));
					}
				}
				for (int i = 0; i < test.getOutputAttributes().size(); i++) {
					TestAttribute ta = test.getOutputAttributes().get(i);
					if(i<this.getOutputResultValues().size()) {
						ResultValue rv = this.getOutputResultValues().get(i);
						newValues.put(ta, new ResultValue(this, ta, rv.getValue()));
					}
				}
				
				//Reset the values
				values.clear();
				values.putAll(newValues);
			}
		}
		this.test = test;
		
		if(test!=null) {
			for (TestAttribute ta: test.getAttributes()) {
				getResultValue(ta);
			}
		}
		
	}

	public ResultValue remove(TestAttribute att) {
		assert att!=null;
		return values.remove(att);
	}
	public Collection<ResultValue> getResultValues() {
		return values.values();
	}
	public Map<TestAttribute, ResultValue> getResultValueMap() {
		return values;
	}
	public void setResultValueMap(Map<TestAttribute, ResultValue> values) {
		this.values = values;
	}

	public ResultValue getResultValue(TestAttribute att) {
		assert test!=null;
		assert att!=null;		
		assert test.equals(att.getTest()): test +"("+test.getId()+")" + " is not equal to "+att.getTest()+"("+att.getTest().getId()+") for result.id= "+getId();
		
		ResultValue v = values.get(att);
		if(v==null) {
			//hack, check if id (hashcode) has been changed 
			for (TestAttribute ta : values.keySet()) {
				if(ta.equals(att)) {
					values = new HashMap<>(values);
					return values.get(ta);
				}
			}
			//end-hack
			v = new ResultValue(this, att, "");
			values.put(att, v);
		}
		return v;
	}
	
	public ResultValue getResultValue(String attName) {
		assert test!=null;
		assert attName!=null;
		
		for (ResultValue v : values.values()) {
			if(v.getAttribute().getName().equals(attName)) return v;
		}
		
		//Check that the attribute is valid
		for (TestAttribute a : test.getAttributes()) {
			System.err.println("Recreate resultvalue for "+attName);
			if(a.getName().equals(attName)) {
				ResultValue v = new ResultValue(this, a, "");
				values.put(a, v);
				return v;				
			}			
		}
		return null;
	}
	
	
	public void setValue(TestAttribute att, String val) {
		assert att!=null;
		ResultValue value = getResultValue(att);
		assert value!=null: att+" is invalid. Valid: "+test.getAttributes();
		value.setValue(val);
	}	
	
	public void setValue(String attName, String val) {
		setValue(getTest().getAttribute(attName), val);
	}	
	
	public List<ResultValue> getInputResultValues(){
		List<TestAttribute> atts = test.getInputAttributes();
		List<ResultValue> res = new ArrayList<>();
		for (TestAttribute att : atts) {
			res.add(getResultValue(att));
		}
		return Collections.unmodifiableList(res);
	}
	
	/**
	 * Get the list of input (space separated)
	 * @return
	 */
	public String getInputResultValuesAsString(){
		if(test==null) return "No test";
		StringBuilder sb = new StringBuilder();
		for (TestAttribute att : test.getInputAttributes()) {
			if(sb.length()>0) sb.append(" ");
			if(getResultValue(att).getValue()!=null) {
				sb.append(getResultValue(att).getValue());
			}
		}
		return sb.toString();
	}
	
	public boolean isEmpty(){
		if(test==null) return true;
		if(getBiosample()!=null) return false;
		for (TestAttribute att : test.getAttributes()) {
			if(getResultValue(att)!=null && getResultValue(att).getValue()!=null && getResultValue(att).getValue().length()>0) return false;
		}
		if(getComments()!=null && getComments().length()>0) return false;
		return true;
	}

	/**
	 * Convenience value to return the first output value.
	 * Either as a double or as as string
	 * @return
	 */
	public String getFirstValue() {
		for(TestAttribute ta: getTest().getAttributes()) {
			if(ta.getOutputType()!=OutputType.OUTPUT) continue;
			ResultValue rv = getResultValue(ta);
			if(rv==null) return null;
//			if(rv.getDoubleValue()!=null) return rv.getDoubleValue();
			return rv.getValue();
		}
		return null;
	}
	public void setFirstOutputValue(String val) {
		for(TestAttribute ta: getTest().getAttributes()) {
			if(ta.getOutputType()!=OutputType.OUTPUT) continue;
			setValue(ta, val);
			return;
		}
	}

	public Double getFirstAsDouble() {
		for(TestAttribute ta: getTest().getAttributes()) {
			if(ta.getOutputType()!=OutputType.OUTPUT) continue;
			ResultValue rv = getResultValue(ta);
			if(rv==null) return null;
			if(rv.getDoubleValue()!=null) return rv.getDoubleValue();
			return null;
		}
		return null;
	}

	public String getOutputResultValuesAsString(){
		if(test==null) return "No test";
		List<TestAttribute> atts = test.getOutputAttributes();
		StringBuilder sb = new StringBuilder();
		for (TestAttribute att : atts) {
			if(getResultValue(att)==null || getResultValue(att).getValue()==null  || getResultValue(att).getValue().length()==0) continue;
			if(sb.length()>0) sb.append(", ");
			sb.append(att.getName()+"="+getResultValue(att).getValue());
		}
		if(comments!=null && comments.length()>0) {
			if(sb.length()>0) sb.append(" / ");
			sb.append(comments);
		}
		return sb.toString();
	}	
	
	public List<ResultValue> getOutputResultValues() {
		List<TestAttribute> atts = test.getOutputAttributes();
		List<ResultValue> res = new ArrayList<>();
		for (TestAttribute att : atts) {
			res.add(getResultValue(att));
		}		
		return Collections.unmodifiableList(res);		
	}	

	/**
	 * Sets the phase of the result.
	 * Note: the phase should be null if the biosample has a phase to avoid redundancies.
	 * @param phase
	 */
	public void setPhase(Phase phase) {
		this.phase = phase;
	}
	
	public Phase getPhase() {
		return phase;
	}
	
	public Phase getInheritedPhase() {
		return getPhase()!=null? getPhase(): getBiosample()!=null? getBiosample().getInheritedPhase(): null;
	}
	
	public Study getStudy() {
		return getBiosample()!=null? getBiosample().getInheritedStudy(): null;
	}
	

	//////////// UTIL
	
	@Override
	public int compareTo(Result r) {
		int c;
		c = CompareUtils.compare(getTest(), r.getTest());
		if(c!=0) return c;

		c = CompareUtils.compare(getBiosample(), r.getBiosample());
		if(c!=0) return c;

		c = CompareUtils.compare(getPhase(), r.getPhase());
		if(c!=0) return c;

		c = CompareUtils.compare(getElb(), r.getElb());
		if(c!=0) return c;

		c = CompareUtils.compare(getId(), r.getId());
		return c;
	}
	
	public void setQuality(Quality quality) {
		this.quality = quality;
	}
	public Quality getQuality() {
		return quality==null? Quality.VALID: quality;
	}
	
	@Override
	public String toString() {
		return  "[Result:"
				+ " biosample=" + biosample
				+ " phase=" + phase
				+ " input=" + getInputResultValuesAsString()
				+ " output=" + getOutputResultValuesAsString();		
	}
	
	public String getDetailsWithoutSampleId() {
		return (getTest()==null?"": getTest().getName()+" ")
				+ (phase==null?"": " " + phase.getShortName() + " ")
				+ getInputResultValuesAsString()
				+ ": " + getOutputResultValuesAsString();		
	}
	
	
	/**
	 * @param creDate the creDate to set
	 */
	public void setCreDate(Date creDate) {
		this.creDate = creDate;
	}
	/**
	 * @return the creDate
	 */
	public Date getCreDate() {
		return creDate;
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
	public Result clone() {
		try {
			Result res = new Result();
			res.biosample = biosample;
			res.comments = comments;
			res.creDate = creDate;
			res.creUser = creUser;
			res.elb = elb;
			res.id = id;
			res.phase = phase;
			res.quality = quality;
			res.test = test;
			res.updDate = updDate;
			res.updUser = updUser;
			res.values = values;
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	/**
	 * @return the biosample.group
	 */
	public Group getGroup() {
		return getBiosample()==null? null: getBiosample().getInheritedGroup();
	}
	
	/**
	 * @return the subgroup
	 */
	public int getSubGroup() {
		return getBiosample()==null? -1: getBiosample().getInheritedSubGroup();
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null) return false;
		if(!(obj instanceof Result)) return false;
		if(this==obj) return true;
		Result r2 = (Result) obj;
		if(getId()>0) {
			return getId()== r2.getId();
		} else {
			if((getPhase()==null && r2.getPhase()!=null) || (getPhase()!=null && !getPhase().equals(r2.getPhase()))) return false;
			if((getBiosample()==null && r2.getBiosample()!=null) || (getBiosample()!=null && !getBiosample().equals(r2.getBiosample()))) return false;
			if((getTest()==null && r2.getTest()!=null) || (getTest()!=null && !getTest().equals(r2.getTest()))) return false;
			if(!getInputResultValuesAsString().equals(r2.getInputResultValuesAsString())) return false;
			
			//Same test, phase, sample, input. This is either the same or a duplicate
			return true;
		}
	}
	
	
	
	public static Set<Test> getTests(Collection<Result> results) {
		Set<Test> res = new HashSet<Test>();
		for (Result r : results) {
			if(r.getTest()!=null) res.add(r.getTest());
		}
		return res;
	}
	
	public static Set<String> getElbs(Collection<Result> results) {
		Set<String> res = new HashSet<String>();
		for (Result r : results) {
			if(r.getElb()!=null) res.add(r.getElb());
		}
		return res;
	}
	
	public static Set<Biosample> getBiosamples(Collection<Result> results) {
		Set<Biosample> res = new HashSet<Biosample>();
		for (Result r : results) {
			if(r.getBiosample()!=null) res.add(r.getBiosample());
		}
		return res;
	}
	
	public static Result getPrevious(Result current, List<Result> from) {
		assert current!=null;
		assert current.getInheritedPhase()!=null;
		assert from!=null;
		
		Result sel = null;
		for (Result r : from) {
			if(r.getInheritedPhase()==null) continue;
			if(current.getBiosample()!=null && !current.getBiosample().equals(r.getBiosample())) continue;
			if(!r.getTest().equals(current.getTest())) continue;
			if(!r.getStudy().equals(current.getStudy())) continue;
			if(r.getInheritedPhase().compareTo(current.getInheritedPhase())>=0) continue;
			if(r.getFirstValue()==null) continue;
			
			if(sel==null) {
				sel = r;
			} else if(r.getInheritedPhase().compareTo(sel.getInheritedPhase())>0) {
				sel = r;
			}
		}
		
		return sel;
	}
	
	public static Result getFirst(Result current, List<Result> from) {
		assert current!=null;
		assert current.getInheritedPhase()!=null;
		assert from!=null;
				
		Result sel = null;
		for (Result r : from) {
			if(r.getInheritedPhase()==null) continue;
			if(current.getBiosample()!=null && !current.getBiosample().equals(r.getBiosample())) continue;
			if(!r.getTest().equals(current.getTest())) continue;
			if(!r.getStudy().equals(current.getStudy())) continue;
			if(r.getInheritedPhase().compareTo(current.getInheritedPhase())>=0) continue;
			if(r.getFirstValue()==null) continue;
			
			if(sel==null) {
				sel = r;
			} else if(r.getInheritedPhase().compareTo(sel.getInheritedPhase())<0) {
				sel = r;
			}
		}
		
		return sel;
	}
	
	public static Map<Test, List<Result>> mapTest(Collection<Result> col) {
		Map<Test, List<Result>> map = new LinkedHashMap<>();
		if(col==null) return map;
		for (Result b : col) {			
			List<Result> l = map.get(b.getTest());
			if(l==null) {
				l = new ArrayList<>();
				map.put(b.getTest(), l);
			}
			l.add(b);			
		}
		return map;
	}
	
	public static Map<Biosample, List<Result>> mapBiosample(Collection<Result> col) {
		Map<Biosample, List<Result>> map = new HashMap<>();
		if(col==null) return map;
		for (Result b : col) {			
//			if(b.getTest()==null) continue;
			List<Result> l = map.get(b.getBiosample());
			if(l==null) {
				l = new ArrayList<>();
				map.put(b.getBiosample(), l);
			}
			l.add(b);			
		}
		return map;
	}
	
	public static Map<Study, List<Result>> mapStudy(Collection<Result> col) {
		Map<Study, List<Result>> map = new HashMap<>();
		if(col==null) return map;
		for (Result b : col) {			
//			if(b.getStudy()==null) continue;
			List<Result> l = map.get(b.getStudy());
			if(l==null) {
				l = new ArrayList<>();
				map.put(b.getStudy(), l);
			}
			l.add(b);			
		}
		return map;
	}
	
	
	public final static Comparator<Result> COMPARATOR_UPDDATE = new Comparator<Result>() {		
		@Override
		public int compare(Result o1, Result o2) {
			if(o2==null) return 1;
			return -CompareUtils.compare(o1.getUpdDate(), o2.getUpdDate());
		}
	};
	

	/**
	 * Returns true if adding the phase could help to discriminate the results  
	 * @param results
	 * @return
	 */
	public static boolean isPhaseDependant(Collection<Result> results) {
		ListHashMap<String, Phase> key2phases = new ListHashMap<>();
		for (Result r : results) {
			if(r.getInheritedPhase()==null) continue;
			if(r.getBiosample()==null) continue;
			
			String key = r.getBiosample().getTopParent().getId() + "_" + r.getTest().getId() + "_" +r.getInputResultValues();
			key2phases.add(key, r.getInheritedPhase());
			System.out.println("Result.isPhaseDependant() "+key+">"+key2phases.get(key).size());
			if(key2phases.get(key).size()>1) return true;
		}
		return false;
	}
	

	public static List<Result> filter(Collection<Result> results, Test test) {
		if(test==null) return null;
		List<Result> res = new ArrayList<>();
		for (Result result : results) {
			if(test.equals(result.getTest())) {
				res.add(result);
			}
		}
		return res;
	}
	
	
	/**
	 * Return a key such as result1.key=result2.key only if result2 is a duplicate of result1
	 * This function does not use database ids to be independant of the instance
	 * @return
	 */
	public String getTestBiosamplePhaseInputKey() {
		return getTest().getName()
				+ "_" + (getBiosample()==null?"": getBiosample().getSampleId())
				+ "_" + (getPhase()==null?"": getPhase().getShortName())
				+ "_" + getInputResultValuesAsString();
	}

	public String debugInfo() {
		return "[Res:"+id+(biosample==null?"":",biosample="+biosample.debugInfo())+(phase==null || phase.getStudy()==null?"":",study="+phase.getStudy().getId())+"]";
	}

}
