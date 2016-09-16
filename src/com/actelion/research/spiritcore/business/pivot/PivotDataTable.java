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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.util.MiscUtils;

/**
 * The PivotDataTable class is responsible for pivot each datapoint, according to the given template.
 * 
 * If the results are formatted like:
 * Biosample Test Input1 Input2 Output1 Output2 Output3
 * 
 * This result has 3 datapoints: Output1, Output2, Output3
 * 
 *  
 *  
 * 
 * @author freyssj
 *
 */
public class PivotDataTable {
	private PivotTemplate template;
	private Set<TestAttribute> skippedAttributes;
	private List<PivotRow> pivotRows = new ArrayList<>();
	private List<PivotColumn> pivotColumns = new ArrayList<>();
	private List<Result> results;

	/**
	 * Creates and populate a PivotTable from the given results and using the given template
	 * @param results
	 * @param skippedAttributes
	 * @param template
	 */
	public PivotDataTable(List<Result> results, Set<TestAttribute> skippedAttributes, PivotTemplate template) {
		if(template==null) throw new IllegalArgumentException("Template cannot be null");
		
		if(template.getComputed()!=null) {
			template.getComputed().calculateComputedValues(results);
		}
		
		this.template = template;
		this.skippedAttributes = skippedAttributes;
		this.results = results;
		pivotRows.clear();
		pivotColumns.clear();
		
		if(results==null) return;
		Map<String, PivotRow> key2pivotRow = new HashMap<String, PivotRow>();		
		Map<String, PivotColumn> key2pivotColumn = new HashMap<String, PivotColumn>();

		
		boolean hasPhaseInColumn = false;
		for(PivotItem item: template.getPivotItems(Where.ASCOL)) {
			if(item==PivotItemFactory.STUDY_PHASE_DATE || item==PivotItemFactory.STUDY_PHASE_DAYS) {
				hasPhaseInColumn = true; //caution: don't sort by Phase if we specifies reference or labels!!!
				break;
			}
		}

		boolean hasAttributeInColumn = false;
		for(PivotItem item: template.getPivotItems(Where.ASCOL)) {
			if(item==PivotItemFactory.RESULT_OUTPUT) {
				hasAttributeInColumn = true;
				break;
			}
		}


		Set<Test> skippableTests = new HashSet<>();
		Set<Biosample> skippableSamples = new HashSet<>();		
		Set<String> skippable = new HashSet<>();
		
		Map<Test, List<Result>> mapTest = Result.mapTest(results);
					
		for(Map.Entry<Test, List<Result>> e: mapTest.entrySet()) {
			Test test = e.getKey();
			//Each result needs to be displayed, 
			//but we canSkip empty output when there exists an other non-empty output  
			canSkip: for(TestAttribute att: test.getAttributes()) {
				if(att.getOutputType()!=OutputType.OUTPUT) continue;
				for (Result r : results) {
					if(!r.getTest().equals(test)) continue;
					ResultValue value = r.getResultValue(att);
					if(value.getValue()!=null && value.getValue().length()>0) {
						skippableTests.add(test);
						break canSkip;
					}
				}
			}
		}
		
		Map<Biosample, List<Result>> map = Result.mapBiosample(results);
		for(Map.Entry<Biosample, List<Result>> e: map.entrySet()) {
			Biosample b = e.getKey();
			canSkip: for (Result r : e.getValue()) {
				for(ResultValue rv: r.getResultValues()) {
					if(rv.getAttribute().getOutputType()!=OutputType.OUTPUT) continue;
					if(rv.getValue()!=null && rv.getValue().length()>0) {
						skippableSamples.add(b);
						break canSkip;
					}
				}
			}
		}
			
		//
		//Put each result in the row and column defined by the template
		for(Map.Entry<Test, List<Result>> e: mapTest.entrySet()) {
			Test test = e.getKey();
			
			//Put each ResulValue in the appropriate cell
			for(TestAttribute att: test.getAttributes()) {
				if(att.getOutputType()!=OutputType.OUTPUT) continue;
				if(skippedAttributes!=null && skippedAttributes.contains(att)) continue;
				
				for (Result r : e.getValue()) {
					ResultValue rv = r.getResultValue(att);

					//Skip null values, only if there is an other non-null values (so the result is always shown)
					if(rv.getValue()==null || rv.getValue().length()==0){
						if(skippableTests.contains(test) && skippableSamples.contains(r.getBiosample()) ) continue;
						if(skippable.contains(test.getId()+"_"+r.getBiosample().getId())) continue;
					}
					skippable.add(test.getId()+"_"+r.getBiosample().getId());
	
					//Create the row if needed
					String rowKey = template.getRowKey(rv);
					PivotRow row = key2pivotRow.get(rowKey);
					if(row==null) {
						row = new PivotRow(this, rv, rowKey);
						key2pivotRow.put(rowKey, row);
					}
			
					//Create the column if needed
					String colKey = template.getColKey(rv);
					PivotColumn column = key2pivotColumn.get(colKey);
					if(column==null) {
						column = new PivotColumn(this, 
								hasPhaseInColumn? r.getInheritedPhase(): null, 
								hasAttributeInColumn? att: null, 
								colKey);
						key2pivotColumn.put(colKey, column);
					}
					
					//Add the value at the cell defined by the pivotrow and pivot column
					row.addValue(column, rv);
				}
			}			
		}
		
		
		pivotRows.addAll(key2pivotRow.values());
		pivotColumns.addAll(key2pivotColumn.values());
		
		Collections.sort(pivotColumns);
		Collections.sort(pivotRows);
	}
	
	

		
	public List<Result> getResults() {
		return results;
	}
	
	
	public List<PivotRow> getPivotRows() {
		return pivotRows;
	}

	public List<PivotColumn> getPivotColumns() {
		return pivotColumns;
	}

	public PivotTemplate getTemplate() {
		return template;
	}
	
	public Set<TestAttribute> getSkippedAttributes() {
		return skippedAttributes;
	}
	
	
	public PivotColumn getPivotColumn(String nameWithoutHtml) {
		for (PivotColumn c : pivotColumns) {
			String name = MiscUtils.removeHtmlAndNewLines(c.getTitle());
			if(name.equals(nameWithoutHtml)) return c;
		}
		return null;
	}
	
}
