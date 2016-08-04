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

import java.util.List;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.util.CompareUtils;

public enum Computed {
	NONE("None", null) {
		@Override public String format(Double val) {return null;}
	},
	INC_DAY("Increase / Day [Absolute]", "Calculates the absolute increase from the last measured value and normalized by the number of days (if several values have been measured, only consider the first of the day)") {
		@Override public String format(Double val) {return val==null? null: (val>=0?"+":"") + Formatter.formatMax2(val) + "/d";}
	},
//	INC_REF("Inc. / RefPhase [Abs.]", "Calculates the absolute increase from the result measured at the reference phase (as defined in the study design / group definition)") {
//		@Override public String format(Double val) {return val==null? null: (val>=0?"+":"") + Formatter.formatMax2(val);}
//	},
//	INC_REF_PERCENT("Inc. / RefPhase [%]", "Calculates the relative increase from the result measured at the reference phase (as defined in the study design / group definition)") {
//		@Override public String format(Double val) {return val==null? null: (val>=0?"+":"") + Formatter.formatMax2(val) + "%";}
//	},
	INC_START("Increase / d0 [Absolute]", "Calculates the absolute increase from the result measured at d0 (or the closest to d0 if none)") {
		@Override public String format(Double val) {return val==null? null: (val>=0?"+":"") + Formatter.formatMax2(val);}
	},
	INC_START_PERCENT("Increase / d0 [%]", "Calculates the relative increase from the result measured at d0 (or the closest to d0 if none)") {
		@Override public String format(Double val) {return val==null? null: (val>=0?"+":"") + Formatter.formatMax2(val) + "%";}
	},
	;
	
	private String name;		
	private String tooltip;		
	private Computed(String name, String tooltip) {this.name = name; this.tooltip = tooltip;}
	public String getName() {return name;}
	public String getTooltip() {return tooltip;}
	@Override
	public String toString() {return name;}
	public abstract String format(Double val);
	
	
	public static final Computed getValue(String name) {
		for (Computed computed : values()) {
			if(computed.getName().equals(name)) return computed;			
		}
		return Computed.NONE;
	}
	
	/**
	 * Update the results by calculated the computed value
	 * @param results
	 */
	public void calculateComputedValues(List<Result> results) {
		if(results==null || results.size()==0) return;
		
		//Reset Calculated Values
		ListHashMap<String, Result> sid_cid_tid_inputs2Result = new ListHashMap<String, Result>();
		for (Result r : results) {
			for(ResultValue rv: r.getResultValues()) { 
				rv.setCalculatedValue(null);
			}
		}
		
		if(this==NONE) return;
		
		for (Result r : results) {
			String key = (r.getBiosample()==null?"":r.getBiosample().getId()) + "_" + r.getTest().getId()+"_"+r.getInputResultValuesAsString();
			sid_cid_tid_inputs2Result.add(key, r);
		}
		
		//Calculate value increase over phases
		if(this==INC_DAY) {
			loop: for (Result r : results) {
				if(r.getInheritedPhase()==null) continue;
				
				//Retrieve values from map
				String key = (r.getBiosample()==null?"":r.getBiosample().getId()) + "_" + r.getTest().getId()+"_"+r.getInputResultValuesAsString();
				List<Result> otherResults = sid_cid_tid_inputs2Result.get(key);
				if(otherResults.size()==0) continue;
				
				Result sel = null;
				for (Result r2 : otherResults) {
					//Find the result which was measured a day before, but only consider the first measure of the day
					if(r2.getInheritedPhase()==null || r2==r) continue;
					if(r2.getInheritedPhase().getDays()>r.getInheritedPhase().getDays()) continue;					
					if(r2.getInheritedPhase().getDays()==r.getInheritedPhase().getDays() && r2.getInheritedPhase().getTime()<r.getInheritedPhase().getTime()) continue loop;					
					if(sel!=null && sel.getInheritedPhase().getDays()>r2.getInheritedPhase().getDays()) continue;
					if(sel!=null && sel.getInheritedPhase().getDays()==r2.getInheritedPhase().getDays() && sel.getInheritedPhase().getTime()>r2.getInheritedPhase().getTime()) continue;
					sel = r2;
				}
				if(sel==null) continue;
				for(TestAttribute att : r.getTest().getOutputAttributes()) { 
					if(att.getDataType()!=DataType.NUMBER) continue;
					Double v1 = r.getResultValue(att).getDoubleValue();
					if(v1==null) continue;
					
					Double v2 = sel.getResultValue(att).getDoubleValue();
					if(v2==null) continue;
					double val = (v1 - v2) / (r.getPhase().getDays() - sel.getPhase().getDays()); 								
					val = ((int)(val*100))/100.0; //Round to 2 decimals
					r.getResultValue(att).setCalculatedValue(val);
				}
			}
		} else if(/*this==INC_REF || this==INC_REF_PERCENT ||*/ this==INC_START || this==INC_START_PERCENT) {
			for (Result r : results) {
				if(r.getPhase()==null) continue;
				
				//Retrieve values from map
				String key = (r.getBiosample()==null?"":r.getBiosample().getId()) + "_" + r.getTest().getId()+"_"+r.getInputResultValuesAsString();
				List<Result> resultFromOtherPhases = sid_cid_tid_inputs2Result.get(key);
				if(resultFromOtherPhases.size()==0) continue;
				
				Result sel = null;
				for (Result r2 : resultFromOtherPhases) {
					if(r2.getPhase()==null || Math.abs(r2.getPhase().getDays())>1 ) continue;
					if(sel==null) {
						sel = r2;
					} else if(Math.abs(r2.getPhase().getTime())<Math.abs(sel.getPhase().getTime())){
						sel = r2;
					}
				}
				
				
				if(sel==null || CompareUtils.compare( r.getPhase(), sel.getPhase())==0) continue;
				
				for(TestAttribute att : r.getTest().getOutputAttributes()) { 
					if(att.getDataType()!=DataType.NUMBER) continue;
					Double v1 = r.getResultValue(att).getDoubleValue();
					if(v1==null) continue;
	
					
					Double v2 = sel.getResultValue(att).getDoubleValue();
					if(v2==null) continue;
					Double val;
					if(/*this==INC_REF_PERCENT ||*/ this==INC_START_PERCENT) {
						val = v1>0 && v2>0? 100*(v1-v2)/v2: null;
					} else { //Absolute increase
						val = v1 - v2;
					}
					val = val==null? null: ((int)(val*100))/100.0; //Round to 2 decimals
					r.getResultValue(att).setCalculatedValue(val);
				}
			}
		} else {
			System.err.println("PivotTable.processComputedResults() invalid template.getComputed()="+this);
		}
	}
}