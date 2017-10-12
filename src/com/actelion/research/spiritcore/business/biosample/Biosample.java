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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PreRemove;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RevisionNumber;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.spiritcore.util.SetHashMap;
import com.actelion.research.util.CompareUtils;

/**
 * Class representing a Biosample.<br>
 * The biosample entity encapsultates the metadata, the main history action and the container.
 *
 *
 * Note: Eager loading and FetchMode=Join should not be done here. All optimizations have to be done through the DAO
 *
 */
@Entity
@Audited
@Table(name="biosample", uniqueConstraints = {
		@UniqueConstraint(name="biosample_sampleId_index", columnNames = "sampleid"),
},
indexes = {
		@Index(name="biosample_biotype_index", columnList = "biotype_id"),
		@Index(name="biosample_topParent_index", columnList = "topparent_id"),
		@Index(name="biosample_parent_index", columnList = "parent_id"),
		@Index(name="biosample_container_index", columnList = "containerid"),
		@Index(name="biosample_location_index", columnList = "location_id"),
		@Index(name="biosample_attstudy_index", columnList = "attachedstudy_id"),
		@Index(name="biosample_study_index", columnList = "study_id"),
		@Index(name="inheritedgroup_index", columnList = "inheritedgroup_id"),
		@Index(name="inheritedphase_index", columnList = "inheritedphase_id"),
		@Index(name="attachedsampling_index", columnList = "attachedsampling_id"),
		@Index(name="department_index", columnList = "department_id"),
		@Index(name="biosample_creuser_index", columnList = "creuser"),
		@Index(name="biosample_credate_index", columnList = "credate"),
		@Index(name="biosample_upduser_index", columnList = "updUser"),
		@Index(name="biosample_upddate_index", columnList = "updDate"),
		@Index(name="biosample_elb_index", columnList = "elb")})
@BatchSize(size=64)
public class Biosample implements Serializable, Comparable<Biosample>, Cloneable, IObject, IAuditable {

	public static final String AUX_RESULT_ALL = "AllResult";

	@Id
	@SequenceGenerator(name="biosample_sequence", sequenceName="biosample_sequence", allocationSize=1)
	@GeneratedValue(generator="biosample_sequence")
	@RevisionNumber
	private int id = 0;

	@Column(name="sampleid")
	private String sampleId = "";

