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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.Metadata;
import com.actelion.research.util.CompareUtils;

@Entity
@Audited
@Table(name="sampling")
@SequenceGenerator(name="sampling_sequence", sequenceName="sampling_sequence", allocationSize=1)
@BatchSize(size=16)
public class Sampling implements Comparable<Sampling>, Cloneable, Serializable {
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="sampling_sequence")
	private int id = 0;
	
	@ManyToOne(fetch=FetchType.LAZY, optional=true, cascade={})
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@JoinColumn(name="parent_sampling_id")
	private Sampling parent = null;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="parent", cascade={})
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	@BatchSize(size=32)
	private Set<Sampling> children = new TreeSet<>();
	
	@Column(name="containertype", nullable=true)
	@Enumerated(EnumType.STRING)
	private ContainerType containerType;
	
	private Double amount;

	@Column(name="locindex", nullable=true)
	private Integer blocNo;

	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.CASCADE)
	private Biotype biotype = null;
	
	@Column(name="samplename")
	private String sampleName;
	
	@Column(name="comments", length=256)
	private String comments;
	
	@Column(name="weighingrequired", nullable=false)
	private boolean weighingRequired = false;
	
	@Column(name="commentsrequired", nullable=false)
	private boolean commentsRequired = false;
	
	@Column(name="lengthrequired", nullable=false)
	private boolean lengthRequired = false;
	
	/**
	 * Metadata serialized as biotypeMetadataId=VAL;...
	 */
	@Column(name="parameters", length=1024)
	private String metadata;
	
	/**
	 * Extra Measurements, serialized as:
	 * testId1#Input1_1#Input1_2, testId2#Input1_1#Input1_2,
	 */
	@Column(name="extrameasurement", length=256)
	private String extraMeasurement;
	
	/**
	 * attachedSamples: refresh but with orphan removal (so that, remove will delete the samples)
	 */
	@OneToMany(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY, mappedBy="attachedSampling")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OrderBy(value="sampleId")
	@BatchSize(size=64)
	private Set<Biosample> samples = new HashSet<>();
	
	
	/**
	 * Reference to the namedSampling if any, this is not the owning side of the relationship and should only be used for querying
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@JoinColumn(name = "study_sampling_id", insertable=false, updatable=false)
	private NamedSampling namedSampling; 
	
	private transient Map<BiotypeMetadata, String> metadataMap; 

	private transient List<Measurement> extraMeasurementList; 

	/**
	 * Constructor for a default Sampling
	 */
	public Sampling() {}	
	
	public Biotype getBiotype() {
		return biotype;
	}
	public void setBiotype(Biotype biotype) {
		this.biotype = biotype;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}
	

	protected String getSerializedMetadata() {
		return metadata;
	}
	protected void setSerializedMetadata(String parameters) {
		this.metadata = parameters;
	}
	
	/**
	 * Return a Map of BiotypeMetadata -> value
	 * @return
	 */
	public Map<BiotypeMetadata, String> getMetadataMap() {
		if(metadataMap==null) {			
			metadataMap = BiotypeMetadata.deserialize(biotype, metadata);
		}
		return Collections.unmodifiableMap(metadataMap);

	}
	
	public void setMetadataMap(Map<BiotypeMetadata, String> metadataMap) {
		this.metadataMap = metadataMap;
	}
	
	
	public void setMetadata(BiotypeMetadata bm, String value) {
		Map<BiotypeMetadata, String> map = new HashMap<>(getMetadataMap());
		if(value==null) map.remove(bm);
		else map.put(bm, value);
		setMetadataMap(map);
	}
	
	public String getMetadata(BiotypeMetadata bm) {
		Map<BiotypeMetadata, String> map = new HashMap<>(getMetadataMap());
		return map.get(bm);
	}
	
	
	/**
	 * Gets short details (name or biotype) in plain text
	 * @return
	 */
	public String getDetailsShort() {		
		return (sampleName!=null? sampleName: biotype.getName());
	}

	public String getDetailsComplement() {
		String values = getMetadataValues();
		
		return  
			(values.length()>0? values: "") +
			(comments!=null? " "+comments:"");
	}

	/**
	 * Gets long details (name or biotype/metadata/comments) in plain text
	 * @return
	 */
	public String getDetailsLong() {
		String values = getMetadataValues();		
		return  
			(sampleName!=null && sampleName.length()>0? sampleName: biotype.getName()) +
			(values.length()>0? ": " + values: "") +
			(comments!=null && comments.length()>0? " "+comments:"");
	}
	
	/**
	 * Get full details on the sample in HTML
	 * @return
	 */
	public String getDetailsWithMeasurements() {
		String values = getMetadataValues();
		
		return "<b>" + biotype.getName() + "</b>" + 
			(sampleName!=null && sampleName.length()>0? " "+sampleName: "") +
			(values.length()>0? ": " + values: "") + 
			(comments!=null && comments.length()>0? " " + comments: "") + 
			(containerType!=null?" <b style='color:#777777'>["+containerType.getName()+ (blocNo!=null && containerType.isMultiple()? " " + blocNo:"") + "]</b>":"") +
			(amount!=null && biotype.getAmountUnit()!=null? " "+amount + biotype.getAmountUnit().getUnit():"") + 
			(hasMeasurements()? ("<span style='color:#0000AA'><b> [" +  
				(isWeighingRequired()?"w":"") +
				(isLengthRequired()?"l":"") + 
				(isCommentsRequired()?"o":"") +
				(getMeasurements().size()>0?""+getMeasurements().size():"") +
			"]</b></span>") : "");
	}
	

	public String getMetadataValues() {
		StringBuilder sb = new StringBuilder();
		if(biotype!=null) {
			for(BiotypeMetadata bm: biotype.getMetadata()) {
				String s = getMetadata(bm);
				if(s!=null && s.length()>0) {
					if(sb.length()>0) sb.append("; ");
					sb.append(s);
				}
			}
		}
		return sb.toString();
	}
	
	
	@Override
	public String toString() {
		String values = getMetadataValues();
		
		return biotype.getName() + 
		(sampleName!=null?" "+sampleName:"") +
		(values.length()>0? ": " + values: "") + 		
		(amount!=null && biotype.getAmountUnit()!=null? " "+amount + biotype.getAmountUnit().getUnit():"") + 
		(comments!=null? " "+comments:"");
	}
	
	/**
	 * Gives a matching score: 0 = No Match, 1= Perfect Match
	 * 
	 * @param biosample
	 * @return
	 */
	public double getMatchingScore(Biosample biosample) {
		if(biosample.getBiotype()==null || !biosample.getBiotype().equals(biotype)) return 0;
		int n = 0;
		int total = 0;
		
		//Check name
		if(biosample.getBiotype().getSampleNameLabel()!=null) {
			total++;
			if(getSampleName()!=null) {
				if(getSampleName().equalsIgnoreCase(biosample.getSampleName())) n++;
			} else {
				if(biosample.getSampleName()==null || biosample.getSampleName().length()==0) n++;
			}
		}
		
		//Check metadata
		for (BiotypeMetadata m : biotype.getMetadata()) {
			String data = getMetadata(m);
			Metadata me = biosample.getMetadata(m);
			total++;		
			if(data!=null) {
				if(me!=null && data.equalsIgnoreCase(me.getValue())) {
					n++;
				}
			} else {
				if(me==null  || me.getValue()==null || me.getValue().length()==0) {
					n++;
				}
			}
				
		}

		//Check comments
		total++;
		if(comments!=null) {
			if(comments.equals(biosample.getComments())) n++;
		} else {
			if(biosample.getComments()==null || biosample.getComments().length()==0) n++;			
		}
		
		//Check container
		total++;
		if(biosample.getContainerType()==containerType) n++;
		
		total++;
		if(getBlocNo()!=null) {
			if(biosample.getContainer()!=null && biosample.getContainer().getBlocNo()==getBlocNo()) n++;
		} else {
			if(biosample.getContainer()==null || biosample.getContainer().getBlocNo()==null) n++;			
		}
		
		return ((double)n) / total;
	}
	
	public Biosample createCompatibleBiosample() {
		Biosample biosample = new Biosample();
		biosample.setBiotype(biotype);
		if(containerType!=null) biosample.setContainer(new Container(containerType));
		populate(biosample);
		return biosample;
	}
	
	public void populate(Biosample biosample) {
		if(biotype==null) return;
		
		if(biosample.getBiotype()!=null && !biosample.getBiotype().equals(biotype)) {
			throw new IllegalArgumentException("The biotype cannot be changed, the metadata cannot be updated");
		}
		
		//name
		if(biotype.getSampleNameLabel()!=null) {
			biosample.setSampleName(getSampleName());
		}
		
		//parameters
		for (BiotypeMetadata m : biotype.getMetadata()) {
			String data = getMetadata(m);
			if(data!=null) biosample.getMetadata(m).setValue(data); 
		}
		
		//amount
		if(biotype.getAmountUnit()!=null) {
			biosample.setAmount(amount);
		}
		
		//Comments
		biosample.setComments(comments);
		
		
		
	}
	
	@Override
	public int compareTo(Sampling o) {
		int c = CompareUtils.compare(biotype, o.getBiotype());
		if(c!=0) return c;
		
		c = CompareUtils.compare(getSampleName(), o.getSampleName());
		if(c!=0) return c;
		
		c = CompareUtils.compare(getMetadataValues(), o.getMetadataValues());
		if(c!=0) return c;
		
		c = CompareUtils.compare(getComments(), o.getComments());
		if(c!=0) return c;
		
		c = CompareUtils.compare(getContainerType(), o.getContainerType());
		if(c!=0) return c;
		
		c = CompareUtils.compare(getBlocNo(), o.getBlocNo());
		if(c!=0) return c;
		
		if(getId()>0 && o.getId()>0) {
			c = (int)(getId()-o.getId());
			if(c!=0) return c;
		}
		
		return 0;
	}
	
	/**
	 * Equals is the (=) identity as sampling can be duplicated and still not be equal
	 */
	@Override
	public boolean equals(Object obj) {
		if(this==obj) return true;
		if(!(obj instanceof Sampling)) return false;
		if(this.getId()>0) return this.getId()==((Sampling)obj).getId();
		return false;
	}
	
	@Override
	public int hashCode() {
		return id;
	}
	
	public void remove() {
		for (Sampling s : getChildren()) {
			s.parent = parent;
		}

		//remove the link from the parent
		if(parent!=null) {
			parent.getChildren().remove(this);
		}
		
		//remove the attached samplings
		samples.clear();
		
		parent = null;
	}

	@Override
	public Sampling clone() {
		try {
			Sampling clone = new Sampling();
			clone.setSerializedMetadata(getSerializedMetadata());
			clone.setAmount(getAmount());
			clone.setBiotype(getBiotype());
			clone.setBlocNo(getBlocNo());
			clone.setComments(getComments());
			clone.setCommentsRequired(isCommentsRequired());
			clone.setContainerType(getContainerType());
			clone.setLengthRequired(isLengthRequired());
			clone.setSampleName(getSampleName());
			clone.setWeighingRequired(isWeighingRequired());
			clone.setMeasurementString(getMeasurementString());
			return clone;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
		
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(Sampling parent) {
		this.parent = parent;
	}

	/**
	 * @return the parent
	 */
	public Sampling getParent() {
		return parent;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(Set<Sampling> children) {
		this.children = children;
	}

	/**
	 * @return the children (ordered)
	 */
	public Set<Sampling> getChildren() {
		return children;
	}

	

	/**
	 * @param weighingRequired the weighingRequired to set
	 */
	public void setWeighingRequired(boolean weighingRequired) {
		this.weighingRequired = weighingRequired;
	}

	/**
	 * @return the weighingRequired
	 */
	public boolean isWeighingRequired() {
		return weighingRequired;
	}

	public boolean isCommentsRequired() {
		return commentsRequired;
	}

	public void setCommentsRequired(boolean commentsRequired) {
		this.commentsRequired = commentsRequired;
	}
	
	public boolean hasMeasurements() {
		return weighingRequired || commentsRequired || lengthRequired || getMeasurements().size()>0;
	}


	public ContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ContainerType locType) {
		this.containerType = locType;
	}

	/**
	 * Careful: starts at 1
	 */
	public Integer getBlocNo() {
		return blocNo;
	}

	/**
	 * Careful: starts at 1
	 * @param locIndex
	 */
	public void setBlocNo(Integer locIndex) {
		this.blocNo = locIndex;
	}

	public boolean isLengthRequired() {
		return lengthRequired;
	}

	public void setLengthRequired(boolean lengthRequired) {
		this.lengthRequired = lengthRequired;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getSampleName() {
		return sampleName;
	}

	public void setSampleName(String name) {
		this.sampleName = name;
	}

	/**
	 * @param samples the samples to set
	 */
	public void setSamples(Set<Biosample> samples) {
		this.samples = samples;
	}

	/**
	 * @return the samples
	 */
	public Set<Biosample> getSamples() {
		return samples;
	}

	public NamedSampling getNamedSampling() {
		return namedSampling;
	}
	
	public void setNamedSampling(NamedSampling namedSampling) {
		this.namedSampling = namedSampling;
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
	
	public static Set<Measurement> getMeasurements(Collection<Sampling> samplings) {		
		Set<Measurement> res = new HashSet<>();
		for (Sampling a: samplings) {
			res.addAll(a.getMeasurements());
		}		
		return res;
	}
	
	/**
	 * Called by Study.preSave() to serialize Measurements and samplings Metadata
	 */
	public void preSave() {
		if(extraMeasurementList!=null) {
			extraMeasurement = Measurement.serialize(extraMeasurementList);
		}
		if(metadataMap!=null) {
			this.metadata = BiotypeMetadata.serialize(metadataMap);
		}
	}
		
}