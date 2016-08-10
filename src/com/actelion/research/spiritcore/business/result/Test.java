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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RevisionTimestamp;

import com.actelion.research.spiritcore.business.IEntity;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;

@Entity
@Table(name="assay")
@SequenceGenerator(name="sequence", sequenceName="assay_seq", allocationSize=1)
@Audited
@AuditTable(value="assay_aud")
public class Test implements Comparable<Test>, IEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="sequence")
	@Column(name="assay_id")
	private int id = 0;

	@Column(name="category", length=20, unique=false, nullable=false)
	private String category;

	@Column(name="assay_name", length=50, unique=true, nullable=false)
	private String name;	
	
//	@Column(name="description", length=1000)
//	private String description;
	
	@Column(name="upd_user", nullable=false)
	private String updUser;
	
	@Column(name="cre_user", nullable=false)
	private String creUser;
	
	@Column(name="upd_date", nullable=false)
	@Temporal(TemporalType.DATE)
	@RevisionTimestamp
	private Date updDate;
	
	@Column(name="cre_date", nullable=false)
	@Temporal(TemporalType.DATE)
	private Date creDate;

	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="test", orphanRemoval=true)
	@Audited(targetAuditMode = RelationTargetAuditMode.AUDITED)
	@OrderBy(value="index")
	private Set<TestAttribute> attributes = new LinkedHashSet<>();	

	public Test() {
	}
	
	public Test(int id) {
		this.id = id;
	}
	
	public Test(String name) {
		setName(name);
	}
	
	
	@PrePersist @PreUpdate
	private void preUpdate() throws Exception {
		int index = 1;
		for (TestAttribute att : getAttributes()) {
			att.setIndex(index);
			att.setTest(this);
			index++;
		}
	}
	
	public Test duplicate() {
		Test test = new Test();
		test.setName(name);
//		test.setProjectName(projectName);
//		test.setDescription(description);
		test.setCategory(category);
		
		for (TestAttribute attribute : attributes) {
			TestAttribute att = new TestAttribute();
			att.setTest(attribute.getTest());
			att.setName(attribute.getName());
			att.setDataType(attribute.getDataType());
			att.setOutputType(attribute.getOutputType());
			att.setRequired(attribute.isRequired());
			test.getAttributes().add(att);
		}
		return test;
	}
	
	@Override
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

//	public String getProjectName() {
//		return projectName;
//	}
//
//	public void setProjectName(String projectName) {
//		this.projectName = projectName;
//	}
//
//	public String getDescription() {
//		return description;
//	}
//
//	public void setDescription(String comments) {
//		this.description = comments;
//	}

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

	public Set<TestAttribute> getAttributes() {
		return attributes;
	}

	public void addAttribute(TestAttribute attribute) {
		attributes.add(attribute);
	}

	public TestAttribute getAttribute(String name) {
		for (TestAttribute testAttribute : getAttributes()) {
			if(testAttribute.getName().equals(name)) return testAttribute;
		}
		return null;
	}

	public List<TestAttribute> getInputAttributes() {
		List<TestAttribute> res = new ArrayList<TestAttribute>();
		for (TestAttribute ta : getAttributes()) {
			if(ta.getOutputType()==OutputType.INPUT) {
				res.add(ta);
			}
		}
		return Collections.unmodifiableList(res);
	}
	public List<TestAttribute> getOutputAttributes() {
		List<TestAttribute> res = new ArrayList<TestAttribute>();
		for (TestAttribute ta : getAttributes()) {
			if(ta.getOutputType()==OutputType.OUTPUT) {
				res.add(ta);
			}
		}
		return Collections.unmodifiableList(res);
	}
	public List<TestAttribute> getInfoAttributes() {
		List<TestAttribute> res = new ArrayList<TestAttribute>();
		for (TestAttribute ta : getAttributes()) {
			if(ta.getOutputType()==OutputType.INFO) {
				res.add(ta);
			}
		}
		return Collections.unmodifiableList(res);
	}

	public void setAttributes(Set<TestAttribute> attributes) {
		this.attributes = attributes;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Test)) return false;
		if(getId()>0) return ((Test) obj).getId()==getId();
		return ((Test) obj).getName().equals(getName());
	}
	@Override
	public int compareTo(Test t) {

		if(t==null) return -1;
		if(t==this) return 0;
		
		int c = getCategory()==null? (t.getCategory()==null?0:-1): getCategory().compareToIgnoreCase(t.getCategory());
		if(c!=0) return c;
		
//		c = CompareUtils.compare(getName(), t.getName());
		c = getName()==null? (t.getName()==null?0:-1): getName().compareToIgnoreCase(t.getName());
		if(c!=0) return c;
		
		return 0;//(int)((getId()-t.getId())%Integer.MAX_VALUE);
	}

//	public void setTestType(TestType testType) {
//		this.assayType = testType;
//	}
//
//	public TestType getTestType() {
//		return assayType;
//	}
	
	public String getFullName() {		
		return  getCategory() + " - " + name;
	}
	public String getCategory() {
		return category; 
	}
	public void setCategory(String cat) {
		this.category = cat; 
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
	public Test clone() {
		Test test = new Test();
		test.attributes = attributes;
		test.category = category;
//		test.description = description;
		test.creDate = creDate;
		test.creUser = creUser;
		test.id = id;
		test.name = name;
		test.updDate = updDate;
		test.updUser = updUser;
		return test;
	}
	
	
	public static Map<String, Test> mapName(Collection<Test> tests) {
		Map<String, Test> map = new HashMap<>();
		if(tests==null) return map;
		for (Test t : tests) {			
			if(t.getName()==null) continue;
			map.put(t.getName(), t);
		}
		return map;
	}
	
	
	

}
