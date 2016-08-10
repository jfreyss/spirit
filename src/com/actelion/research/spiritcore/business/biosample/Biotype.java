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

package com.actelion.research.spiritcore.business.biosample;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RevisionTimestamp;

import com.actelion.research.spiritcore.business.IEntity;

@Entity
@Audited
@Table(name="biotype")
@Cacheable
@Cache(usage=CacheConcurrencyStrategy.NONE)
public class Biotype implements Serializable, Comparable<Biotype>, Cloneable, IEntity {

	public static final String ANIMAL = "Animal";
	public static final String ORGAN = "Organ";
	public static final String SLICE = "Slice";
	
	@Id
	@SequenceGenerator(name="biotype_sequence", sequenceName="biotype_sequence", allocationSize=1)
	@GeneratedValue(generator="biotype_sequence")
	@Column(name="id")
	private int id = 0;
	
	@Column(name="name", unique=true)
	private String name = "";
	
//	@Column(name="description")
//	private String description;
	
	@ManyToOne(fetch=FetchType.LAZY)	
	@JoinColumn(name="parent_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	private Biotype parent;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="parent")	
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	private Set<Biotype> children;
	
	@Column(nullable=false, name="cat")
	@Enumerated(EnumType.STRING)
	private BiotypeCategory category = null;
	
	@OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval=true, mappedBy="biotype")
	@OrderBy("index, name")
	@BatchSize(size=200) //to load all of them when needed (if not in cache)
	@Audited(targetAuditMode=RelationTargetAuditMode.AUDITED)
	private Set<BiotypeMetadata> metadata = new LinkedHashSet<>();
	
	/**
	 * 
	 */
	@Column(name="namelabel", length=30, nullable=true)
	private String sampleNameLabel = "Name";
	
	private Boolean nameRequired = Boolean.FALSE;
	
	private Boolean nameAutocomplete = Boolean.FALSE;
	
	private Boolean hideSampleId = Boolean.TRUE;

	private Boolean hideContainer = Boolean.FALSE;

	/**
	 * The barcode prefix
	 */
	private String prefix = null;
		
	/**
	 * If an amount has to be stored and in which unit
	 */
	@Enumerated(EnumType.STRING)
	@Column(name="amountunit", nullable=true, length=10)
	private AmountUnit amountUnit;
	

	/**
	 * The default containerType (if any)
	 */
	@Enumerated(EnumType.STRING)
	private ContainerType containerType;
	
	/**
	 * An abstract biotype cannot have containers/locations
	 */
	private boolean isAbstract = false;
	
	/**
	 * A hidden biotype can only be seen by an admin
	 */
	private boolean isHidden = false;
	
	private String updUser;
	
	private String creUser;

	@Temporal(TemporalType.TIMESTAMP)
	private Date creDate = new Date();

	@RevisionTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private Date updDate = new Date();
	
	
	/**Attributes used for displaying the hierarchy*/
	private transient int depth;

	public Biotype() {
		
	}
	
	@PrePersist	
	void preUpdate() {
		Set<BiotypeMetadata> mts = metadata;
		metadata = new TreeSet<BiotypeMetadata>();
		int count = 0;
		for (BiotypeMetadata m : mts) {
			m.setIndex(count++);
			metadata.add(m);
		}
	}

	@Override
	public int getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public void setId(int id) {
		this.id = id;
	}
	public void setName(String name) {
		this.name = name;
	}

	public BiotypeCategory getCategory() {
		return category;
	}

	public void setCategory(BiotypeCategory category) {
		this.category = category;
	}

	public void setMetadata(Set<BiotypeMetadata> metadata) {
		this.metadata = metadata;
	}
	public Set<BiotypeMetadata> getMetadata() {
		return metadata;
	}
	
	public BiotypeMetadata getMetadata(String name) {
		for (BiotypeMetadata bm : getMetadata()) {
			if(bm.getName().equals(name)) return bm;
		}
		return null;
	}
	
	public BiotypeMetadata getMetadata(int id) {
		for (BiotypeMetadata bm : getMetadata()) {
			if(bm.getId()==id) return bm;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return name;
	}

	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Biotype)) return false;
		if(this==obj) return true;
		Biotype t2 = (Biotype) obj;

		if(getId()>0 && t2.getId()>0) {
			return getId()==t2.getId();
		} else {
			return (getName()==null && t2.getName()==null) || (getName()!=null && getName().equals(t2.getName()));
		}
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}
	
	private List<Biotype> getAncestors() {
		LinkedList<Biotype> res = new LinkedList<Biotype>();
		Biotype b = this;
		while(b!=null) {
			res.addFirst(b);
			b = b.getParent();
		}
		return res;
	}

	@Override
	public int compareTo(Biotype o) {
		if(this.equals(o)) return 0;
		
		List<Biotype> l1 = getAncestors();
		List<Biotype> l2 = o.getAncestors();
		
		for (int i = 0; i<l1.size() || i<l2.size(); i++) {
			if(i>=l1.size()) return -1;
			if(i>=l2.size()) return 1;
			
			Biotype b1 = l1.get(i);
			Biotype b2 = l2.get(i);
			
			int c = b1.isHidden()? (b2.isHidden()?0: 1): (b2.isHidden()?-1: 0);
			if(c!=0) return c;
			
			c = b1.getCategory()==null? (b2.getCategory()==null?0: 1): b1.getCategory().compareTo(b2.getCategory());
			if(c!=0) return c;
			
			c = b1.getName().compareToIgnoreCase(b2.getName());
			if(c!=0) return c;
		}
		return 0;
		
	}

