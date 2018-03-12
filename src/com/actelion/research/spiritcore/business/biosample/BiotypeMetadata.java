/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

/**
 * Describes the metadata of a Biosample
 *
 */
@Entity
@Audited
@BatchSize(size=100)
@Table(name="biotype_metadata", indexes = {
		@Index(name="biotypemetatada_type_index", columnList = "biotype_id")
})
@SequenceGenerator(name="biotype_metadata_sequence", sequenceName="biotype_metadata_sequence", allocationSize=1)
public class BiotypeMetadata implements Serializable, Comparable<BiotypeMetadata> {

	public static final String STAINING = "Staining";
	public static final String SECTIONNO = "SectionNo";

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="biotype_metadata_sequence")
	@Column(name="id")
	private int id = 0;

	@Column(name="name", nullable=false)
	private String name = "";

	@ManyToOne( fetch=FetchType.LAZY, cascade={}, optional=false)
	@JoinColumn(name="biotype_id")
	@Audited(targetAuditMode=RelationTargetAuditMode.NOT_AUDITED)
	private Biotype biotype = null;

	@Column(name="datatype", nullable=false)
	@Enumerated(EnumType.STRING)
	private DataType dataType = DataType.ALPHA;

	@Column(name="required", nullable=false)
	private boolean required = false;

	@Column(name="hideFromDisplay")
	private Boolean secundary = false;

	@Column(name="parameters", length=4000)
	private String parameters;

	@Column(name="idx", nullable=false)
	private int index = 0;

	public BiotypeMetadata() {
	}

	public BiotypeMetadata(int id) {
		this.id = id;
	}

	public BiotypeMetadata(String name, DataType dataType) {
		super();
		this.name = name;
		this.dataType = dataType;
	}

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

	public Biotype getBiotype() {
		return biotype;
	}

	public DataType getDataType() {
		return dataType;
	}

	public boolean isRequired() {
		return required;
	}

	public void setSecundary(boolean secundary) {
		this.secundary = secundary;
	}

	public boolean isSecundary() {
		return secundary==Boolean.TRUE;
	}

	public void setBiotype(Biotype biotype) {
		this.biotype = biotype;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String[] getParametersArray() {
		if(this.parameters==null) return new String[0];
		return MiscUtils.split(this.parameters);
	}

	public void setParametersArray(String[] parameters) {
		this.parameters = MiscUtils.unsplit(parameters);
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public int getIndex() {
		return index;
	}


	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof BiotypeMetadata)) return false;
		if(obj==this) return true;
		BiotypeMetadata mt2 = ((BiotypeMetadata)obj);
		if(getId()>0) return (getId() == mt2.getId());
		else {
			if(getBiotype()!=null && mt2.getBiotype()!=null && !getBiotype().equals(mt2.getBiotype())) return false;
			return getName().equals(((BiotypeMetadata)obj).getName());
		}
	}

	@Override
	public int compareTo(BiotypeMetadata o) {
		int c = CompareUtils.compare(getBiotype(), o.getBiotype());
		if(c!=0) return c;
		c = getIndex() - o.getIndex();
		if(c!=0) return c;
		return (getName()==null?"":getName()).compareTo((o.getName()==null?"":o.getName()));
	}

	@Override
	public String toString() {
		return name;
	}

	public List<String> extractChoices(){
		return splitChoices(parameters);
	}

	public String extractUnit() {
		int index = getName().indexOf("[");
		int index2 = getName().indexOf("]");

		if(index>0 && index<index2) {
			return getName().substring(index+1, index2);
		} else {
			return "";
		}

	}

	public static List<String> splitChoices(String csvChoices){
		List<String> res = new ArrayList<String>();
		if(csvChoices==null) return res;
		StringTokenizer st = new StringTokenizer(csvChoices, ",");
		while (st.hasMoreElements()) {
			String s = st.nextToken().trim();
			if(s.length()>0 && !res.contains(s)) res.add(s);
		}
		return res;
	}

	/**
	 * Clone this biotypemetadata. Careful, you will get 2 identical objects with a different reference,
	 * so it is important to change the id/name/biotype after calling this function
	 */
	@Override
	public BiotypeMetadata clone() {
		BiotypeMetadata m2 = new BiotypeMetadata();
		m2.setId(getId());
		m2.setBiotype(getBiotype());
		m2.setDataType(getDataType());
		m2.setName(getName());
		m2.setParameters(getParameters());
		m2.setRequired(isRequired());
		m2.setSecundary(isSecundary());
		return m2;
	}

	public static Map<BiotypeMetadata, String> deserialize(Biotype biotype, String metadataString) {
		Map<Integer,String> map = MiscUtils.deserializeIntegerMap(metadataString);
		Map<BiotypeMetadata, String> res = new HashMap<>();
		for (int id : map.keySet()) {
			assert id>0;

			if(id>0 && biotype.getMetadata(id)!=null) {
				res.put(biotype.getMetadata(id), map.get(id));
			}
		}
		return res;
	}

	public static String serialize(Map<BiotypeMetadata, String> metadata) {
		Map<Integer,String> map = new HashMap<>();
		for (Map.Entry<BiotypeMetadata, String> e : metadata.entrySet()) {
			assert e.getKey().getId()>0: "Cannot serialize metadata: "+metadata+": "+e.getKey()+" has an id of "+e.getKey().getId();
			if(e.getKey().getBiotype().getMetadata(e.getKey().getId())==null) throw new RuntimeException(e.getKey().getId()+" not in "+e.getKey().getBiotype().getMetadata());
			map.put(e.getKey().getId(), e.getValue());
		}
		return MiscUtils.serializeIntegerMap(map);
	}

	/**
	 * Comparator that compare all fields, to check if a modification occured
	 */
	public static Comparator<BiotypeMetadata> EXACT_COMPARATOR = new Comparator<BiotypeMetadata>() {
		@Override
		public int compare(BiotypeMetadata o1, BiotypeMetadata o2) {
			int c = CompareUtils.compare(o1.getName(), o2.getName());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.getDataType(), o2.getDataType());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.getParameters(), o2.getParameters());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.isRequired(), o2.isRequired());
			if(c!=0) return c;

			c = CompareUtils.compare(o1.isSecundary(), o2.isSecundary());
			if(c!=0) return c;

			return 0;
		}
	};


}
