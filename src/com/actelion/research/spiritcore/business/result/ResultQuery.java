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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.util.SetHashMap;

public class ResultQuery implements Serializable {

	private Collection<Integer> stids = null;	
	private Collection<Integer> bids = new HashSet<>();
	
	private Phase phase;	
	private String phases;	
	private int sid;
	private String sampleId;
	private String containerIds;
	private String topSampleIds;
	private String studyIds;
	private String group;
	private String keywords;
	private String biotype;
	private String updUser;
	private String updDays;
	private String creDate;
	private String resultElbs;
	private Quality minQuality = null;
	private Set<Integer> testIds = new TreeSet<>();	
	private SetHashMap<TestAttribute, String> attribute2values = new SetHashMap<>();
	private Set<TestAttribute> skippedOutputAttributes = new HashSet<>();
	private Set<String> inputs = new TreeSet<>();	
	private Set<String> biotypes = new TreeSet<>();	
	
	public ResultQuery() { }
	
	public static ResultQuery createQueryForElb(String elbs) {
		ResultQuery query = new ResultQuery();
		query.setQuality(null);
		if(elbs!=null && elbs.length()>0) {
			query.setElbs(elbs);
		} else {
			query.setElbs("????");
		}
		
		return query;
	}

	public static ResultQuery createQueryForBiosampleId(int biosampleId) {
		ResultQuery query = new ResultQuery();
		query.setQuality(null);
		query.setBid(biosampleId);
		return query;
	}
	
	public static ResultQuery createQueryForBiosampleIds(Collection<Integer> biosampleIds) {
		ResultQuery query = new ResultQuery();
		query.setQuality(null);
		query.setBids(biosampleIds);
		return query;
	}

	public static ResultQuery createQueryForPhase(Phase phase) {
		ResultQuery query = new ResultQuery();
		query.setPhase(phase);
		query.setQuality(null);
		return query;
	}
	
	public static ResultQuery createQueryForStudyIds(String studyIds) {
		ResultQuery query = new ResultQuery();
		query.setStudyIds(studyIds);
		return query;
	}

	public static ResultQuery createQueryForSids(Collection<Integer> sids) {
		ResultQuery query = new ResultQuery();
		query.setSids(sids);
		return query;
	}

	
//	public static ResultQuery createQueryForTestIdPhaseId(Test test, Phase phase) {
//		ResultQuery query = new ResultQuery();
//		query.getTestIds().add((int)testId);
//		query.setPhaseId(phaseId);
//		query.setQuality(null);
//		return query;
//	}

//
//	public static ResultQuery createQueryForAnimalId(int animalId) {
//		ResultQuery query = new ResultQuery();
//		query.setQuality(null);
//		query.setAnimalId(animalId);
//		return query;
//	}
	
	public void copyFrom(ResultQuery query) {
		this.bids = query.bids;
		this.phase = query.phase;
		this.topSampleIds = query.topSampleIds;
		this.sampleId = query.sampleId;
		this.studyIds = query.studyIds;
		this.group = query.group;
		this.resultElbs = query.resultElbs;
		this.keywords = query.keywords;
		this.biotype = query.biotype;
		this.updUser = query.updUser;
		this.updDays = query.updDays;
		this.skippedOutputAttributes = query.skippedOutputAttributes;
		
		this.setQuality(query.minQuality);
		this.setTestIds(new HashSet<Integer>(query.testIds));
		this.setAttribute2Values(new SetHashMap<TestAttribute, String>(query.attribute2values));
	}
	
	
	public String getSampleIds() {
		return sampleId;
	}
	public void setSampleIds(String sampleId) {
		this.sampleId = sampleId;
	}
	public String getGroups() {
		return group;
	}
	public void setGroups(String group) {
		this.group = group;
	}
	public void setTestIds(Set<Integer> testIds) {
		this.testIds = testIds;
	}
	public Set<Integer> getTestIds() {
		return testIds;
	}
	public Collection<Integer> getBids() {
		return bids;
	}
	public void setElbs(String resultElbs) {
		this.resultElbs = resultElbs;
	}
	public String getElbs() {
		return resultElbs;
	}
	public void setStudyIds(String studyIdsString) {
		this.studyIds = studyIdsString;
	}
	public String getStudyIds() {
		return studyIds;
	}
	
	public void setBid(int biosampleId) {
		bids.clear();
		bids.add(biosampleId);
	}
	
	public void setBids(Collection<Integer> ids) {
		bids.clear();
		bids.addAll(ids);
	}

	public void setAttribute2Values(SetHashMap<TestAttribute, String> attributes) {
		this.attribute2values = attributes;
	}

	public SetHashMap<TestAttribute, String> getAttribute2Values() {
		return attribute2values;
	}