//	/**
//	 * @param description the description to set
//	 */
//	public void setDescription(String description) {
//		this.description = description;
//	}
//
//	/**
//	 * @return the description
//	 */
//	public String getDescription() {
//		return description;
//	}

	/**
	 * @param amountUnit the amountUnit to set
	 */
	public void setAmountUnit(AmountUnit amountUnit) {
		this.amountUnit = amountUnit;
	}

	/**
	 * @return the amountUnit
	 */
	public AmountUnit getAmountUnit() {
		return amountUnit;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(Biotype parent) {
		this.parent = parent;
	}

	/**
	 * @return the parent
	 */
	public Biotype getParent() {
		return parent;
	}
	
	/**
	 * @return the parent
	 */
	public Biotype getTopParent() {
		Biotype top = this;
		while(top.getParent()!=null) {
			top = top.getParent();
		}
		return top;
	}


	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	@PreRemove
	public void remove() {
		parent = null;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public ContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ContainerType containerType) {
		this.containerType = containerType;
	}

//	public boolean isAliquotType() {
//		return aliquotType;
//	}
//	public void setAliquotType(boolean aliquotType) {
//		this.aliquotType = aliquotType;
//	}

//	@Override
//	public Biotype clone() {
//		try {
//			return (Biotype) super.clone();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

	/**
	 * The label is the name given to the main attribute, used to define the sample.
	 * If null, then the sample has no specific label
	 * @param nameLabel the nameLabel to set
	 */
	public void setSampleNameLabel(String sampleNameLabel) {
		this.sampleNameLabel = (sampleNameLabel!=null && sampleNameLabel.length()==0)? null : sampleNameLabel;
	}

	/**
	 * Can be null if the sample has no specific label
	 * @return the nameLabel
	 */
	public String getSampleNameLabel() {
		return sampleNameLabel==null || sampleNameLabel.length()==0? null: sampleNameLabel;
	}

	/**
	 * @param isHidden the isHidden to set
	 */
	public void setHidden(boolean isHidden) {
		this.isHidden = isHidden;
	}

	/**
	 * @return the isHidden
	 */
	public boolean isHidden() {
		return isHidden;
	}
	@Override
	public int hashCode() {
		return id>0? (int)(id%Integer.MAX_VALUE): name==null?0: name.hashCode();
	}
	
	public void setNameAutocomplete(boolean nameAutocomplete) {
		this.nameAutocomplete = nameAutocomplete;
	}
	
	public boolean isNameAutocomplete() {
		return nameAutocomplete == Boolean.TRUE;
	}
	
	public void setNameRequired(boolean nameAutocomplete) {
		this.nameRequired = nameAutocomplete;
	}
	
	public boolean isNameRequired() {
		return nameRequired == Boolean.TRUE;
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

	public Date getCreDate() {
		return creDate;
	}

	public void setCreDate(Date creDate) {
		this.creDate = creDate;
	}

	public Date getUpdDate() {
		return updDate;
	}

	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
	}
	
	public void setHideSampleId(boolean hideSampleId) {
		this.hideSampleId = hideSampleId;
	}
	
	public boolean isHideSampleId() {
		return hideSampleId==Boolean.TRUE;
	}
	
	public void setHideContainer(boolean hideContainer) {
		this.hideContainer = hideContainer;
	}
	
	public boolean isHideContainer() {
		return hideContainer==Boolean.TRUE;
	}

	public Set<Biotype> getChildren() {
		return children;
	}
	
	/**
	 * 
	 * @param biotypes
	 * @param cat
	 * @return
	 */
	public static List<Biotype> filter(Collection<Biotype> biotypes, BiotypeCategory cat){
		List<Biotype> res  = new ArrayList<>();
		if(biotypes==null) return res;
		for (Biotype biotype : biotypes) {
			if(biotype.getCategory()==cat) res.add(biotype);
		}
		return res;
	}
	
	/**
	 * Removes the abstract biotypes, and returns a new list
	 * @param biotypes
	 * @return
	 */
	public static List<Biotype> removeAbstract(Collection<Biotype> biotypes) {
		List<Biotype> res  = new ArrayList<>();
		if(biotypes==null) return res;
		for (Biotype biotype : biotypes) {
			if(!biotype.isAbstract) res.add(biotype);
		}
		return res;
	}
	
	/**
	 * Removes the hidden biotypes, and returns a new list
	 * @param biotypes
	 * @return
	 */
	public static List<Biotype> removeHidden(Collection<Biotype> biotypes) {
		List<Biotype> res  = new ArrayList<>();
		if(biotypes==null) return res;
		for (Biotype biotype : biotypes) {
			if(!biotype.isHidden()) res.add(biotype);
		}
		return res;
	}

	public static Map<String, Biotype> mapName(Collection<Biotype> biotypes){
		Map<String, Biotype> res = new HashMap<>();
		if(biotypes==null) return res;
		for (Biotype s : biotypes) {
			res.put(s.getName(), s);
		}
		return res;
	}
	
	/**
	 * Used for debug
	 * @return
	 */
	public String getMetadataIds() {
		String s = "";
		for (BiotypeMetadata bm : getMetadata()) {
			s+="["+bm.getId()+"]"+bm.getName()+",";
		}
		return s.length()==0?"": s.substring(0, s.length()-1);
	}
	
}