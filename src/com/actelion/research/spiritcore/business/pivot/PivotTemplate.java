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

import java.awt.Image;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;

/**
 * The PivotTemplate describes how the PivotTable should be formatted.
 * Each results consists of a value and several elements (input, study, biosample).
 * Each of those elements can be displayed ASROW, ASCOL, ASCELL or be MERGEd
 * 
 * When the values are merged into one cell, one has to decide on the display: all, median, average,... std, ...
 * 
 * 
 * @author freyssj
 *
 */
public class PivotTemplate implements Serializable, Cloneable {
	
	public static enum Where {
		MERGE,
		ASROW,
		ASCOL,
		ASCELL
	}

	public static enum Aggregation {
		ALL_VALUES("All Values"),
		MEDIAN("Median"),
		AVERAGE("Average"),
		GEOMETRIC_MEAN("Geometric Mean"),
		MINIMUM("Minimum"),
		MAXIMUM("Maximum"),
		COUNT("Count"),
		HIDE("Hide Values"),
		;
		
		private String name;		
		public String getName() {return name;}
		private Aggregation(String name){this.name = name;}
		@Override
		public String toString() {return name;}
	}
		
	public static enum Deviation {
		NONE("No dispersion"),		
		STD("Std.Dev."),
		COEFF_VAR("Coeff.Of.Var.");
		private String name;		
		public String getName() {return name;}
		private Deviation(String name) {this.name = name;}
		@Override
		public String toString() {return name;}
	}
	
	private final Map<PivotItem, Where> item2where = new HashMap<>();
	
	/**Computed only when getWhere is accessed*/
	private final Map<Where, List<PivotItem>> where2items = new HashMap<>();

	
	private Aggregation aggregation = Aggregation.MEDIAN;
	private Deviation deviation = Deviation.NONE;
	private Computed computed = Computed.NONE;
	
	private boolean showN = true;

	private String name;
	private String thumbnailName;
	
	public PivotTemplate() {}

	public PivotTemplate(String name, String thumbnailName) {
		this.name = name;
		this.thumbnailName = thumbnailName;
	}
	
	public void setName(String name) {
		this.name = name;
	}
		
	public synchronized void setWhere(PivotItem item, Where where) {		
		if(item==null) throw new IllegalArgumentException("item is null");
		if(where==null) throw new IllegalArgumentException("where is null");
		
		item2where.put(item, where);
		where2items.clear();
	}
	private synchronized void remove(PivotItem item) {
		item2where.remove(item);
		where2items.clear();
	}
	
	
	public synchronized void clear() {
		item2where.clear();
		where2items.clear();
		setWhere(PivotItemFactory.RESULT_OUTPUT, Where.ASCOL);
		this.aggregation = Aggregation.MEDIAN;
		this.computed = Computed.NONE;
		this.deviation = Deviation.NONE;
		this.showN = true;
				
	}
	
	
	
	public void setAggregation(Aggregation display1) {
		this.aggregation = display1;
	}
	
	public Aggregation getAggregation() {
		return aggregation;
	}
	
	public void setDeviation(Deviation display2) {
		this.deviation = display2;
	}
	
	public Deviation getDeviation() {
		return deviation;
	}	
	
	public Where getWhere(PivotItem item) {
		Where w =  item2where.get(item);
		if(w!=null) return w;
		return Where.MERGE;
	}
	
	public List<PivotItem> getPivotItems() {
		return getPivotItems(null);
	}
	
	public synchronized List<PivotItem> getPivotItems(Where where) {		
		if(where2items.size()==0) {
			where2items.put(null, new ArrayList<PivotItem>());
			where2items.put(Where.ASCELL, new ArrayList<PivotItem>());
			where2items.put(Where.ASCOL, new ArrayList<PivotItem>());
			where2items.put(Where.ASROW, new ArrayList<PivotItem>());
			where2items.put(Where.MERGE, new ArrayList<PivotItem>());
			
			for(PivotItem item: item2where.keySet()) {
				where2items.get(item2where.get(item)).add(item);
				where2items.get(null).add(item);
			}
			for (List<PivotItem> l : where2items.values()) {
				Collections.sort(l, PivotItem.COMPARATOR_SORTORDER);					
			}
		}
		return where2items.get(where);
	}
	
