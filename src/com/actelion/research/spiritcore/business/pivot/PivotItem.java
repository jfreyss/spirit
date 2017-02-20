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

package com.actelion.research.spiritcore.business.pivot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory.PivotItemBiosampleLinker;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.util.CompareUtils;



/**
 * Represents an element on which we allow pivoting
 * 
 * @author freyssj
 *
 */
public abstract class PivotItem implements Comparable<PivotItem>, Serializable {

	private static int index = 0;	
	private final static Map<String, Integer> id2sortOrder = new HashMap<String, Integer>();	
	
	private final String id;
	private final PivotItemClassifier classifier;
	private final String name;
	private final int sortOrder;
	
	PivotItem(PivotItemClassifier classifier, String name) {
		this.id = classifier.getLabel() + "." + name;
		this.classifier = classifier;
		this.name = name;
		if(id2sortOrder.get(id)!=null) {
			this.sortOrder = id2sortOrder.get(id);
		} else {
			this.sortOrder = ++index;
			id2sortOrder.put(id, sortOrder);
		}
	}

	
	public String getId() {
		return id;
	}
	
	public String getFullName() {
		return name;
	}
	
	public String getShortName() {
		return name.substring(getSubClassifier().length()).trim();
	}
	
	@Override
	public String toString() {
		return name;
	}	
	
	@Override
	public int compareTo(PivotItem o) {
		return COMPARATOR_SORTORDER.compare(this, o);
	}
	
	
	public abstract String getTitle(ResultValue rv);
	
	public String getTitleCleaned(ResultValue rv) {
		String s = getTitle(rv);
		if(s.length()>3 && s.charAt(0)=='<' && s.charAt(2)=='>') s = s.substring(3);
		return s;
	}
	
	public int getSortOrder() {
		return sortOrder;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof PivotItem)) return false;
		if(this==obj) return true;
		return compareTo((PivotItem)obj)==0;
	}
	
	@Override
	public int hashCode() {
		return sortOrder;
	}
	
	public PivotItemClassifier getClassifier() {
		return classifier;
	}
	
	public String getSubClassifier() {
		if(name.startsWith("[")) {
			int index = name.indexOf("]");
			if(index<0) return "";
			return name.substring(0, index+1);
		}
		return "";
	}
	
	
	/**
	 * Check if this pivotItems would return more than 2 values for the given results
	 * @param forResults
	 * @param results
	 * @return
	 */
	public boolean isDiscriminating(List<Result> results) {
		if(results==null || results.isEmpty()) return false;
		boolean isAlwaysShown = 
				this==PivotItemFactory.RESULT_INPUT
				|| this==PivotItemFactory.STUDY_GROUP		
				|| this==PivotItemFactory.BIOSAMPLE_NAME
				|| this==PivotItemFactory.RESULT_TEST;
		
		if(isAlwaysShown) {
			//If it is the ALWAYS_SHOWN list, we display it if one value is not null
			for (Result result : results) {
				for (ResultValue rv : result.getResultValues()) {
					if(rv.getAttribute().getOutputType()!=OutputType.OUTPUT) continue;
					String value = getTitle(rv);
					if(value!=null && value.length()>0) {
						return true;
					}
				}
			}
		} else {
			
			boolean first = true;
			String seenValue = null;
			for (Result result : results) {
				for(ResultValue rv: result.getResultValues()) {
					if(rv.getAttribute().getOutputType()!=OutputType.OUTPUT) continue;
					String value = getTitle(rv);
					if(first) {
						first = false;
						seenValue = value;
					} else if(!CompareUtils.equals(value, seenValue)) {
						//More than 2 different values, it is discriminating
						return true;
					}
				}
			}
		}
		return false;
	}

	
	/**
	 * Comparator used to 
	 */
	public static final Comparator<PivotItem> COMPARATOR_SORTORDER = new Comparator<PivotItem>() {		
		@Override
		public int compare(PivotItem o1, PivotItem o2) {
			int c;
			c = o1.getClassifier().compareTo(o2.getClassifier());
			if(c!=0) return c;

			if((o1 instanceof PivotItemBiosampleLinker) && (o2 instanceof PivotItemBiosampleLinker)) {
				BiosampleLinker l1 = ((PivotItemBiosampleLinker) o1).getLinker();
				BiosampleLinker l2 = ((PivotItemBiosampleLinker) o2).getLinker();
				return l1.compareTo(l2);
				
			} else if((o1 instanceof PivotItemBiosampleLinker) && !(o2 instanceof PivotItemBiosampleLinker)) {
				return 1;
			} else if(!(o1 instanceof PivotItemBiosampleLinker) && (o2 instanceof PivotItemBiosampleLinker)) {
				return -1;				
			} else {
				c = o1.getSortOrder() - o2.getSortOrder();
				if(c!=0) return c;				
			}
			
			
			return o1.getFullName().compareTo(o2.getFullName());
		}
	};
	

	public static Map<String, List<PivotItem>> mapClassifier(Collection<PivotItem> collection){
		List<PivotItem> items = new ArrayList<PivotItem>(collection);
		Collections.sort(items, COMPARATOR_SORTORDER);
		Map<String, List<PivotItem>> res = new LinkedHashMap<String, List<PivotItem>>();
		for (PivotItem pivotItem : items) {
			PivotItemClassifier classifier = pivotItem.getClassifier();
			
			
			String key = pivotItem.getSubClassifier().length()>0? pivotItem.getSubClassifier(): classifier.getLabel();
			List<PivotItem> l = res.get(key);
			if(l==null) {
				l = new ArrayList<PivotItem>();
				res.put(key, l);
			}
			l.add(pivotItem);
		}
		return res;
	}

	public boolean isHideForBlinds() {
		return false;
	}
	
}
