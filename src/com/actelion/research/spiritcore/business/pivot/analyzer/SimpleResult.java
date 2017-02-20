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

package com.actelion.research.spiritcore.business.pivot.analyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;

/**
 * Class used to represent a simple result, the test/input are supposed fixed
 * @author Joel Freyss
 *
 */
public class SimpleResult implements Comparable<SimpleResult> {
	private Group group;
	private Phase phase;
	private Biosample biosample;
	private Double doubleValue;
	private String value;
	
	public SimpleResult(Group group, Phase phase, Biosample biosample, Double value, String string) {
		assert string!=null && string.length()>0;
		this.group = group;
		this.phase = phase;
		this.biosample = biosample;
		this.doubleValue = value;
		this.value = string;
	}
	
	public Group getGroup() {
		return group;
	}
	
	public Phase getPhase() {
		return phase;
	}
	
	public String getPhaseString() {
		return phase==null?"": phase.getShortName();
	}
	
	public Double getDoubleValue() {
		return doubleValue;
	}
	
	public String getValue() {
		return value;
	}
	
	public Biosample getBiosample() {
		return biosample;
	}
	
	public String getLabel() {
    	String lbl =(getBiosample().getInheritedGroup()==null?"":getBiosample().getInheritedGroup().getShortName())
    			+ (getBiosample().getInheritedPhase()==null?"": "/" + getBiosample().getInheritedPhase().getShortName())
    			+ getBiosample()==null? "": " " + getBiosample().getTopParent().getSampleId();
    	return lbl;
	}
	
	@Override
	public String toString() {
		return getLabel() + " " + value;
	}
	
	@Override
	public int compareTo(SimpleResult o) {
		int c = group==null? (o.getGroup()==null?0: -1): group.compareTo(o.getGroup());
		if(c!=0) return c;
		
		c = phase==null? (o.getPhase()==null?0: -1): phase.compareTo(o.getPhase());
		if(c!=0) return c;

		c = biosample==null? (o.getBiosample()==null?0: -1): biosample.compareTo(o.getBiosample());
		if(c!=0) return c;
		
		return value.compareTo(o.value);
		
	}
	
	public static Set<Group> getGroups(Collection<SimpleResult> simpleResults) {
		Set<Group> groups = new TreeSet<>();
		for (SimpleResult r : simpleResults) {
			groups.add(r.getGroup());
		}
		return groups;
	}
	
	public static Set<Phase> getPhases(Collection<SimpleResult> simpleResults) {
		Set<Phase> phases = new TreeSet<>();
		for (SimpleResult r : simpleResults) {
			phases.add(r.getPhase());
		}
		return phases;
	}
	
	public static List<Double> getValues(Collection<SimpleResult> simpleResults) {
		List<Double> values = new ArrayList<>();
		for (SimpleResult r : simpleResults) {
			if(r.getDoubleValue()!=null) {
				values.add(r.getDoubleValue());
			}
		}
		return values;
	}
	
	public static Map<Group, List<Double>> groupingValuesPerGroup(Collection<SimpleResult> simpleResults) {
		Map<Group, List<Double>> map = new HashMap<>();
		for (SimpleResult r: simpleResults) {
			List<Double> doubles = map.get(r.getGroup());
			if(doubles==null) {
				map.put(r.getGroup(), doubles = new ArrayList<>());
			}
			if(r.getDoubleValue()!=null) {
				doubles.add(r.getDoubleValue());
			}
		}
		return map;
	}
	
	public static Map<Group, List<SimpleResult>> groupingPerGroup(Collection<SimpleResult> simpleResults) {
		Map<Group, List<SimpleResult>> map = new HashMap<>();
		for (SimpleResult r: simpleResults) {
			List<SimpleResult> list = map.get(r.getGroup());
			if(list==null) {
				map.put(r.getGroup(), list = new ArrayList<>());
			}
			list.add(r);
		}
		return map;
	}
	
}