	public Set<String> getInputs() {
		return inputs;
	}
	public Set<String> getBiotypes() {
		return biotypes;
	}
	
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	public String getKeywords() {
		return keywords;		
	}
	
	public boolean isSpecificToStudy() {
		return sid!=0 || (studyIds!=null && studyIds.length()>0) || (phases!=null && phases.length()>0) || (group!=null && group.length()>0);
	}
	
	public void setPhase(Phase phase) {
		this.phase = phase;
	}

	public Phase getPhase() {
		return phase;
	}
	public String getBiotype() {
		return biotype;
	}
	public void setBiotype(String biotype) {
		this.biotype = biotype;
	}

	public String getUpdUser() {
		return updUser;
	}

	public void setUpdUser(String updUser) {
		this.updUser = updUser;
	}

	public String getUpdDate() {
		return updDays;
	}

	public void setUpdDate(String updDays) {
		this.updDays = updDays;
	}

	public Quality getQuality() {
		return minQuality;
	}
	public void setQuality(Quality quality) {
		this.minQuality = quality;
	}

	/**
	 * @param sid the study.id to set
	 */
	public void setSid(int sid) {
		this.sid = sid;
	}

	/**
	 * @return the studyId
	 */
	public int getSid() {
		return sid;
	}

	public String getTopSampleIds() {
		return topSampleIds;
	}

	public void setTopSampleIds(String topSampleIds) {
		this.topSampleIds = topSampleIds;
	}

	

	public String getQueryKey() {
		ResultQuery query = this;
		StringBuilder sb = new StringBuilder();
		if(query.getStudyIds()!=null) {
			sb.append(query.getStudyIds());				
			if(query.getGroups()!=null && query.getGroups().length()>0) {
				sb.append("/" + query.getGroups());				
			}
			if(query.getPhase()!=null) {
				sb.append("/" + query.getPhase());				
			}
		}
		if(query.getBiotype()!=null) {
			sb.append(" "+query.getBiotype());
		}
		

		if(query.getElbs()!=null) {
			sb.append(" "+query.getElbs());
		}
		if(query.getTopSampleIds()!=null) {
			sb.append(" "+query.getTopSampleIds());
		}
		if(query.getContainerIds()!=null) {
			sb.append(" "+query.getContainerIds());
		}
		if(query.getSampleIds()!=null) {
			sb.append(" "+query.getSampleIds());
		}
		if(query.getKeywords()!=null) {
			sb.append(" "+query.getKeywords());
		}

		if(query.getTestIds().size()>0) {
			for (Integer id : query.getTestIds()) {
				sb.append(" "+id);
			}
		}
		
		if(inputs.size()>0) {
			for (String s : inputs) {
				sb.append(" "+s);
			}
		}
		
		if(query.getUpdUser()!=null && query.getUpdUser().length()>0) {
			sb.append(" "+query.getUpdUser());
		}
		if(query.getUpdDate()!=null && query.getUpdDate().length()>0) {
			sb.append(" "+query.getUpdDate()+"days");
		}
		String n = sb.toString().trim();
		n = n.replaceAll("[ ]+", " ");
		n = n.replaceAll("[ ]+:", ":");
		return n;
	}	
	
	public String getCreDays() {
		return creDate;
	}
	public void setCreDays(String creDate) {
		this.creDate = creDate;
	}
	
	public void setCreDays(int days) {
		this.creDate = days<=0? null: days+" days";
	}
	
	public void setUpdDays(int days) {
		this.updDays = days<=0? null: days+" days";
	}
	
	public boolean isEmpty() {
		if(sid>0) return false;
		if(studyIds!=null && studyIds.length()>0) return false;
		
		String sugg = getQueryKey();		
		return sugg.length()==0 || sugg.equals("AllTests");
	}
	

	public Collection<Integer> getSids() {
		return stids;
	}
	public void setSids(Collection<Integer> stids) {
		this.stids = stids;
	}
	
	public Set<TestAttribute> getSkippedOutputAttribute() {
		return skippedOutputAttributes;
	}
	public void setSkippedOutputAttribute(Set<TestAttribute> skippedOutputAttributes) {
		this.skippedOutputAttributes = skippedOutputAttributes;
	}
	public String getPhases() {
		return phases;
	}
	public void setPhases(String phases) {
		this.phases = phases;
	}
	public String getContainerIds() {
		return containerIds;
	}
	public void setContainerIds(String containerIds) {
		this.containerIds = containerIds;
	}
//	public String getActNoOrElns() {
//		return actNoOrElns;
//	}
//	public void setActNoOrElns(String actNoOrElns) {
//		this.actNoOrElns = actNoOrElns;
//	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null) return false;
		if(!(obj instanceof ResultQuery)) return false;
		return getQueryKey().equals(((ResultQuery)obj).getQueryKey());
	}
}