	public Computed getComputed() {
		return computed;
	}
	public void setComputed(Computed computed) {
		this.computed = computed;
	}

	
	@Override
	public String toString() {
		StringBuilder sb = new  StringBuilder();
		for ( PivotItem item : item2where.keySet()) {
			sb.append(item.getSortOrder()+"-"+item +" "+item2where.get(item)+"\n");
		}
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof PivotTemplate) && this.getName().equals(((PivotTemplate)obj).getName());
	}
	
	public boolean isShowN() {
		return showN && aggregation!=Aggregation.COUNT;
	}
	public void setShowN(boolean showN) {
		this.showN = showN;
	}
	
	
	@Override
	public PivotTemplate clone() {
		PivotTemplate tpl = new PivotTemplate();
		tpl.item2where.putAll(item2where);
		tpl.computed = computed;
		tpl.aggregation = aggregation;
		tpl.deviation = deviation;
		tpl.showN = showN;
		return tpl;	
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	private transient Image image;
	public Image getThumbnail() {
		if(thumbnailName!=null && image==null) {
			image = PivotTemplate.getThumbnail(thumbnailName);
			if(image==null) thumbnailName = null;
		}
		return image;
	}
	public String getThumbnailName() {
		return thumbnailName;
	}
	
	/**
	 * All all possible related data at the proper position.
	 * ie. if group is ASROW, we add all group columns ASROW 
	 * 
	 * we consider the entities:
	 * Group
	 * Phase
	 * Sample
	 * 
	 * @param results
	 * @param forResults
	 */
	public void expand(List<Result> results, SpiritUser user) {
		List<PivotItem> all = new ArrayList<>();
		for(PivotItem item: PivotItemFactory.ALL) all.add(item);
		all.addAll(PivotItemFactory.getPossibleItems(results, user));
		
		Map<String, List<PivotItem>> map = PivotItem.mapClassifier(all);
		for(String classifier: map.keySet()) {
			
			//Find where items sharing the same classifier are
			Boolean presentAsRow = null;
			for (PivotItem pivotItem : map.get(classifier)) {				
				Where r = getWhere(pivotItem);
				if(r==Where.ASCELL || r==Where.ASCOL) presentAsRow = false;
				if(r==Where.ASROW && presentAsRow==null) presentAsRow = true;
			}
			//move all items of the same classifier together
			if(presentAsRow==Boolean.TRUE) {
				for (PivotItem pivotItem : map.get(classifier)) {
					if(getWhere(pivotItem)!=Where.ASROW) {
						setWhere(pivotItem, Where.ASROW);
					}
				}
			}
		}
	}
	
	
	/**
	 * Simplify is used to hide all items where discrimination of the given results is not possible (ie less than 2 different values) 
	 * @param results
	 * @param forResults
	 */
	public void simplify(List<Result> results) {
		
		if(results==null) return;
		List<PivotItem> all = getPivotItems();

		//Hide all items having less than 2 values (no discrimination possible)
		for (PivotItem pivotItem : all) {
//			if(pivotItem==PivotItemFactory.RESULT_INPUT) continue; //don't skip input if it is in the template
//			if(pivotItem==PivotItemFactory.BIOSAMPLE_TOPID) continue; //don't skip top if it is in the template
//			System.out.println("PivotTemplate.simplify() "+pivotItem+" > "+pivotItem.isDiscriminating(results));
			if(!pivotItem.isDiscriminating(results)) {			
				remove(pivotItem);
			}
		}
	}
	
	/**
	 * To be overriden by subclasses, to initialize the template from the given results
	 * @param results
	 */
	public void init(List<Result> results) {}
	
	/**
	 * To be called by the app after init
	 * @param results
	 * @param user
	 */
	public void removeBlindItems(List<Result> results, SpiritUser user) {
		for (PivotItem pv : new ArrayList<PivotItem>(item2where.keySet())) {			
			//Skip not blind items
			if(item2where.get(pv)==Where.MERGE) continue;
			if(!pv.isHideForBlinds()) continue;
			
			//Check if some results should be blinded
			boolean remove = false;
			for (Result r : results) {
				if(r.getStudy()!=null && SpiritRights.isBlind(r.getStudy(), user)) {
					remove = true;
					break;
				}
			}
			
			if(remove) {
				setWhere(pv, Where.MERGE);
			}		
		}
	}
	
	private static Image getThumbnail(String name) {
		try {
			URL url = PivotTemplate.class.getResource(name);
			return ImageIO.read(url);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void copyDisplaySettings(PivotTemplate from) {
		if(from==null) return;
		setAggregation(from.getAggregation());
		setComputed(from.getComputed());
		setDeviation(from.getDeviation());
		setShowN(from.isShowN());
	}
	
	
	public String getRowKey(ResultValue rv) {
		StringBuilder key = new StringBuilder();
		for(PivotItem item: getPivotItems(Where.ASROW)) { 
			String s = item.getTitle(rv);
			if(s!=null) key.append(s);
			key.append("\n");
		}
		return key.toString();
	}
	
	public String getColKey(ResultValue rv) {
		StringBuilder key = new StringBuilder();
		for(PivotItem item: getPivotItems(Where.ASCOL)) {
			String s = item.getTitle(rv);
			if(s!=null) key.append(s);
			key.append("\n");
		}
		if(key.length()==0) {
			key.append(getAggregation().getName()); //Generic name			
		}

		return key.toString();
	}
	
	private String getCellKey(ResultValue rv) {
		StringBuilder key = new StringBuilder();
		for(PivotItem item: getPivotItems(Where.ASCELL)) {
			String s = item.getTitle(rv);
			if(s!=null) key.append(s);
			key.append("\n");
		}
		if(key.length()==0) {
			key.append(getAggregation().getName()); //Generic name			
		}

		return key.toString();
	}
	
	public static boolean isPopulated(PivotItem discrimator, List<Result> results) {
		for(Result result: results) {
			for(ResultValue rv: result.getOutputResultValues()) {
			
				if(rv.getValue()==null || rv.getValue().length()==0) continue;
				
				String key = discrimator.getTitle(rv);
				if(key!=null && key.length()>0) return true;
			}
		}
		return false;
	}
	
	public boolean isDiscriminating(PivotItem discrimator, List<Result> results) {
		return isDiscriminating(discrimator, results, 0);
	}
	
	/**
	 * Checks if the given discriminator helps to categorizes more than 10% of the results
	 * @param discrimator
	 * @param results
	 * @return
	 */
	public boolean isDiscriminating(PivotItem discrimator, List<Result> results, double percentage) {
		Set<String> keysWithout = new HashSet<>();
		Set<String> keysWith = new HashSet<>();

		int count = 0;
		for(int i=0; i<results.size(); i++) {
			Result result = results.get(i);
			for(ResultValue rv: result.getOutputResultValues()) {
			
				if(rv.getValue()==null || rv.getValue().length()==0) continue;
				
				String key = getRowKey(rv) + "|" + getColKey(rv) + "|" + getCellKey(rv);
				String key2 = key + "|" + discrimator.getTitle(rv);
				if(keysWithout.contains(key)) {
					if(!keysWith.contains(key2)) {
						count++;
						if(percentage==0) return true;
					}
					keysWith.add(key2);
				} else {
					keysWithout.add(key);
					keysWith.add(key2);
				}
			}
			if(percentage>0) {
				if(i>10) i++;
				if(i>100) i+=2;
				if(i>1000) i+=12;
				if(i>10000) i+=100;
			}
		}
		return count>keysWith.size()*percentage;
		
	}
	
}