	/**
	 * Non Unique Id, user-readable (only if biotype.nameLabel is not null)
	 */
	@Column(name="localid", length=64)
	private String name;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="biotype_id", nullable=false)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private Biotype biotype = null;

	/**
	 * Top biosample of the branch. Used for making queries faster
	 * should not be null
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@JoinColumn(name="topparent_id")
	private Biosample topParent;

	/**
	 * Parent
	 */
	@ManyToOne(fetch=FetchType.LAZY, cascade=CascadeType.REFRESH, optional=true)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@JoinColumn(name="parent_id")
	private Biosample parent;

	/**
	 * Children, mapped by the parent biosample
	 */
	@OneToMany(fetch=FetchType.LAZY, cascade=CascadeType.REFRESH, mappedBy="parent")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@BatchSize(size=32)
	private Set<Biosample> children = new TreeSet<>();

	/**
	 * Serialized metadata Spirit v2
	 */
	@Column(name="metadata", length=4000)
	private String serializedMetadata;

	@Column(name="metadata2", length=4000)
	private String serializedMetadata2;

	@OneToMany(fetch=FetchType.LAZY, cascade=CascadeType.REFRESH)
	@JoinTable(name="biosample_biosample", joinColumns=@JoinColumn(name="biosample_id"), inverseJoinColumns=@JoinColumn(name="linkedbiosample_id"))
	@MapKeyJoinColumn(name="biotypemetadata_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@BatchSize(size=32)
	private Map<BiotypeMetadata, Biosample> linkedBiosamples = new HashMap<>();


	//Buggy code: Envers (Hibernate<=5.2) is not able to retrieve data due to a wrong join on biotypemetadata.
	//To fix it, we use an integer for the key
	@OneToMany(fetch=FetchType.LAZY, cascade=CascadeType.ALL)
	@JoinTable(name="biosample_document", joinColumns=@JoinColumn(name="biosample_id"), inverseJoinColumns=@JoinColumn(name="linkeddocument_id"))
	@MapKeyColumn(name="biotypemetadata_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.AUDITED)
	@BatchSize(size=32)
	private Map<Integer, Document> linkedDocuments = new HashMap<>();


	/**
	 * Location, only if a container is specified. All biosamples within the container should share the same location and position
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="location_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private Location location;

	@Column(name="location_pos")
	private Integer pos = -1;

	/**
	 * The sample can be attached to a study (but no groups)
	 * ex: human -> blood -> aliquot1, aliquot2,
	 * human can be attached to the study but no group
	 * aliquot1 can be attached to group A
	 * aliquot2 can be attached to group B
	 *
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@JoinColumn(name="attachedstudy_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private Study attachedStudy;

	/**
	 * The inheritedStudy
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@JoinColumn(name="study_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private Study inheritedStudy;

	/**
	 * The inherited group is used for fast searching. It has to be maintained  by the developer, so that
	 * if(attachedGroup!=null) inheritedGroup = attachedGroup
	 * else if(parent.inheritedGroup!=null) inheritedGroup = parent.inheritedGroup
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@JoinColumn(name="inheritedgroup_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private Group inheritedGroup = null;

	/**
	 * In case of stratification, the sample belongs to the attachedStudy/inheritedGroup/attachedSubgroup
	 */
	@Column(name="inheritedsubgroup")
	private Integer inheritedSubgroup = 0;

	/**
	 * The phase is null except if the sample was sampled at this given timepoint in the study
	 */
	@ManyToOne(fetch=FetchType.LAZY/*, cascade=CascadeType.PERSIST*/) //Cascade persist, because AddSampling can add a phase
	@OnDelete(action=OnDeleteAction.CASCADE)
	@JoinColumn(name="inheritedphase_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private Phase inheritedPhase = null;

	/**
	 * The attached sampling (if the sampling was created from the study->sampling function
	 * This is to ensure that modifying a sampling, will edit the related samples
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="attachedsampling_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.NO_ACTION)
	private Sampling attachedSampling = null;

	/**
	 * The lastAction is used to record the last noticeable actions (weighing, treatment)
	 */
	@Column(name="lastaction", length=256)
	private String lastAction;

	/**
	 * Exceptional handling, if this biosample does not go through the whole lifetime of the study.
	 * This allows to keep the sample in the group, while ignoring all future actions
	 */
	@ManyToOne(fetch=FetchType.LAZY/*, cascade=CascadeType.PERSIST*/) //Cascade persist, because AddSampling can add a phase
	@OnDelete(action=OnDeleteAction.CASCADE)
	@JoinColumn(name="endphase_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private Phase endPhase;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="department_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private EmployeeGroup group = null;

	/**
	 * Amount of the biosample
	 * (Note: should be moved to container)
	 */
	@Column(precision=6, scale=3)
	private Double amount;

	@Column(length=20)
	private String updUser;

	@Column(length=20)
	private String creUser;

	@Temporal(TemporalType.TIMESTAMP)
	private Date creDate;

	@Temporal(TemporalType.TIMESTAMP)
	private Date updDate;

	@Temporal(TemporalType.TIMESTAMP)
	private Date expiryDate;

	@Column(length=20)
	private String elb;

	/**Comments*/
	@Column(length=256)
	private String comments;

	/**Quality*/
	@Column(name="quality", nullable=true)
	@Enumerated(EnumType.ORDINAL)
	private Quality quality = Quality.VALID;

	/**Status*/
	@Column(name="state", nullable=true)
	@Enumerated(EnumType.STRING)
	private Status status = Status.INLAB;

	/**Container, to be specified when the containertype is multiple only, it should be null otherwise*/
	@Embedded
	private Container container;

	@Column(precision=3,name="container_index")
	private Integer containerIndex;

	/**If there was a scan */
	private transient String scannedPosition;

	private transient Map<BiotypeMetadata, String> metadataValues = null;

	/**Auxiliary infos that can be used for internal code
	 * This field is transient, meaning that the developer is responsible for the storage
	 * The values can be null
	 */
	private transient final Map<String, Object> infos = new HashMap<>();

	public static enum HierarchyMode {
		/**All parents and all children*/
		ALL,
		/**All parents and all children (depth of 2 max)*/
		ALL_MAX2,
		/**return parents(excluding this)*/
		PARENTS,
		/**Includes this + dividing samples and attached samples*/
		AS_STUDY_DESIGN,
		/**Includes this + attached samples, stop at dividing samples*/
		ATTACHED_SAMPLES,
		/**CHILDREN excludes 'this' */
		CHILDREN,
		/**CHILDREN_NOT_ATTACHED = CHILDREN minus all attached*/
		CHILDREN_NOT_ATTACHED,
		SIEBLINGS,
		TERMINAL
	}

	public Biosample() {
		this(0);
	}

	public Biosample(int id) {
		this.id = id;
	}

	public Biosample(String sampleId) {
		this();
		this.sampleId = sampleId;
	}

	public Biosample(Biotype type) {
		this();
		setBiotype(type);
	}

	public Biosample(Biotype type, String sampleId) {
		this.biotype = type;
		this.sampleId = sampleId;
	}

	@Override
	public String toString() {
		return sampleId==null?"": sampleId;
	}

	@Override
	public int getId() {
		return id;
	}

	public Biotype getBiotype() {
		return biotype;
	}

	public String getSampleId() {
		return sampleId;
	}

	public String getComments() {
		return comments;
	}

	public String getUpdUser() {
		return updUser;
	}

	public Date getCreDate() {
		return creDate;
	}

	public Date getUpdDate() {
		return updDate;
	}

	public Date getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	public void setBiotype(Biotype biotype) {
		this.biotype = biotype;
		if(biotype!=null && biotype.getContainerType()!=null && getContainerType()==null) {
			if(biotype.isAbstract()) {
				setContainerType(null);
			} else {
				setContainerType(biotype.getContainerType());
			}
		}
	}

	public void setSampleId(String sampleId) {
		this.sampleId = sampleId;
	}

	public void setComments(String comments) {
		this.comments = comments==null? null: comments.trim();
	}

	public void setUpdUser(String updUser) {
		this.updUser = updUser;
	}

	public void setCreDate(Date creDate) {
		this.creDate = creDate;
	}

	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
	}

	public Set<Biosample> getChildren() {
		return children;
	}

	/**
//	 * Returns the children that are still attached to this study, and to an other dividing group
//	 * Should be called on a topbiosample only
//	 * @return
//	 */
	//	public SortedSet<Biosample> getDividingChildren() {
	//		SortedSet<Biosample> res = new TreeSet<>();
	//		for (Biosample c : getChildren()) {
	//			if(c.getAttachedStudy()!=null && c.getAttachedStudy().equals(getAttachedStudy())) res.add(c);
	//		}
	//		return res;
	//	}

	public void setChildren(Set<Biosample> children) {
		this.children = children;
	}

	public Biosample getParent() {
		return parent;
	}

	public void setParent(Biosample parent) {
		setParent(parent, true);
	}

	public void setParent(Biosample parent, boolean updateDoubleRelationship) {
		if(parent==this.parent) return;

		if(this==parent) {
			System.err.println("Cannot set the parent to itself");
			return;
		}

		if(updateDoubleRelationship && this.parent!=null) {
			this.parent.getChildren().remove(this);
		}

		this.parent = parent;
		if(parent!=null) {
			if(parent.getAttachedStudy()!=null) {
				this.attachedStudy = null;
			}
			if(parent.getInheritedStudy()!=null) {
				this.inheritedStudy = parent.getInheritedStudy();
			}
			if(parent.getInheritedGroup()!=null) {
				this.inheritedGroup = parent.getInheritedGroup();
			}
			if(parent.getInheritedPhase()!=null) {
				this.inheritedPhase = parent.getInheritedPhase();
			}
		}

		if(updateDoubleRelationship) {
			if(this.parent!=null) {
				this.parent.getChildren().add(this);
				setTopParent(this.parent.getTopParent());
			} else {
				setTopParent(this);
			}
		}
	}

	public Map<BiotypeMetadata, String> getMetadataValues() {
		if(metadataValues==null) postLoad(); //can be null for Envers
		return metadataValues;
	}


	/**
	 * Sets the metadataValue
	 * Throws an exception if bType is invalid
	 * @param bType
	 * @param value
	 */
	public void setMetadataValue(String metadataName, String value) {
		//		for (BiotypeMetadata mType : biotype.getMetadata()) {
		//			assert mType.getBiotype().getId()==biotype.getId(): "Id mismatch in "+mType+" > "+mType.getBiotype()+":"+mType.getBiotype().getId()+" <> "+ biotype+":"+biotype.getId();
		//			if(mType.getName().equals(metadataName)) {
		//				setMetadataValue(mType, value);
		//				return;
		//			}
		//		}
		BiotypeMetadata bType = getBiotype().getMetadata(metadataName);
		if(bType==null) throw new IllegalArgumentException("Invalid metadatatype: " + metadataName+" not in " + biotype.getMetadata());
		setMetadataValue(bType, value);
	}

	/**
	 * Sets the metadataValue for any datatype (to set Files or Biosample, please call setMetadataDocument or setMetadataBiosample)
	 * Throws an exception if bType is invalid
	 * @param bType
	 * @param value
	 */
	public void setMetadataValue(BiotypeMetadata bType, String value) {
		assert bType!=null;
		assert biotype!=null;
		assert biotype.equals(bType.getBiotype());

		if(bType.getDataType()==DataType.MULTI) {
			//Sort Multi values
			String[] v = MiscUtils.split(value, ";");
			Arrays.sort(v);
			getMetadataValues().put(bType, MiscUtils.unsplit(v, ";"));
		} else {
			getMetadataValues().put(bType, value==null?"": value.trim());
		}
	}

	/**
	 * Sets the document for a metadata where dataType = FILES
	 * @param metadataName
	 * @param bio
	 */
	public void setMetadataDocument(String metadataName, Document doc) {
		setMetadataDocument(biotype.getMetadata(metadataName), doc);
	}

	/**
	 * Sets the document for a metadata where dataType = FILES
	 * @param metadataName
	 * @param bio
	 */
	public void setMetadataDocument(BiotypeMetadata bType, Document doc) {
		assert bType!=null;
		assert biotype!=null;
		assert biotype.equals(bType.getBiotype());
		if(bType.getDataType()!=DataType.D_FILE && bType.getDataType()!=DataType.FILES) throw new IllegalArgumentException(bType+ " is not a document's type");
		if(doc==null) {
			linkedDocuments.remove(bType.getId());
		} else {
			linkedDocuments.put(bType.getId(), doc);
		}
		getMetadataValues().put(bType, doc==null? "": doc.getFileName());
	}

	/**
	 * Sets the biosample for a metadata where dataType = BIOSAMPLE
	 * @param metadataName
	 * @param bio
	 */
	public void setMetadataBiosample(String metadataName, Biosample bio) {
		setMetadataBiosample(biotype.getMetadata(metadataName), bio);
	}

	/**
	 * Sets the biosample for a metadata where dataType = BIOSAMPLE
	 * @param bType
	 * @param bio
	 */
	public void setMetadataBiosample(BiotypeMetadata bType, Biosample bio) {
		assert bType!=null;
		assert biotype!=null;
		assert biotype.equals(bType.getBiotype());
		if(bType.getDataType()!=DataType.BIOSAMPLE) throw new IllegalArgumentException(bType+ " is not a document's type");
		if(bio==null) {
			linkedBiosamples.remove(bType);
		} else {
			linkedBiosamples.put(bType, bio);
		}
		getMetadataValues().put(bType, bio==null? "": bio.getSampleId());
	}


	/**
	 * @param metadataName
	 * @return
	 */
	public String getMetadataValue(String metadataName) {
		if(getBiotype()==null) return null;
		BiotypeMetadata bm = getBiotype().getMetadata(metadataName);
		if(bm==null) return null;
		return getMetadataValue(bm);
	}

	/**
	 * Returns the value of the given metadata
	 * Return "", if value is empty.
	 * Returns null, if bType is invalid
	 * @param metadataName
	 * @return
	 */
	public String getMetadataValue(BiotypeMetadata bType) {
		assert bType!=null;
		if(getBiotype()==null) return null;
		return getMetadataValues().get(bType);
	}

	public Document getMetadataDocument(String bType) {
		if(getBiotype()==null) return null;
		return getMetadataDocument(getBiotype().getMetadata(bType));
	}

	/**
	 * Returns the linked document corresponding to the linked bType. It is assumed that the datatype of bType is File
	 * @param bType
	 * @return
	 */
	public Document getMetadataDocument(BiotypeMetadata bType) {
		if(metadataValues==null) postLoad();
		assert bType!=null;
		assert bType.getDataType().isDocument();
		assert biotype!=null;

		return linkedDocuments.get(bType.getId());
	}

	public Biosample getMetadataBiosample(String bType) {
		return getMetadataBiosample(getBiotype().getMetadata(bType));
	}

	/**
	 * Returns the linked biosample corresponding to the linked bType. It is assumed that the datatype of bType is Biosample
	 * @param bType
	 * @return
	 */
	public Biosample getMetadataBiosample(BiotypeMetadata bType) {
		if(metadataValues==null) postLoad();
		assert bType!=null;
		assert bType.getDataType()==DataType.BIOSAMPLE;
		assert biotype!=null;
		Biosample b = linkedBiosamples.get(bType);
		if(b==null) {
			if(getMetadataValue(bType)!=null) {
				b = new Biosample(getMetadataValue(bType));
			}
		}
		return b;

	}

	public Location getLocation() {
		return location;
	}

	/**
	 * Return the position of the biosample inside the location.
	 * <li> a value -1 means that the sample has no position or no location
	 * <li> a value >=0 means a position is set.
	 * @return
	 */
	public int getPos() {
		return pos==null?-1: pos;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Biosample)) return false;
		if(this==obj) return true;
		if(getId()>0) return getId()==((Biosample)obj).getId();
		if(getId()<0 && getSampleId()!=null && getSampleId().length()>0 && getSampleId().equals(((Biosample)obj).getSampleId())) return true;
		return false;
	}

	@Override
	public int hashCode() {
		return id;
	}

	/**
	 * Changes the attached study, group, subgroup
	 * You should use this function from the user interface, as its makes the validation.
	 *
	 * @param attachedStudy
	 * @param group
	 * @param subGroup
	 */
	public void setAttached(Study attachedStudy, Group group, int subGroup) {

		if(CompareUtils.compare(attachedStudy, getAttachedStudy())==0 && CompareUtils.compare(group, getInheritedGroup())==0 && subGroup==getInheritedSubGroup() ) {
			//No change, skip it
			return;
		}
		if(group==null) {
			setAttachedStudy(attachedStudy);
			setInheritedGroup(null);
			setInheritedSubGroup(0);
		} else {
			setAttachedStudy(group.getStudy());
			setInheritedGroup(group);
			setInheritedSubGroup(subGroup<0 || subGroup>=group.getNSubgroups()? 0 : subGroup);
		}
	}

	public Phase getInheritedPhase() {
		return inheritedPhase;
	}
	public Group getInheritedGroup() {
		return inheritedGroup;
	}

	public String getInheritedPhaseString() {
		return getInheritedPhase()==null? "": getInheritedPhase().getShortName();
	}

	/**
	 * Updates the phase and adds an ActionMoveGroup.
	 * The phase must be set after the group, and must match the corresponding study
	 * This method returns immediately if the phase is the same.
	 * @param inheritedPhase
	 */
	public void setInheritedPhase(Phase inheritedPhase) {
		//Return immediately if there are no changes
		if(inheritedPhase==getInheritedPhase()) {
			return;
		}

		if(getInheritedGroup()!=null && inheritedPhase!=null) {
			if(inheritedPhase.getStudy() ==null ) {
				inheritedPhase.setStudy(getInheritedGroup().getStudy());
			} else if(!inheritedPhase.getStudy().equals(getInheritedGroup().getStudy())) {
				throw new IllegalArgumentException("The phase should come from the study "+getInheritedGroup().getStudy()+" not "+inheritedPhase.getStudy());
			}
		}

		//Make sure the study matched the phase
		if(inheritedStudy==null && inheritedPhase!=null) {
			this.inheritedStudy = inheritedPhase.getStudy();
		}

		//Update the biosample
		this.inheritedPhase = inheritedPhase;
	}

	/**
	 * Updates the group and adds an ActionMoveGroup.
	 * This method returns immediately if the group is the same
	 * @param inheritedGroup
	 */
	public void setInheritedGroup(Group inheritedGroup) {
		//Return immediately if there are no changes
		if((inheritedGroup==null && getInheritedGroup()==null) || (inheritedGroup!=null && inheritedGroup.equals(getInheritedGroup()))) {
			return;
		}

		//Make sure the study matched the phase
		if(inheritedStudy==null && inheritedGroup!=null) {
			this.inheritedStudy = inheritedGroup.getStudy();
		}

		//Update the biosample
		this.inheritedGroup = inheritedGroup;
		if(inheritedGroup==null) this.inheritedSubgroup = 0;
	}

	/**
	 * Return the groupName + stratification, formatted for the given user
	 * @param user
	 * @return
	 */
	public String getInheritedGroupString(String user) {
		if(getInheritedStudy()==null) {
			return "";
		} else if(user!=null && getInheritedStudy().getBlindAllUsers().contains(user)) {
			return "Blinded";
		} else if(user!=null && getInheritedStudy().getBlindDetailsUsers().contains(user==null?"": user)) {
			return getInheritedGroup()==null?"": getInheritedGroup().getShortName() + (getInheritedGroup().getNSubgroups()>1? " '"+(getInheritedSubGroup()+1):"");
		} else {
			return getInheritedGroup()==null?"": getInheritedGroup().getName() + (getInheritedGroup().getNSubgroups()>1? " '"+(getInheritedSubGroup()+1):"");
		}
	}

	public Study getInheritedStudy() {
		return inheritedStudy;
	}

	/**
	 * Clones this sample except its children and container (don't follow the relationships)
	 * Keeps the same parent and id
	 */
	@Override
	public Biosample clone() {
		try {
			Biosample res = new Biosample();
			res.biotype = biotype;
			res.comments = comments;
			res.creDate = creDate;
			res.creUser = creUser;
			res.group = group;
			res.elb = elb;
			res.id = id;
			res.attachedStudy  = attachedStudy;
			res.attachedSampling = attachedSampling;
			res.inheritedStudy = inheritedStudy;
			res.inheritedGroup = inheritedGroup;
			res.inheritedPhase = inheritedPhase;
			res.name = name;
			res.container = container;
			res.containerIndex = containerIndex;

			res.quality = quality;
			res.sampleId = sampleId;
			res.expiryDate = expiryDate;
			res.amount = amount;
			res.containerIndex = containerIndex;

			res.setParent(parent);

			res.serializedMetadata = serializedMetadata;
			res.serializedMetadata2 = serializedMetadata2;
			res.linkedBiosamples = new HashMap<>(linkedBiosamples);
			res.linkedDocuments = new HashMap<>();
			res.updDate = updDate;
			res.updUser = updUser;
			for(Map.Entry<Integer, Document> e: linkedDocuments.entrySet()) {
				byte[] copy = new byte[e.getValue().getBytes().length];
				System.arraycopy(e.getValue().getBytes(), 0, copy, 0, copy.length);
				res.linkedDocuments.put(e.getKey(), new Document(e.getValue().getFileName(), copy));
			}
			return res;
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Some important rules:
	 * - If no top parents, 2 animals should be sorted by biotype, group, name, sampleId (in this order)
	 * - If top parents, 2 animals should be sorted by group/animalNo first
	 */
	@Override
	public int compareTo(Biosample o2) {
		return COMPARATOR_NATURAL.compare(this, o2);
	}


	/**
	 * Reasonably fast comparator, which order based on the top sample, and based on the biosample metadata (no parent checking)
	 */
	public static final Comparator<Biosample> COMPARATOR_NATURAL = new Comparator<Biosample>() {
		@Override
		public int compare(Biosample o1, Biosample o2) {
			if(o2==null) return o1==null?0: -1;
			if(o2==o1) return 0;
			if(o1.getId()>0 && o1.getId()==o2.getId()) return 0;

			//Compare Scanned Position
			int c;
			c = (o1.getScannedPosition()==null? "":o1.getScannedPosition()).compareTo(o2.getScannedPosition()==null? "":o2.getScannedPosition());
			if(c!=0) return c;

			//Compare TopParent (ie type, date)
			Biosample p1 = o1.getTopParentInSameStudy();
			Biosample p2 = o2.getTopParentInSameStudy();

			c = p1.compareToNoHierarchyCheck(p2);
			if(c!=0) return c;

			return o1.compareToNoHierarchyCheck(o2);
		}
	};

	public static final Comparator<Biosample> HIERARCHY_COMPARATOR = new Comparator<Biosample>() {
		@Override
		public int compare(Biosample o1, Biosample o2) {
			if(o2==null) return -1;
			if(o2==o1) return 0;

			if(o1.getId()>0 && o1.getId()==o2.getId()) return 0;

			//Compare Parents (ie type, date)
			Biosample p1 = o1.getParent();
			Biosample p2 = o2.getParent();

			if(p1==null && p2==null) {
				//OK
			} else if(p1==null) {
				int c = compare(o1, p2);
				if(c!=0) return c;
				return -1;
			} else if(p2==null) {
				int c = compare(p1, o2);
				if(c!=0) return c;
				return 1;
			} else {
				int c = compare(p1, p2);
				if(c!=0) return c;
			}
			return o1.compareToNoHierarchyCheck(o2);

		}
	};

	private int compareToNoHierarchyCheck(Biosample o2) {
		int c;

		//Compare study
		c = -(getInheritedStudy()==null?"": getInheritedStudy().getStudyId()).compareTo((o2.getInheritedStudy()==null?"": o2.getInheritedStudy().getStudyId()));
		if(c!=0) return c;

		if(getInheritedStudy()!=null && !getInheritedStudy().isBlindAll()) {
			//Compare groups
			c = CompareUtils.compare(getInheritedGroup(), o2.getInheritedGroup());
			if(c!=0) return c;

			c = CompareUtils.compare(getInheritedSubGroup(), o2.getInheritedSubGroup());
			if(c!=0) return c;
		}

		//Compare phases
		c = CompareUtils.compare(getInheritedPhase(), o2.getInheritedPhase());
		if(c!=0) return c;

		//Compare biotype
		c = getBiotype()==null? (o2.getBiotype()==null?0:1): getBiotype().compareTo(o2.getBiotype());
		if(c!=0) return c;


		//Compare SampleName (if any): the comparison should compare numbers in a numeral and not in lexical comparison, so that 1<8<10
		String s1 =    getBiotype()==null?"":    getBiotype().getCategory()==BiotypeCategory.PURIFIED &&    !getBiotype().isHideSampleId()?    getSampleId():    getBiotype().getSampleNameLabel()!=null?    getSampleName():"";
		String s2 = o2.getBiotype()==null?"": o2.getBiotype().getCategory()==BiotypeCategory.PURIFIED && !o2.getBiotype().isHideSampleId()? o2.getSampleId(): o2.getBiotype().getSampleNameLabel()!=null? o2.getSampleName():"";
		//		c = (s1==null?"":s1).compareToIgnoreCase((s2==null?"":s2));
		c = CompareUtils.compare(s1, s2);
		if(c!=0) return c;

		//Compare
		c = (getComments()==null?"":getComments()).compareToIgnoreCase((o2.getComments()==null?"":o2.getComments()));
		if(c!=0) return c;

		//Compare containers
		c = CompareUtils.compare(getContainerType(), o2.getContainerType());
		if(c!=0) return c;

		c = CompareUtils.compare(getContainerId(), o2.getContainerId());
		if(c!=0) return c;

		//Compare sampleId
		c = CompareUtils.compare(getSampleId(), o2.getSampleId());
		if(c!=0) return c;

		//Could only happen when the sampleid is not set. Still the result should never be 0 (ie they are not equal)
		return o2.instance-instance;
	}

	private static transient int counter = 0;
	private transient final int instance = counter++;


	/**
	 * The top of the tree hierarchy, should never be null
	 * @return
	 */
	public Biosample getTopParent() {
		return topParent==null? this: topParent;
	}
	/**
	 * The top of the tree hierarchy, which is in the same study.
	 * This function is useful, when living samples are reused across studies.
	 * In this case, the samples are cloned from a common sample detached from all study, and having no name
	 *
	 * <pre>
	 *                   Living12345
	 *                  /           \
	 * Living12345A [12] (S-1)            Living12345B [14] (S-2)
	 *   /          \                   /           \
	 * Org1 (S-1)    Org2 (S-1)      Org3 (S-1)    Org4 (S-2)
	 *   ....                        .....
	 *
	 *   Input: org3 -> returns Living12345B
	 * </pre>
	 *
	 * @return
	 */
	public Biosample getTopParentInSameStudy() {
		Study s = getInheritedStudy();
		Biosample top = getTopParent();

		//Skip going to the hierarchy if there is not study, or if the study matches
		if(s==null || s.equals(top.getInheritedStudy())) return top;

		//If not, go up until we find a parent within the same study
		top = this;
		while(top.getParent()!=null && (s.equals(top.getParent().getInheritedStudy()))) {
			top = top.getParent();
		}
		return top;
	}

	public void setTopParent(Biosample topParent) {
		this.topParent = topParent;
	}

	public String getMetadataAsString() {
		return getInfos(EnumSet.of(InfoFormat.METATADATA), InfoSize.ONELINE);
	}



	/**
	 * If mode =
	 * TERMINAL : return leaves only (excluding this)
	 * SIEBLINGS: returns children of the parents (excluding this)
	 * PARENTS: return parents(excluding this)
	 * CHILDREN: return children (excluding this)
	 * ALL: return all (including this)
	 *
	 * The items are returned sorted hierarchically
	 * @param mode
	 * @return
	 */
	public Set<Biosample> getHierarchy(HierarchyMode mode) {
		Set<Biosample> res = new LinkedHashSet<>();
		if(mode==HierarchyMode.ALL || mode==HierarchyMode.ALL_MAX2) {
			Biosample b = getParent();
			while(b!=null) {
				res.add(b);
				b = b.getParent();
			}
			getHierarchyRec(HierarchyMode.CHILDREN, res, this, mode==HierarchyMode.ALL? 99: 2);
			return res;

		} else if(mode==HierarchyMode.SIEBLINGS) {
			if(getParent()!=null) {
				res.addAll(getParent().getChildren());
			}
			res.remove(this);
		} else if(mode==HierarchyMode.PARENTS) {
			Biosample b = getParent();
			while(b!=null) {
				res.add(b);
				b = b.getParent();
			}
		} else {
			getHierarchyRec(mode, res, this, 99);
			if(mode!=HierarchyMode.AS_STUDY_DESIGN && mode!=HierarchyMode.ATTACHED_SAMPLES) res.remove(this);
		}

		return res;
	}

	private void getHierarchyRec(HierarchyMode mode, Set<Biosample> res, Biosample root, int maxDepth) {
		if(maxDepth<0) return;
		if(res.contains(root)) return; //avoid loops, but this should never happen

		if(mode==HierarchyMode.TERMINAL) {
			if(root.getChildren().size()==0) {
				res.add(root);
			}
		} else if(mode==HierarchyMode.AS_STUDY_DESIGN) {
			if(root.equals(this) || root.getAttachedSampling()!=null  || (root.getAttachedStudy()!=null && root.getAttachedStudy().equals(getAttachedStudy()))) {
				res.add(root);
			}
		} else if(mode==HierarchyMode.ATTACHED_SAMPLES) {
			if(root.equals(this) || root.getAttachedSampling()!=null) {
				res.add(root);
			} else if(root.getAttachedStudy()!=null && root.getAttachedStudy().equals(getAttachedStudy())) {
				return;
			}
		} else if(mode==HierarchyMode.CHILDREN || mode==HierarchyMode.CHILDREN_NOT_ATTACHED) {
			res.add(root);
		}

		//Process children recursively (sorted)
		if(root.getChildren()!=null && root.getChildren().size()>0) {
			List<Biosample> children = new ArrayList<>(root.getChildren());
			Collections.sort(children);
			for(Biosample child: children) {
				if(mode==HierarchyMode.CHILDREN_NOT_ATTACHED && child.getAttachedStudy()!=null) continue;
				getHierarchyRec(mode, res, child, maxDepth-1);
			}
		}
	}

	public void setSampleName(String name) {
		this.name = name==null? null: name.trim();
	}

	public String getSampleName() {
		return name;
	}

	/**
	 * @param group the department to set
	 */
	public void setEmployeeGroup(EmployeeGroup group) {
		this.group = group;
	}

	/**
	 * @return the department
	 */
	public EmployeeGroup getEmployeeGroup() {
		return group;
	}

	public boolean isEmpty() {
		if(getSampleId()!=null && getSampleId().length()>0) return false;
		if(getBiotype()!=null &&  getBiotype().getSampleNameLabel()!=null && getSampleName()!=null && getSampleName().length()>0) return false;
		if(getAmount()!=null) return false;
		if(getContainerId()!=null && getContainerId().length()>0) return false;
		if(getMetadataAsString().trim().length()>0) return false;
		if(getComments()!=null && getComments().length()>0) return false;
		return true;
	}

	/**
	 * @param elb the elb to set
	 */
	public void setElb(String elb) {
		this.elb = elb;
	}

	/**
	 * @return the elb
	 */
	public String getElb() {
		return elb;
	}

	public boolean isCompatible(String metadata, String groupPrefixOrSuffix) {
		if(metadata!=null && metadata.length()>0) {
			StringTokenizer st = new StringTokenizer(metadata, " ,;/");
			String m = getInfos(EnumSet.allOf(InfoFormat.class), InfoSize.ONELINE).toLowerCase();
			while(st.hasMoreTokens()) {
				String token = st.nextToken().toLowerCase();
				if(!m.contains(token)) return false;
			}
		}
		if(groupPrefixOrSuffix!=null && groupPrefixOrSuffix.length()>0) {
			if(getInheritedGroup()==null || !getInheritedGroup().getName().equalsIgnoreCase(groupPrefixOrSuffix)) {
				//Ok
			} else if(getInheritedGroup()==null || !getInheritedGroup().getName().startsWith(groupPrefixOrSuffix)) {
				return false;
			} else if(getInheritedGroup()==null || !getInheritedGroup().getName().endsWith(groupPrefixOrSuffix)) {
				return false;
			}
		}
		return true;
	}


	/**
	 * Get the hierarchy of all children conformed to the study design, either dividing (attached to a group), or samples (attached to a sampling), including itself
	 * Sorted hierarchically
	 * @param phase
	 * @param onlyWithRequiredAction (true to return only the animal/children requiring weighing/length)
	 * @return
	 */
	public List<Biosample> getSamplesFromStudyDesign(Phase phase, boolean onlyWithRequiredAction) {
		List<Biosample> res = new ArrayList<>();
		for (Biosample 	sample : getHierarchy(HierarchyMode.AS_STUDY_DESIGN)) {
			if(sample==this) {
				//Animal
			} else if(sample.getInheritedPhase()==null) {
				//Dividing Sample
				if(!onlyWithRequiredAction) {
					res.add(sample);
				}
			} else if(phase==null || sample.getInheritedPhase().equals(phase)) {
				//Sample
				if(!onlyWithRequiredAction) {
					res.add(sample);
				} else if(sample.getAttachedSampling()!=null && sample.getAttachedSampling().hasMeasurements()) {
					res.add(sample);
				}
			}
		}

		return res;
	}

	public List<Biosample> getCompatibleInFamily(HierarchyMode mode, String metadata, String groupPrefixOrSuffix, String phase) {
		List<Biosample> res = new ArrayList<Biosample>();
		Set<Biosample> family = getHierarchy(mode);
		family.remove(this);

		for (Biosample b : family) {

			//Test the metadata
			if(!b.isCompatible(metadata, groupPrefixOrSuffix)) continue;

			//Test the group
			if(groupPrefixOrSuffix!=null && groupPrefixOrSuffix.length()>0) {

				if(b.getInheritedGroup()==null) {
					continue;
				} else if(b.getInheritedGroup().getName().equals(groupPrefixOrSuffix)) {
					//Ok
				} else if(b.getInheritedGroup().getName().startsWith(groupPrefixOrSuffix) && getInheritedGroup().getName().length()>groupPrefixOrSuffix.length()) {
					String suffix = b.getInheritedGroup().getName().substring(groupPrefixOrSuffix.length());
					if(!getInheritedGroup().getName().endsWith(suffix)) continue;
				} else if(b.getInheritedGroup().getName().endsWith(groupPrefixOrSuffix) && getInheritedGroup().getName().length()>groupPrefixOrSuffix.length()) {


					String prefix = b.getInheritedGroup().getName().substring(0, b.getInheritedGroup().getName().length()-groupPrefixOrSuffix.length());
					if(!getInheritedGroup().getName().startsWith(prefix)) continue;
				} else {
					continue;
				}
			}

			//Test the phase
			if(phase!=null && (b.getInheritedPhase()==null || !phase.equals( b.getInheritedPhase().getShortName()))) continue;


			res.add(b);
		}
		return res;
	}

	/**
	 * @param quality the quality to set
	 */
	public void setQuality(Quality quality) {
		this.quality = quality;
	}

	/**
	 * @return the quality
	 */
	public Quality getQuality() {
		return quality==null? Quality.VALID: quality;
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

	@PreRemove
	public void preRemove() {
		//		actions.clear();

		//detach from the study
		inheritedGroup = null;
		inheritedPhase = null;
		attachedStudy = null;
		inheritedStudy = null;

		setContainer(null);

		//remove link from children
		for (Biosample b : new ArrayList<>(children)) {
			b.setParent(null);
		}

		//remove link from the parent
		if(parent!=null) {
			parent.children.remove(this);
		}
		this.parent = null;
		this.topParent = null;
		this.children.clear();
	}

	/**
	 * @param study the study to set
	 */
	public void setInheritedStudy(Study study) {
		this.inheritedStudy = study;
		if(study==null || (inheritedGroup!=null && inheritedGroup.getStudy().getId()>0 && !inheritedGroup.getStudy().equals(study))) {
			this.inheritedGroup = null;
		}
		if(study==null || (inheritedPhase!=null && inheritedPhase.getStudy().getId()>0 && !inheritedPhase.getStudy().equals(study))) {
			this.inheritedPhase = null;
		}
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * @return the amount
	 */
	public Double getAmount() {
		return amount;
	}

	/**
	 * @return the amount
	 */
	public Amount getAmountAndUnit() {
		return biotype==null || biotype.getAmountUnit()==null? null: new Amount(amount, biotype.getAmountUnit());
	}

	//	/**
	//	 * Adds an action (and delete the similar action in the list if it was not saved, so that
	//	 * "move to loc1" will erase any other "move to ..." recorded in the same session)
	//	 * @param action
	//	 */
	//	public void addAction(ActionBiosample action) {
	//		for (Iterator<ActionBiosample> iterator = actions.iterator(); iterator.hasNext();) {
	//			ActionBiosample a = iterator.next();
	//			if(a.getId()<=0 && a.getClass()==action.getClass()) {
	//				iterator.remove();
	//			}
	//		}
	//		actions.add(action);
	//	}

	public Map<String, Object> getAuxiliaryInfos() {
		return infos;
	}

	public String getContainerId() {
		return this.container==null? null: this.container.getContainerId();
	}

	public ContainerType getContainerType() {
		return this.container==null? null:this.container.getContainerType();
	}

	public void setContainerType(ContainerType containerType) {
		setContainer(containerType==null? null: new Container(containerType, getContainerId()));
	}

	public void setContainerId(String containerId) {
		setContainer(new Container(getContainerType(), containerId));
	}

	public Integer getContainerIndex() {
		return containerIndex;
	}

	public void setContainerIndex(Integer index) {
		this.containerIndex = index;
	}

	public boolean isAbstract() {
		return getBiotype()!=null && getBiotype().isAbstract();
	}

	public Container getContainer() {
		//Be sure to add a relation to this object (if we call getBiosamples)
		if(container==null) {
			container = new Container();
		}

		//Make sure to call 'container.setCreatedFor' even if container is not null, to avoid bugs
		container.setCreatedFor(this);
		return container;
	}

	public Integer getBlocNo() {
		return Container.getBlocNo(getContainerType(), getContainerId());
	}

	/**
	 * Sets the container and adds an ActionContainer
	 * @param container
	 */
	public void setContainer(Container container) {
		String oldCid = getContainerId();
		ContainerType oldCType = getContainerType();

		String newCid = container==null? null: container.getContainerId();
		ContainerType newCType = container==null? null: container.getContainerType();

		//Return if there are no change
		if(container!=null) {
			container.setCreatedFor(this);
		}
		if(((oldCid==null && newCid==null) || (oldCid!=null && oldCid.equals(newCid)))
				&& (oldCType==newCType)) {
			return;
		}

		//Remove this sample from the older container
		if(this.container!=null) {
			this.container.removeBiosample(this);
		}

		//And add it to the new one
		if(container!=null) {
			container.addBiosample(this);
		}

		//		//Adds an action if this is a multiple container
		//		if(container!=null && container.getContainerType()!=null /*&& container.getContainerType().isMultiple()*/) {
		//			//Set a multiple container
		//			if(getContainerId()==null || !getContainerId().equals(container.getContainerId())) {
		//				addAction(new ActionContainer(this, container.getContainerId()));
		//			}
		//		} else if(container==null && this.container!=null) {
		//			addAction(new ActionContainer(this, null));
		//		}

		//Update the container
		this.container = container;
	}

	public static SortedSet<Location> getLocations(Collection<Biosample> biosamples){
		SortedSet<Location> locations = new TreeSet<>();
		for (Biosample animal: biosamples) {
			Location loc = animal.getLocation();
			if(loc!=null) {
				if(!locations.contains(loc)) {
					locations.add(loc);
				}
			}
		}
		return locations;
	}

	public static Biotype getBiotype(Collection<Biosample> biosamples) {
		Set<Biotype> res = getBiotypes(biosamples);
		if(res.size()==1) return res.iterator().next();
		return null;
	}

	public static SortedSet<Biotype> getBiotypes(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		SortedSet<Biotype> biotypes = new TreeSet<>();
		for (Biosample b : biosamples) {
			if(b!=null && b.getBiotype()!=null) biotypes.add(b.getBiotype());
		}
		return biotypes;
	}

	public static Study getStudy(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<Study> res = getStudies(biosamples);
		if(res.size()==1) return res.iterator().next();
		return null;
	}

	public static Status getStatus(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		SortedSet<Status> res = new TreeSet<>();
		for (Biosample b : biosamples) {
			if(b!=null) res.add(b.getStatus());
		}
		return res.size()==1? res.iterator().next(): null;
	}

	/**
	 * Return unsorted studies including null values
	 * @param biosamples
	 * @return
	 */
	public static Set<Study> getStudies(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<Study> res = new HashSet<>();
		for (Biosample b : biosamples) {
			res.add(b.getInheritedStudy());
		}
		return res;
	}

	/**
	 * Return phases including null values
	 * @param biosamples
	 * @return
	 */
	public static Set<Phase> getPhases(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<Phase> res = new LinkedHashSet<>();
		for (Biosample b : biosamples) {
			res.add(b.getInheritedPhase());
		}
		return res;
	}

	public static Phase getPhase(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<Phase> res = getPhases(biosamples);
		if(res.size()==1) return res.iterator().next();
		return null;
	}

	public static String getBiotypeString(Collection<Biosample> biosamples) {
		Collection<Biotype> biotypes = getBiotypes(biosamples);
		String s = biotypes.size()==1? biotypes.iterator().next().getName(): "";
		return s;
	}

	public static enum InfoFormat {
		STUDY,
		BIOTYPE,
		TOPIDNAMES,
		SAMPLEID,
		SAMPLENAME,
		METATADATA,
		AMOUNT,
		PARENT_SAMPLENAME,
		PARENT_METATADATA,
		COMMENTS,
		LOCATION
	}

	public static enum InfoSize {
		ONELINE,
		COMPACT,
		EXPANDED
	}

	public String getInfos() {
		return getInfos(EnumSet.allOf(InfoFormat.class));
	}

	public String getInfos(EnumSet<InfoFormat> dataFormat) {
		return getInfos(Collections.singletonList(this), dataFormat, InfoSize.ONELINE);
	}

	public String getInfos(EnumSet<InfoFormat> dataFormat, InfoSize infoSize) {
		return getInfos(Collections.singletonList(this), dataFormat, infoSize);
	}

	/**
	 * Util function to get the shared metadata
	 * @param biosamples
	 * @return
	 */
	public static String getInfosMetadata(Collection<Biosample> biosamples) {
		return getInfos(biosamples, EnumSet.of(InfoFormat.BIOTYPE, InfoFormat.SAMPLENAME, InfoFormat.METATADATA, InfoFormat.COMMENTS, InfoFormat.AMOUNT), InfoSize.ONELINE);
	}

	/**
	 * Util function to get the shared study info
	 * @param biosamples
	 * @return
	 */
	public static String getInfosStudy(Collection<Biosample> biosamples) {
		return getInfos(biosamples, EnumSet.of(InfoFormat.STUDY, InfoFormat.TOPIDNAMES), InfoSize.COMPACT);
	}

	public static boolean isEmpty(Collection<Biosample> biosamples) {
		for (Biosample biosample : biosamples) {
			if(!biosample.isEmpty()) return false;
		}
		return true;
	}

	/**
	 * Find shared metadata
	 * @param biosamples
	 * @return
	 */
	public static String getInfos(Collection<Biosample> biosamples, EnumSet<InfoFormat> dataFormat, InfoSize infoSize) {
		if(biosamples==null) return null;
		StringBuilder sb = new StringBuilder();

		Set<Biotype> types = getBiotypes(biosamples);
		String separator1 = infoSize==InfoSize.ONELINE?" ": "\n"; //Separator between different items (ex: groups and metadata)
		String separator2 = infoSize==InfoSize.EXPANDED?"\n": " "; //Separator between similar items (ex: metadata)


		//STUDY Display
		if(dataFormat.contains(InfoFormat.STUDY)) {

			//Add the study
			Study study = Biosample.getStudy(biosamples);
			if(study!=null) {
				sb.append(study.getStudyId() + (study.getLocalId()!=null && study.getLocalId().length()>0?" (" + study.getLocalId() + ")":"") + separator1);
			}

			//Add the group
			Group g = Biosample.getGroup(biosamples);
			if(study!=null && g!=null) {
				sb.append((study.isBlind()?"Gr."+g.getShortName(): g.getName()) + separator1);
			}
			//Add the phase
			Phase phase = Biosample.getPhase(biosamples);
			if(phase!=null) {
				sb.append(phase.getAbsoluteDateAndName() + separator1);
			}
		}

		//TOPIDNAME Display
		if(dataFormat.contains(InfoFormat.TOPIDNAMES)) {
			boolean first = true;
			Set<Biosample> tops = Biosample.getTopParentsInSameStudy(biosamples);
			tops.removeAll(biosamples);

			if(tops.size()==0 || tops.size()>4) {
				//skip
			} else {
				for (Biosample b: tops) {
					if(first) first = false;
					else sb.append(separator2);
					sb.append(b.getSampleIdName());
				}
				sb.append(separator1);
			}
		}

		//SAMPLEID Display
		if(dataFormat.contains(InfoFormat.SAMPLEID) && biosamples.size()==1) {
			Biosample b = biosamples.iterator().next();
			sb.append(b.getSampleId() + separator1);
		}

		//TYPE Display
		if(dataFormat.contains(InfoFormat.BIOTYPE)) {
			if(types.size()==1) {
				String s = types.iterator().next().getName();
				sb.append(s + separator2);
			}
		}

		//SAMPLENAME Display
		if(dataFormat.contains(InfoFormat.SAMPLENAME)) {
			HashSet<String> map = new HashSet<String>();
			for(Biosample b : biosamples) {
				if(b.getBiotype()!=null && b.getBiotype().getSampleNameLabel()!=null && b.getSampleName()!=null) {
					map.add(b.getSampleName());
				} else {
					map.add("");
				}
			}
			if(map.size()==1 && map.iterator().next().length()>0) {
				sb.append(map.iterator().next() + separator2);
			}
		}
		//Find shared metadata
		StringBuilder sb2 = new StringBuilder();
		if(dataFormat.contains(InfoFormat.METATADATA)) {

			SetHashMap<String, String> map = new SetHashMap<>();
			for(Biosample b : biosamples) {
				if(b.getBiotype()==null) continue;
				for(BiotypeMetadata bm: b.getBiotype().getMetadata()) {

					if(bm.isSecundary()) continue;
					if(bm.getDataType()==DataType.D_FILE) continue;
					else if(bm.getDataType()==DataType.FILES) continue;
					else if(bm.getDataType()==DataType.LARGE) continue;
					String s = b.getMetadataValue(bm);
					if(s==null || s.length()==0) continue;
					map.add(bm.getName(), s + bm.extractUnit());
				}
			}
			int count = 0;
			for(String mt: map.keySet()) {
				Set<String> l = map.get(mt);
				String val = l.size()==1? l.iterator().next(): null;
				if(val!=null && val.length()>0 ) {
					sb2.append(val + separator2);
					if(++count>6) break;
				}
			}
		}
		//Amount
		if(dataFormat.contains(InfoFormat.AMOUNT)) {
			if(biosamples.size()==1) {
				for(Biosample b : biosamples) {
					Amount amount = b.getAmountAndUnit();
					if(amount!=null) {
						sb.append(amount.toString() + separator2);
						break;
					}
				}
			}
		}

		if(dataFormat.contains(InfoFormat.PARENT_SAMPLENAME)) {
			//Inherited Parents??
			{
				SetHashMap<String, String> map = new SetHashMap<>();
				for(Biosample b : biosamples) {
					if(b.getBiotype()!=null && b.getBiotype().getParent()!=null && b.getParent()!=null && b.getBiotype().getParent().equals(b.getParent().getBiotype())) {
						if(b.getParent().getBiotype().getSampleNameLabel()!=null && b.getParent().getSampleName()!=null) {
							map.add("_name",b.getParent().getSampleName());
						}
					}
				}
				for(String mt: map.keySet()) {
					Set<String> l = map.get(mt);
					String val = l.size()==1? l.iterator().next(): null;
					if(val!=null && val.length()>0) sb2.append(l.iterator().next() + separator2);

				}
			}
		}
		if(sb2.length()>0) {
			sb.append(sb2.substring(0, sb2.length()-1) + separator1);
		}

		//Find shared metadata
		sb2 = new StringBuilder();
		if(dataFormat.contains(InfoFormat.PARENT_METATADATA)) {
			SetHashMap<String, String> map = new SetHashMap<>();
			for(Biosample b : Biosample.getParents(biosamples)) {
				if(b.getBiotype()!=null) {
					for(BiotypeMetadata m: b.getBiotype().getMetadata()) {
						if(m.getDataType()==DataType.D_FILE) continue;
						else if(m.getDataType()==DataType.FILES) continue;
						else if(m.getDataType()==DataType.LARGE) continue;
						else if(m.isSecundary()) continue;
						map.add(m.getName(), b.getMetadataValue(m) + m.extractUnit());
					}
				}
			}
			int count = 0;
			for(String mt: map.keySet()) {
				Set<String> l = map.get(mt);
				String val = l.size()==1? l.iterator().next(): null;
				if(val!=null && val.length()>0 ) {
					sb2.append(val + separator2);
					if(++count>6) break;
				}
			}
		}

		//Comments
		if(dataFormat.contains(InfoFormat.COMMENTS)) {
			String val = null;
			for(Biosample b : biosamples) {
				String v = b.getComments();
				if(val!=null && !val.equals(v)) {
					val = null;
					break;
				}
				val = v;
			}
			if(val!=null) {
				sb.append(val + separator1);
			}
		}

		if(dataFormat.contains(InfoFormat.LOCATION)) {
			//Location
			Set<Location> locations = getLocations(biosamples);
			if(locations.size()==1) {
				sb.append("["+locations.iterator().next()+"]"+separator1);
			} else if(locations.size()>1) {
				sb.append("["+locations.size()+" Locs]"+separator1);
			} else {
				if(types.size()==1 && !types.iterator().next().isAbstract()) {
					//					sb.append("[NoLoc]"+separator1);
				} else {
					sb.append(separator1);
				}
			}
		}




		String res = sb.toString();
		while(res.startsWith(separator1)) res = res.substring(separator1.length());
		while(res.endsWith(separator1)) res = res.substring(0, res.length() - separator1.length());

		return res;

	}

	public static Set<String> getTypes(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<String> res = new HashSet<>();
		for (Biosample b : biosamples) {
			String s = b.getBiotype()==null? "": b.getBiotype().getName();
			res.add(s);
		}
		return res;
	}

	public static Set<String> getMetadata(String metadataName, Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<String> res = new HashSet<>();
		for (Biosample b : biosamples) {
			if(b.getBiotype()==null) continue;
			String s = b.getMetadataValue(metadataName);
			if(s!=null && s.length()>0) {
				res.add(s);
			}
		}
		return res;
	}

	public static Set<String> getMetadata(BiotypeMetadata metadata, Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<String> res = new HashSet<>();
		for (Biosample b : biosamples) {
			if(b.getBiotype()==null) continue;
			String s = b.getMetadataValue(metadata);
			if(s!=null && s.length()>0) {
				res.add(s);
			}
		}
		return res;
	}

	public static Set<Biosample> getTopParents(Collection<Biosample> biosamples) {
		Set<Biosample> res = new HashSet<>();
		for (Biosample b : biosamples) {
			res.add(b.getTopParent());
		}
		return res;
	}

	public static Biosample getTopParentInSameStudy(Collection<Biosample> biosamples) {
		Set<Biosample> res = getTopParentsInSameStudy(biosamples);
		if(res.size()==1) return res.iterator().next();
		return null;
	}

	public static Set<Biosample> getTopParentsInSameStudy(Collection<Biosample> biosamples) {
		Set<Biosample> res = new LinkedHashSet<>();
		for (Biosample b : biosamples) {
			res.add(b.getTopParentInSameStudy());
		}
		return res;
	}

	/**
	 * Gets the parent of the given biosamples (just the parent, excluding the input)
	 * @param biosamples
	 * @return
	 */
	public static Set<Biosample> getParents(Collection<Biosample> biosamples) {
		Set<Biosample> res = new LinkedHashSet<>();
		for (Biosample b : biosamples) {
			if(b.getParent()!=null) res.add(b.getParent());
		}
		return res;
	}

	/**
	 * Gets all the parent of the given biosamples (recursively, including the input)
	 * @param biosamples
	 * @return
	 */
	public static Set<Biosample> getParentRecursively(Collection<Biosample> biosamples) {
		Set<Biosample> res = new LinkedHashSet<>();
		for (Biosample b : biosamples) {
			Biosample tmp = b;
			while(tmp!=null) {
				res.add(tmp);
				tmp = tmp.getParent();
			}
		}
		return res;
	}

	/**
	 * Display the identifier as followed:
	 * - no biotype: sampleId
	 * - biotype where sampleId is hidden: sampleName (or sampleId if none)
	 * - sampleId [sampleName] otherwise
	 *
	 * @return
	 */
	public String getSampleIdName() {
		if(getBiotype()==null) {
			return getSampleId();
		} else if(getBiotype().isHideSampleId()) {
			return (getBiotype()!=null && getBiotype().getSampleNameLabel()!=null && getSampleName()!=null && getSampleName().length()>0? getSampleName(): getSampleId());
		} else {
			return getSampleId() + (getBiotype()!=null && getBiotype().getSampleNameLabel()!=null && getSampleName()!=null && getSampleName().length()>0?" ["+getSampleName()+"]":"");
		}
	}

	public String getSampleNameOrId() {
		return getBiotype()!=null && getBiotype().getSampleNameLabel()!=null && getSampleName()!=null && getSampleName().length()>0? getSampleName() : getSampleId();
	}

	/**
	 * return (non null) groups
	 * @param biosamples
	 * @return
	 */
	public static SortedSet<Group> getGroups(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		SortedSet<Group> res = new TreeSet<>();
		for (Biosample b : biosamples) {
			if(b.getInheritedGroup()!=null) res.add(b.getInheritedGroup());
		}
		return res;
	}

	public static SortedSet<NamedSampling> getNamedSamplings(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		SortedSet<NamedSampling> res = new TreeSet<>();
		for (Biosample b : biosamples) {
			if(b.getAttachedSampling()!=null) res.add(b.getAttachedSampling()==null? null: b.getAttachedSampling().getNamedSampling());
		}
		return res;
	}

	public static Group getGroup(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<Group> res = new HashSet<>();
		for (Biosample b : biosamples) {
			res.add(b.getInheritedGroup());
			if(res.size()>1) return null;
		}
		if(res.size()==1) return res.iterator().next();
		return null;
	}

	public static SortedSet<ContainerType> getContainerTypes(Collection<Biosample> biosamples) {
		SortedSet<ContainerType> res = new TreeSet<>();
		if(biosamples==null) return null;
		for (Biosample b : biosamples) {
			if(b.getContainerType()!=null) res.add(b.getContainerType());
		}
		return res;
	}

	public static ContainerType getContainerType(Collection<Biosample> biosamples) {
		SortedSet<ContainerType> res = getContainerTypes(biosamples);
		return res.size()==1? res.first(): null;
	}

	public static SortedSet<String> getContainerIds(Collection<Biosample> biosamples) {
		SortedSet<String> res = new TreeSet<>();
		if(biosamples==null) return null;
		for (Biosample b : biosamples) {
			if(b.getContainerId()!=null && b.getContainerId().length()>0) res.add(b.getContainerId());
		}
		return res;
	}

	public static boolean isInDifferentContainers(Collection<Biosample> biosamples) {
		if(biosamples==null || biosamples.size()<=1) return false;

		//Count number of Containers
		List<String> l = new ArrayList<>();
		for (Biosample b : biosamples) {
			if(b.getContainerId()!=null && b.getContainerId().length()>0) {
				if(l.contains(b.getContainerId())) {
					//ok
				} else if(l.size()>=1) {
					return true;
				} else {
					l.add(b.getContainerId());
				}
			} else {
				if(l.size()>=1) return true;
				l.add(b.getContainerId());
			}
		}
		return false;
	}


	public static SortedSet<Integer> getScannedPoses(Collection<Biosample> biosamples, Location loc) {
		SortedSet<Integer> res = new TreeSet<>();
		if(biosamples==null) return null;
		for (Biosample b : biosamples) {
			int pos = b.getPos();
			try {
				if(b.getScannedPosition()!=null && loc!=null) pos = loc.parsePosition(b.getScannedPosition());
			} catch (Exception e) {
				e.printStackTrace();
			}
			res.add(pos);
		}
		return res;
	}

	/**
	 *
	 * @param biosamples
	 * @return
	 */
	public static List<Container> getContainers(Collection<Biosample> biosamples) {
		return getContainers(biosamples, false);
	}


	public static List<Container> getContainers(Collection<Biosample> biosamples, boolean createFakeContainerForEmptyOnes) {
		if(biosamples==null) return null;
		List<Container> res = new ArrayList<>();
		HashSet<Container> seen = new HashSet<>();
		for (Biosample b : biosamples) {
			if(b==null) continue;
			Container c = b.getContainer();
			if(c==null || c.getContainerType()==null) {
				if(createFakeContainerForEmptyOnes) {
					//Create a container with a one-way relationship container->biosample
					//(biosample->container is not kept because we don't want it to become persistant)
					c = new Container();
					b.setContainer(c);
					res.add(c);
				}
			} else if(!seen.contains(c)) {
				res.add(c);
				seen.add(c);
			}
		}
		return res;
	}

	public static Set<String> getSampleIds(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<String> res = new HashSet<>();
		for (Biosample b : biosamples) {
			if(b==null) continue;
			res.add(b.getSampleId());
		}
		return res;
	}

	public static Set<String> getSampleNames(Collection<Biosample> biosamples) {
		if(biosamples==null) return null;
		Set<String> res = new HashSet<>();
		for (Biosample b : biosamples) {
			if(b==null || b.getSampleName()==null || b.getSampleName().length()==0) continue;
			res.add(b.getSampleName());
		}
		return res;
	}


	/**
	 * Check if the animal is dead at the given phase.
	 * Note, if the animal is marked dead at d5, he is still not considered dead at d5 (not inclusive)
	 * @param phase
	 * @return
	 */
	public boolean isDeadAt(Phase phase) {
		return isDeadAt(phase, false);
	}

	/**
	 * Check if the animal is dead at the given phase.
	 * If the animal is marked dead at d5, and if we ask if he is dead at d5, the result depends of the inclusive flag
	 * @param phase
	 * @return
	 */
	public boolean isDeadAt(Phase phase, boolean inclusiveOfDateOfDeath) {
		assert phase!=null;

		if(getStatus()!=null && !getStatus().isAvailable() && getEndPhase()==null) {
			return true;
		}
		Phase last = getExpectedEndPhase();
		if(last==null) return false;
		if(inclusiveOfDateOfDeath) return last.getTime()<=phase.getTime();
		else return last.getTime()<phase.getTime();
	}

	/**
	 * @param study the attachedStudy to set
	 */
	public void setAttachedStudy(Study study) {
		if(study==attachedStudy) return;

		if(this.attachedStudy!=null) {
			this.attachedStudy.getParticipants().remove(this);
		}
		this.attachedStudy = study;
		if(this.attachedStudy!=null) {
			this.attachedStudy.getParticipants().add(this);
		}

		setInheritedStudy(study!=null?study: getParent()==null? null: getParent().getInheritedStudy());
	}

	/**
	 * @return the attachedStudy
	 */
	public Study getAttachedStudy() {
		return attachedStudy;
	}

	/**
	 * Returns a hierarchy of parents including self:
	 * @return [TOP, ..., parent, this]
	 */
	public List<Biosample> getParentHierarchy(){
		LinkedList<Biosample> res = new LinkedList<>();
		Biosample current = this;
		do {
			res.addFirst(current);
			current = current.getParent();
		} while(current!=null && current!=current.getParent());
		return res;

	}

	/**
	 * @param attachedSampling the attachedSampling to set
	 */
	public void setAttachedSampling(Sampling attachedSampling) {
		this.attachedSampling = attachedSampling;
	}

	/**
	 * @return the attachedSampling
	 */
	public Sampling getAttachedSampling() {
		return attachedSampling;
	}

	public int getInheritedSubGroup() {
		return inheritedSubgroup==null?0 :inheritedSubgroup;
	}

	public void setInheritedSubGroup(int inheritedSubgroup) {
		this.inheritedSubgroup = inheritedSubgroup;
		//		addAction(new ActionMoveGroup(this, getInheritedPhase(), getInheritedGroup(), getInheritedSubGroup()));
	}

	public Status getStatus() {
		return status==null? Status.INLAB: status;
	}

	public void setStatus(Status status) {
		if(status==this.status) return;
		setStatus(status, null);
	}

	public void setStatus(Status status, Phase atPhase) {
		this.endPhase = atPhase;
		this.status = status;
	}

	/**
	 * Get the studyAction for the given animal and phase
	 * if the group was split, this function will return the studyaction from the originating group
	 * @param phase
	 * @return
	 */
	public StudyAction getStudyAction(Phase phase) {
		if(phase==null || getAttachedStudy()==null) return null;
		return getAttachedStudy().getStudyAction(phase, this);
	}

	/**
	 * Returns the last expected status
	 * @return a pair of <Status,Phase>
	 */
	public Pair<Status, Phase> getLastActionStatus() {
		Phase phase = getAttachedStudy()==null? null: getExpectedEndPhase();
		return new Pair<Status, Phase>(status, phase);
	}

	public Phase getExpectedEndPhase() {
		Biosample animal = getTopParentInSameStudy();
		if(!this.equals(animal)) {
			return animal.getExpectedEndPhase();
		} else {
			if(getEndPhase()!=null && getStatus()!=null && !getStatus().isAvailable()) {
				return getEndPhase();
			} else {
				if(getInheritedGroup()==null) {
					return null;
				} else {
					return getInheritedGroup().getEndPhase(getInheritedSubGroup());
				}
			}
		}
	}

	public Phase getEndPhase() {
		return endPhase;
	}

	public void setEndPhase(Phase endPhase) {
		this.endPhase = endPhase;
	}

	public static Map<String, List<Biosample>> mapContainerId(Collection<Biosample> biosamples){
		Map<String, List<Biosample>> res = new HashMap<>();

		for(Biosample b: biosamples) {
			if(b.getContainerId()!=null) {
				List<Biosample> l = res.get(b.getContainerId());
				if(l==null) {
					l = new ArrayList<Biosample>();
					res.put(b.getContainerId(), l);
				}
				l.add(b);
			}
		}
		return res;
	}



	/**
	 * Compare group, subgroup, phase, name, sampleId
	 *
	 */
	public final static Comparator<Biosample> COMPARATOR_GROUP_SAMPLENAME = new Comparator<Biosample>() {
		@Override
		public int compare(Biosample o1, Biosample o2) {
			if(o2==null) return -1;
			if(o2==o1) return 0;

			//Compare groups
			int c = o1.getInheritedGroup()==null? (o2.getInheritedGroup()==null?0:1): o1.getInheritedGroup().compareTo(o2.getInheritedGroup());
			if(c!=0) return c;

			c = o1.getInheritedSubGroup() - o2.getInheritedSubGroup();
			if(c!=0) return c;

			//Compare phases
			c = o1.getInheritedPhase()==null? (o2.getInheritedPhase()==null?0:1): o1.getInheritedPhase().compareTo(o2.getInheritedPhase());
			if(c!=0) return c;

			//Compare SampleName
			c =  CompareUtils.compare((o1.getSampleName()==null? "": o1.getSampleName()), (o2.getSampleName()==null? "": o2.getSampleName()));
			if(c!=0) return c;

			//Compare SampleId
			c =  (o1.getSampleId()==null? "": o1.getSampleId()).compareTo((o2.getSampleId()==null? "": o2.getSampleId()));
			if(c!=0) return c;

			//Should never happen
			return o1.getId()-o2.getId();
		}
	};

	public final static Comparator<Biosample> COMPARATOR_POS = new Comparator<Biosample>() {
		@Override
		public int compare(Biosample o1, Biosample o2) {
			if(o2==null) return -1;
			int c = CompareUtils.compare(o1.getLocation(), o2.getLocation());
			if(c!=0) return c;
			c = CompareUtils.compare(o1.getScannedPosition(), o2.getScannedPosition());
			if(c!=0) return c;
			return o1.getPos() - o2.getPos();
		}
	};

	public final static Comparator<Biosample> COMPARATOR_NAME = new Comparator<Biosample>() {
		@Override
		public int compare(Biosample o1, Biosample o2) {
			if(o2==null) return 1;
			return CompareUtils.compare(o1.getSampleNameOrId(), o2.getSampleNameOrId());
		}
	};


	public void addAuxResult(Result r) {
		List<Result> results = (List<Result>) getAuxiliaryInfos().get(AUX_RESULT_ALL);
		if(results==null) {
			results = new ArrayList<Result>();
			getAuxiliaryInfos().put(AUX_RESULT_ALL, results);
		}
		results.add(r);
	}

	/**
	 * Clear the Auxiliary results attached to the given phase only
	 * @param phase (null to clear all)
	 */
	public void clearAuxResults(Phase phase) {
		assert phase!=null;
		List<Result> results = (List<Result>) getAuxiliaryInfos().get(AUX_RESULT_ALL);
		if(results!=null) {
			List<Result> cleaned = new ArrayList<>();
			for (Result result : new ArrayList<>(results)) {
				if(!phase.equals(result.getInheritedPhase())) {
					cleaned.add(result);
				}
			}
			getAuxiliaryInfos().put(AUX_RESULT_ALL, cleaned);
		}
	}


	public List<Result> getAuxResults() {
		List<Result> results = (List<Result>) getAuxiliaryInfos().get(AUX_RESULT_ALL);
		return results==null? new ArrayList<Result>(): results;
	}
	/**
	 * Retrieved attached results with the given filters (null is used to retrieve all)
	 * @param t
	 * @param p
	 * @return
	 */
	public List<Result> getAuxResults(Test t, Phase p) {
		List<Result> results = (List<Result>) getAuxiliaryInfos().get(AUX_RESULT_ALL);
		List<Result> tmp = new ArrayList<Result>();
		if(results==null) return tmp;
		for (Result result : results) {
			if(t!=null && !result.getTest().equals(t)) continue;
			if(p!=null && !result.getInheritedPhase().equals(p)) continue;
			tmp.add(result);
		}
		return tmp;
	}

	public List<Result> getAuxResults(String testName, Phase p) {
		List<Result> results = (List<Result>) getAuxiliaryInfos().get(AUX_RESULT_ALL);
		List<Result> tmp = new ArrayList<Result>();
		if(results==null) return tmp;
		for (Result result : results) {
			if(testName!=null && !result.getTest().getName().equals(testName)) continue;
			if(p!=null && !result.getInheritedPhase().equals(p)) continue;
			tmp.add(result);
		}
		return tmp;
	}

	/**
	 * Gets the first auxiliary result corresponding to the parameters
	 * @param test (not null
	 * @param p (can be null, to return result from any phase)
	 * @return
	 */
	public Result getAuxResult(Test test, Phase p) {
		assert test!=null;
		return getAuxResult(test, p, new String[0]);
	}

	/**
	 * Gets the first auxiliary result corresponding to the parameters
	 * @param test (not null
	 * @param p (can be null, to return result from any phase)
	 * @param inputParams (can be null, to return results with any input params)
	 * @return
	 */
	public Result getAuxResult(Test test, Phase p, String[] inputParams) {
		assert test!=null;
		List<Result> results = (List<Result>) getAuxiliaryInfos().get(AUX_RESULT_ALL);
		if(results==null) return null;
		Result sel = null;
		loop: for (int i = 0; i < results.size(); i++) {
			Result result = results.get(i);
			if(!test.equals(result.getTest())) continue;
			if(p!=null && !result.getInheritedPhase().equals(p)) continue;
			if(inputParams!=null) {
				List<ResultValue> rvs = result.getResultValues(OutputType.INPUT);
				for (int j = 0; j < inputParams.length && j<rvs.size(); j++) {
					String v = rvs.get(j).getValue();
					if(CompareUtils.compare(v==null? "": v, inputParams[j])!=0) continue loop;
				}
			}
			if(sel!=null && sel.getUpdDate().after(result.getUpdDate())) continue;
			sel = result;
		}
		return sel;
	}

	/**
	 * Return the first children attached to the given sampling or null if none
	 * @param sampling (not null)
	 * @param phase (can be null, return)
	 * @return
	 */
	public Biosample getSample(Sampling sampling, Phase phase) {
		assert sampling!=null;
		for(Biosample b: getHierarchy(HierarchyMode.ATTACHED_SAMPLES)) {
			if(phase!=null && !phase.equals(b.getInheritedPhase())) continue;
			if(!sampling.equals(b.getAttachedSampling())) continue;
			return b;
		}
		return null;
	}

	public void setPos(int pos) {
		setLocPos(location, pos);
	}

	public void setLocation(Location location) {
		setLocPos(location, pos==null?-1: pos);
	}

	public LocPos getLocPos() {
		return location==null? null: new LocPos(location, pos==null?-1: pos);
	}

	public void setLocPos(LocPos locPos) {
		setLocPos(locPos==null? null: locPos.getLocation(), locPos==null?-1: locPos.getPos());
	}

	public void setLocPos(Location loc, int pos) {
		//Return if nothing is changed
		if(loc==this.location && (loc==null || this.pos==pos)) {
			return;
		}

		if(loc==null) {
			this.location.getBiosamples().remove(this);
			this.location = null;
			this.pos = -1;
		} else {
			if(this.location!=null) {
				this.location.getBiosamples().remove(this);
			}
			this.location = loc;
			this.pos = pos;

			this.location.getBiosamples().add(this);
		}
	}

	public String getDetails() {
		StringBuilder sb = new StringBuilder();
		sb.append(getInheritedStudy()==null?"": getInheritedStudy().getStudyId() + (getInheritedGroup()==null? "": " / " + getInheritedGroup().getShortName() + (getInheritedPhase()==null?"": " / "+getInheritedPhase().getShortName())));
		sb.append("\t");
		sb.append(getTopParentInSameStudy()==null?"": getTopParentInSameStudy().getSampleId());
		sb.append("\t");
		sb.append(getSampleId());
		sb.append("\t");
		sb.append(getContainerId()==null?"": getContainerId());
		sb.append("\t");
		sb.append(getLocation()==null?"": getLocation().toString());
		sb.append("\t");
		sb.append(getMetadataAsString());
		sb.append("\t");
		sb.append(getChildren()==null?"": getChildren().size()+" children");
		return sb.toString();
	}

	public int getRow() {
		if(getLocation()==null) return 0;
		return getLocation().getLabeling().getRow(getLocation(), getPos());
	}

	public int getCol() {
		if(getLocation()==null) return 0;
		return getLocation().getLabeling().getCol(getLocation(), getPos());
	}

	public String getLocationString(LocationFormat format, SpiritUser user) {
		StringBuilder sb = new StringBuilder();
		Location location = getLocation();
		if(location!=null) {
			Privacy privacy = location.getInheritedPrivacy();
			if(user==null || SpiritRights.canRead(location, user)) {
				switch(format) {
				case FULL_POS:
					sb.append(location.getHierarchyFull());
					break;
				case MEDIUM_POS:
					sb.append(location.getHierarchyMedium());
					break;
				case NAME_POS:
					sb.append(location.getName());
					break;
				}

				if(getLocation().getLabeling()!=LocationLabeling.NONE) {
					sb.append(":"+location.getLabeling().formatPosition(location, getPos()));
				}

			} else {
				sb.append(privacy.getName() + (location.getEmployeeGroup()!=null? " (" + location.getEmployeeGroup().getName() + ")": ""));
			}
		}
		return sb.toString();
	}

	public static void clearAuxInfos(Collection<Biosample> col) {
		for (Biosample b : col) {
			b.getAuxiliaryInfos().clear();
		}
	}

	public String getScannedPosition() {
		return scannedPosition;
	}

	public void setScannedPosition(String scannedPosition) {
		this.scannedPosition = scannedPosition;
	}

	public static List<Biosample> filter(Collection<Biosample> biosamples, Biotype biotype) {
		if(biotype==null) return null;
		List<Biosample> res = new ArrayList<>();
		for (Biosample biosample : biosamples) {
			if(biotype.equals(biosample.getBiotype())) {
				res.add(biosample);
			}
		}
		return res;
	}


	/**
	 * Don't use postLoad, called by getMetadata
	 */
	//	@PostLoad
	protected void postLoad() {
		this.metadataValues = new LinkedHashMap<>();
		if(getBiotype()==null || this.serializedMetadata==null) return;
		Map<Integer, String> res = MiscUtils.deserializeIntegerMap(this.serializedMetadata + (this.serializedMetadata2==null?"":this.serializedMetadata2));
		for (BiotypeMetadata mt : getBiotype().getMetadata()) {
			this.metadataValues.put(mt, res.get(mt.getId()));
		}
	}

	/**
	 * Careful: Never use preSave hibernate directive because of bugs, but call it directly
	 */
	public void preSave() throws Exception {
		if(this.metadataValues!=null) {
			Map<Integer, String> res = new LinkedHashMap<>();
			for (Entry<BiotypeMetadata, String> entry : this.metadataValues.entrySet()) {
				BiotypeMetadata bm = entry.getKey();
				String val = entry.getValue();
				res.put(bm.getId(), val);
			}

			String serialized = MiscUtils.serializeIntegerMap(res);
			if(serialized.length()>7900) throw new Exception("The metadata for "+this+" are too large: "+serialized.length()+" (max:7900)");
			if(serialized.length()>3950) {
				this.serializedMetadata = serialized.substring(0, 3950);
				this.serializedMetadata2 = serialized.substring(3950);
			} else {
				this.serializedMetadata = serialized;
				this.serializedMetadata2 = "";
			}
		}
	}

	/**
	 * Increment the sampleId by appending .1,.2,3,... if needed
	 * @param sampleId
	 * @return
	 */
	public static String incrementSampleId(String sampleId) {
		if(sampleId.indexOf('.')>0) {
			try {
				int suffix = Integer.parseInt(sampleId.substring(sampleId.indexOf('.')+1));
				return sampleId.substring(0, sampleId.indexOf('.')+1) + String.valueOf(suffix+1);
			} catch(Exception e) {
				//Switch to normal mode
			}

		}
		return sampleId+".1";
	}

	public static Map<Biotype, List<Biosample>> mapBiotype(Collection<Biosample> col) {
		Map<Biotype, List<Biosample>> map = new HashMap<>();
		if(col==null) return map;
		for (Biosample b : col) {
			if(b.getBiotype()==null) continue;
			List<Biosample> l = map.get(b.getBiotype());
			if(l==null) {
				l = new ArrayList<>();
				map.put(b.getBiotype(), l);
			}
			l.add(b);
		}
		return map;
	}

	public static Map<String, Biosample> mapSampleId(Collection<Biosample> col) {
		Map<String, Biosample> map = new HashMap<>();
		if(col==null) return map;
		for (Biosample b : col) {
			if(b.getSampleId()==null) continue;
			map.put(b.getSampleId(), b);
		}
		return map;
	}

	public String debugInfo() {
		return "[Bio:"+id+(parent==null && parent!=this?"":",parent="+parent.getId())+(topParent==null || topParent==this || topParent==parent?"":",top="+topParent.getId())+(inheritedStudy==null?"":",study="+inheritedStudy.getId())+"]";
	}

	/**
	 * Returns a string containing the differences between 2 samples (usually 2 different versions).
	 * The result is an empty string if there are no differences or if b is null
	 * @param b
	 * @return
	 */
	@Override
	public String getDifference(IAuditable b) {
		if(b==null) b = new Biosample();
		if(!(b instanceof Biosample)) return "";
		return MiscUtils.flatten(getDifferenceMap((Biosample)b)).replace("ACTION=", "");
	}

	/**
	 * Returns a map containing the differences between 2 samples (usually 2 different versions).
	 * The result is an empty string if there are no differences or if b is null
	 * @param b
	 * @return
	 */
	public Map<String, String> getDifferenceMap(Biosample b) {

		Map<String, String> map = new LinkedHashMap<>();
		if(b==null) return map;
		if(!CompareUtils.equals(getInheritedStudy(), b.getInheritedStudy())) {
			map.put("Study", getInheritedStudy()==null?"NA":getInheritedStudy().getStudyId());
		}
		if(!CompareUtils.equals(getInheritedGroup(), b.getInheritedGroup())) {
			map.put("Group", getInheritedGroup()==null?"": getInheritedGroup().getName());
		}
		if(!CompareUtils.equals(getInheritedPhase(), b.getInheritedPhase())) {
			map.put("Phase", getInheritedPhaseString());
		}
		if(!CompareUtils.equals(getContainerType(), b.getContainerType()) || !CompareUtils.equals(getContainerId(), b.getContainerId()) || !CompareUtils.equals(getContainerIndex(), b.getContainerIndex())) {
			map.put("Container", (getContainerType()==null? "NA": getContainerType().getName()) + (getContainerId()!=null && getContainerId().length()>0? " " + getContainerId():"") + (getContainerIndex()!=null && getContainerIndex()>0? ":"+getContainerIndex():""));
		}
		if(!CompareUtils.equals(getAmount(), b.getAmount())) {
			map.put("Amount", getAmount()==null?"NA": getAmount().toString());
		}
		if(!CompareUtils.equals(getLocation(), b.getLocation()) || getPos()!=b.getPos()) {
			map.put("Location", getLocationString(LocationFormat.FULL_POS, null));
		}
		if(!CompareUtils.equals(getSampleId(), b.getSampleId())) {
			map.put("SampleId", getSampleId());
		}
		if(!CompareUtils.equals(getBiotype(), b.getBiotype())) {
			map.put("Biotype", getBiotype()==null?"":getBiotype().getName());
		}
		if(getBiotype()!=null) {
			if(getBiotype().getSampleNameLabel()!=null && !CompareUtils.equals(getSampleName(), b.getSampleName())) {
				map.put(getBiotype().getSampleNameLabel(), getSampleName());
			}
			for (BiotypeMetadata bm : getBiotype().getMetadata()) {
				if(!CompareUtils.equals(getMetadataValue(bm), b.getMetadataValue(bm))) {
					map.put(bm.getName(), getMetadataValue(bm));
				}
			}
		}
		if(!CompareUtils.equals(getComments(), b.getComments())) {
			map.put("Comments", getComments());
		}
		if(!CompareUtils.equals(getStatus(), b.getStatus()) || !CompareUtils.equals(getEndPhase(), b.getEndPhase()) ) {
			map.put("Status", getStatus()==null?"NA":getStatus().getName() + (getEndPhase()==null?"": " at " + getEndPhase().getShortName()));
		}
		if(!CompareUtils.equals(getQuality(), b.getQuality())) {
			map.put("Quality", getQuality()==null?"NA":getQuality().getName());
		}
		if(!CompareUtils.equals(getCreUser(), b.getCreUser())) {
			map.put("Owner", getCreUser()==null?"NA":getCreUser());
		}
		if(!CompareUtils.equals(getLastAction(), b.getLastAction())) {
			if(getLastAction()!=null) {
				map.put("ACTION", getLastAction().getDetails());
			}
		}

		return map;
	}


	public String getLastActionSerialized() {
		return lastAction;
	}

	public void setLastActionSerialized(String s) {
		this.lastAction = s;
	}

	public ActionBiosample getLastAction() {
		return ActionBiosample.deserialize(getLastActionSerialized());
	}
	public void setLastAction(ActionBiosample lastAction) {
		this.lastAction = lastAction==null? null: lastAction.serialize();
	}

	public String getNextCloneId() {
		Set<String> childrenSampleIds = Biosample.getSampleIds(getChildren());
		for (int i = 0; i < 26; i++) {
			String sampleId = trimRight(getSampleId()) + (char) ('A'+i);
			if(!childrenSampleIds.contains(sampleId)) return sampleId;
		}
		return null;
	}

	/**
	 * Special trim function, which remove all non alphanumerical characters from the string
	 * (ie. '. label()' returns 'label'
	 * @param s
	 * @return
	 */
	private String trimRight(String s) {
		int i2 = s.length();
		for(;i2>0 && !Character.isLetterOrDigit(s.charAt(i2-1)); i2--) {}
		return s.substring(0, i2);
	}
}
