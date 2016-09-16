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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.util.Counter;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.CompareUtils;

@Entity
@Audited
@Table(name = "study_sampling")
@SequenceGenerator(name = "study_sampling_sequence", sequenceName = "study_sampling_sequence", allocationSize = 1)
public class NamedSampling implements Comparable<NamedSampling>, IObject {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "study_sampling_sequence")
	private int id = 0;

	/**
	 * The study for which this sampling was designed or none if any
	 */
	@ManyToOne(fetch=FetchType.LAZY, cascade={}, optional = true)
	@JoinColumn(name = "study_id")
	private Study study = null;

	@Column(name="name", nullable = false)
	private String name;

	/**Do we perform a necropsy after? */ 
	@Column(name="necropsy")
	private Boolean necropsy;
	
	@OneToMany(fetch = FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "study_sampling_id")
	private List<Sampling> samplings = new ArrayList<>();
	
	@Column(name="creuser")
	private String creUser = "";
		
	@Temporal(TemporalType.TIMESTAMP)
	private Date creDate = new Date();

	public NamedSampling() {}
	
	public NamedSampling(String name) {
		this.name = name;
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

	public List<Sampling> getAllSamplings() {
		return samplings;
	}
	

	/**
	 * Return the top sampling ordered
	 * @return
	 */
	public List<Sampling> getTopSamplings() {
		return getTopSamplings(getAllSamplings());
	}

	public void setAllSamplings(List<Sampling> samplings) {
		this.samplings = samplings;
	}

	
	public void setStudy(Study study) {
		this.study = study;
	}

	public Study getStudy() {
		return study;
	}

	public void remove() {
		if(study==null || study.getNamedSamplings()==null) return;
		for (Iterator<NamedSampling> iter = study.getNamedSamplings().iterator(); iter.hasNext();) {
			NamedSampling ns = iter.next();			
			if (ns.equals(this)) {
				iter.remove();
			}
		}
		
		for(Sampling s: getAllSamplings()) {
			for(Biosample b: s.getSamples()) {
				b.setAttachedSampling(null);
			}
			s.getSamples().clear();
		}
		
		study = null;
	}
	
	@Override
	public int hashCode() {
		return id;
	}


	@Override
	public boolean equals(Object obj) {
		if(this==obj) return true;
		if(!(obj instanceof NamedSampling)) return false;
		NamedSampling ns = (NamedSampling) obj;
		return getId() > 0 && getId() == ns.getId();
	}
	

	@Override
	public int compareTo(NamedSampling o) {
		if(this==o) return 0;
		int c = -CompareUtils.compare(getStudy(), o.getStudy());
		if(c!=0) return c;
		
		c = (isNecropsy()?1:0) - (o.isNecropsy()?1:0);
		if(c!=0) return c;
		
		c = CompareUtils.compare(getName(), o.getName());
		if(c!=0) return c;		
		return -(int)(getId()-o.getId());		
	}
	

	@Override
	public String toString() {
		return (getStudy()==null?"": getStudy().getStudyId() + " - ") + name;
	}

	public String getShortDetails() {
		StringBuilder sb = new StringBuilder();
		for (Sampling sampling : getAllSamplings()) {
			if(!sb.toString().contains(sampling.getBiotype().getName())) {
				if(sb.length()>0) sb.append(", ");
				if(sb.length()>20) {
					sb.append("...");
					break;
				}
				sb.append(sampling.getBiotype().getName());
			}
		}
		return getAllSamplings().size()+" samples: " +sb.toString();
	}

	public String getDetails() {
		StringBuilder sb = new StringBuilder();
		for (Sampling top : getTopSamplings()) {
			sb.append(getDetailsRec(0, top));
		}
		return sb.toString();
	}

	private String getDetailsRec(int depth, Sampling top) {
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 2+depth*2; i++) sb.append("&nbsp;");
		
		sb.append("-&nbsp;" + top.getDetailsWithMeasurements()+"<br>");
		
		for (Sampling child : top.getChildren()) {
			sb.append(getDetailsRec(depth+1, child));
		}
		return sb.toString();
		
	}

	private Sampling getSampling(int id) {
		if(id<=0) return null;
		for (Sampling s : getAllSamplings()) {
			if(s.getId()==id) return s;		
		}
		return null;
	}
	
	
	/**
	 * Copy all sampling items from the given input to this object.
	 * 
	 * @param input
	 */
	public void copyFrom(NamedSampling input) {
		if(this==input) throw new IllegalArgumentException("ns cannot be equal to this");
		setName(input.getName());
		setNecropsy(input.isNecropsy());
		setStudy(input.getStudy());
		
		
		//delete outdated sampling objects (or without id)
		for(Iterator<Sampling> iter = getAllSamplings().iterator(); iter.hasNext(); ) {
			Sampling s = iter.next();
			if(input.getSampling(s.getId())==null) {
				s.remove();
				iter.remove();
			}
		}

		//At this stage this.samplings have all an id
		//Either, there is a match between this and ns and we keep the link
		//Either, there is no match and we create a copy
		IdentityHashMap<Sampling, Sampling> input2this = new IdentityHashMap<>();
		for (Sampling inputSampling : input.getAllSamplings()) {
			Sampling thisSampling = getSampling(inputSampling.getId());
			if(thisSampling!=null) {
				//there is a match between this and ns and we keep the link
				input2this.put(inputSampling, thisSampling);				
			} else {
				//work with a copy (id>0 -> copy.id=sampling.id)
				thisSampling = new Sampling();
				thisSampling.setId(inputSampling.getId());
				getAllSamplings().add(thisSampling);
			}
			
			//always copy the attributes of the sampling
			thisSampling.setBiotype(inputSampling.getBiotype());
			thisSampling.setSampleName(inputSampling.getSampleName());
			thisSampling.setSerializedMetadata(inputSampling.getSerializedMetadata());
			thisSampling.setComments(inputSampling.getComments());
			thisSampling.setWeighingRequired(inputSampling.isWeighingRequired());
			thisSampling.setLengthRequired(inputSampling.isLengthRequired());
			thisSampling.setCommentsRequired(inputSampling.isCommentsRequired());
			thisSampling.setAmount(inputSampling.getAmount());
			thisSampling.setContainerType(inputSampling.getContainerType());
			thisSampling.setBlocNo(inputSampling.getBlocNo());
			thisSampling.setSamples(inputSampling.getSamples());
			thisSampling.setMeasurements(inputSampling.getMeasurements());
			
			input2this.put(inputSampling, thisSampling);
		}
		
		//Recreate hierarchy
		for (Sampling existingSampling : new ArrayList<Sampling>( input.getAllSamplings())) {
			Sampling s = input2this.get(existingSampling);
			s.setParent(input2this.get(existingSampling.getParent()));
			
			s.getChildren().clear();
			for (Sampling child : existingSampling.getChildren()) {
				s.getChildren().add(input2this.get(child));
			}			
		}
	}
	
	public NamedSampling duplicate() {
		NamedSampling res = new NamedSampling();
		res.setName(getName());
		res.setNecropsy(isNecropsy());
		IdentityHashMap<Sampling, Sampling> old2new = new IdentityHashMap<>();

		//Create new sampling objects
		for (Sampling existingSampling : getAllSamplings()) {
			Sampling s = new Sampling();			
			s.setBiotype(existingSampling.getBiotype());
			s.setSampleName(existingSampling.getSampleName());
			s.setSerializedMetadata(existingSampling.getSerializedMetadata());
			s.setComments(existingSampling.getComments());
			s.setWeighingRequired(existingSampling.isWeighingRequired());
			s.setLengthRequired(existingSampling.isLengthRequired());
			s.setCommentsRequired(existingSampling.isCommentsRequired());
			s.setAmount(existingSampling.getAmount());
			s.setContainerType(existingSampling.getContainerType());
			s.setBlocNo(existingSampling.getBlocNo());
			res.getAllSamplings().add(s);		
			old2new.put(existingSampling, s);
		}
		
		//Recreate hierarchy
		for (Sampling existingSampling : getAllSamplings()) {
			Sampling s = old2new.get(existingSampling);
			s.setParent(old2new.get(existingSampling.getParent()));
			
			for (Sampling child : existingSampling.getChildren()) {
				s.getChildren().add(old2new.get(child));
			}			
		}
		
		return res;
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
	
	/**
	 * Returns a map of (ContainerType/ContainerIndex) -> List of Samplings
	 * @return
	 */
	public Map<Pair<ContainerType, Integer>, List<Sampling>> getContainerToSamplingsMap() {
		Map<Pair<ContainerType, Integer>, List<Sampling>> map = new HashMap<>();
		
		for (Sampling s : getAllSamplings()) {
			Pair<ContainerType, Integer> key = s.getContainerType()==null? null: new Pair<ContainerType, Integer>(s.getContainerType(), s.getBlocNo());			
			List<Sampling> l = map.get(key);
			if(l==null) {
				l = new ArrayList<>();
				map.put(key, l);
			}
			l.add(s);
		}
		
		return map;
	}

	public String getHtmlBySampling() {
		StringBuilder sb = new StringBuilder();
		for(Sampling ns: getTopSamplings()) {
			getHtmlBySamplingRec(ns, 0, sb);
		}
		return sb.toString();
	}
	
	private void getHtmlBySamplingRec(Sampling ps, int depth, StringBuilder sb) {
		for (int i = 0; i < depth; i++) {
			sb.append("&nbsp;&nbsp;");
		}
		sb.append("-" + ps.getDetailsWithMeasurements() + "<br>");		
		for(Sampling ns: ps.getChildren()) {
			getHtmlBySamplingRec(ns, depth+1, sb);
		}
	}
	
	
	public String getHtmlByContainer() {
		StringBuilder sb = new StringBuilder();		
		NamedSampling ns = this;
	
		Map<Pair<ContainerType, Integer>, List<Sampling>> map = ns.getContainerToSamplingsMap();
		List<Pair<ContainerType, Integer>> keys = new ArrayList<>(map.keySet());
		Collections.sort(keys, new Comparator<Pair<ContainerType, Integer>>() {
			@Override
			public int compare(Pair<ContainerType, Integer> o1, Pair<ContainerType, Integer> o2) {
				return CompareUtils.compare(o1, o2);
			}
		});
		for (Pair<ContainerType, Integer> key: keys) {
			
			sb.append("<b style='font-size:105%'>");
			if(key!=null) sb.append("Container: " + key.getFirst() + (key.getSecond()==null?"": " " + key.getSecond())  );
			else sb.append("No Container");
			sb.append("</u></b><br>");
			sb.append("<div style='margin-left:20px'>");
			for (Sampling sampling : map.get(key)) {
				sb.append("- " +  sampling.toString() +"<br>");					
			}
			sb.append("</div>");
		}
		

		return sb.toString();
	}

	public boolean isNecropsy() {
		return necropsy==Boolean.TRUE;
	}
	public void setNecropsy(boolean necropsy) {
		this.necropsy = necropsy;
	}

	public String getDescription() {
		Counter<Biotype> counter = new Counter<>();
		for(Sampling s: samplings) {
			counter.increaseCounter(s.getBiotype());
		}
		StringBuilder sb = new StringBuilder();
		for (Biotype b : counter.getKeys()) {
			if(sb.length()>0) sb.append(", ");
			sb.append(counter.getCount(b) + " "+b.getName());
		}
		
		return sb.toString();
	}


	public static List<Sampling> getTopSamplings(Collection<Sampling> samplings) {
		List<Sampling> res = new ArrayList<>();
		for (Sampling s : samplings) {
			if(s.getParent()==null) res.add(s);
		}
		Collections.sort(res);
		return res;
	}


	
}
