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

package com.actelion.research.spiritapp.spirit.services.report;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Sheet;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.SetHashMap;
import com.actelion.research.util.HtmlUtils;

public class SamplesMeasurementReport extends AbstractReport {

	private static ReportParameter SKIP_EMPTY_ANIMAL_PARAMETER = new ReportParameter("Skip samples without any measurements", Boolean.TRUE);
	private static ReportParameter MAX_PER_SHEET_PARAMETER = new ReportParameter("Maximum of 16 measurements / sheet", Boolean.TRUE);

	public SamplesMeasurementReport() {
		super(ReportCategory.SAMPLES, 
			"Results", 
			"<ul><li> Each test (weighing, LCMS) is shown on a separate tab"
			+ "<li> Only the tests with one output value per animal-input are shown"
			+ "</ul>"+HtmlUtils.convert2Html(
					"\tTopId1\tTopId2\tTopId3\n"
					+ "Sample1\t\t\t\n"
					+ "Sample2\t\t\t\n"),
			new ReportParameter[] {SKIP_EMPTY_ANIMAL_PARAMETER, MAX_PER_SHEET_PARAMETER});
	}

	@SuppressWarnings("unused")
	@Override
	protected void populateWorkBook() throws Exception {
		boolean skipEmptyAnimals = (Boolean) getParameter(SKIP_EMPTY_ANIMAL_PARAMETER);
		boolean maxColumnsPerSheet = (Boolean) getParameter(MAX_PER_SHEET_PARAMETER);
		
		//Query Results
		ResultQuery q = new ResultQuery();
		q.setSid(study.getId());
		List<Result> results = DAOResult.queryResults(q, Spirit.getUser());
		Map<Test, List<Result>> test2results = Result.mapTest(results);
		
		
		int startRow = 3;
		int row, col;
		
		loopTest: for (Test test: test2results.keySet()) {
			
			if(DAOTest.FOODWATER_TESTNAME.equals(test.getName())) continue;
			if(DAOTest.LENGTH_TESTNAME.equals(test.getName())) continue;
			if(DAOTest.OBSERVATION_TESTNAME.equals(test.getName())) continue;
			if(DAOTest.WEIGHING_TESTNAME.equals(test.getName())) continue;
			Set<Biosample> hasData = new HashSet<>();
			
			///////////////////////////////////////////////////////////////////////////
			//Precalculate inputs
			Set<String> inputs = new TreeSet<String>();
			SetHashMap<String, String> input2output = new SetHashMap<String, String>();
			ListHashMap<String, ResultValue> key2results = new ListHashMap<String, ResultValue>(); 
			for(Result r: test2results.get(test)) {
				for(ResultValue rv: r.getOutputResultValues()) {
					if(r.getBiosample()==null) continue;
					Biosample top = r.getBiosample().getTopParentInSameStudy();
					String input = PivotItemFactory.RESULT_INPUT.getTitleCleaned(rv) + (rv.getResult().getInheritedPhase()==null?"": " " + rv.getResult().getInheritedPhase().getShortName());
					String output = PivotItemFactory.RESULT_OUTPUT.getTitleCleaned(rv);
					inputs.add(input);
					input2output.add(input, output);
					hasData.add(top);
					
					String key = top.getId()+"_"+input+"_"+output;
					if(key2results.get(key)!=null && key2results.get(key).size()>0) {
						System.err.println("Skip test "+test);
						continue loopTest;					
					}
					key2results.add(key, rv);
				}
			}
			if(inputs.size()<=0) continue;
			
			/////////////////////////////////////////////////////////////////////////////
			//Split the sheet into several if we have more than ## inputs
			int totalColumns = 0; 
			for (Set<String> collection : input2output.values()) totalColumns+=collection.size();
			
			//if the columns don't fit one sheet, split it uniformely (to avoid 14 on 1st page, and 1 on 2nd)
			int nPages = (totalColumns+16-1)/16;
			int nPerPage = maxColumnsPerSheet? totalColumns / nPages: totalColumns;
			
			for(int page=0; page<nPages; page++) {
				
				
				// Create sheet
				Sheet sheet = createSheet(wb, "Test "+test.getName() + (nPages>1?"("+(page+1)+")":""));
				createHeadersWithTitleSubtitle(sheet , study, "Resultss", test.getName());
	
				//////////////////////////////////////////////////////////////////////////////////////////
				//Display animals at the row level
				row = startRow+2;
				for(Group group: study.getGroups()) {
					int nAnimals = 0;
					for (Biosample animal : study.getTopAttachedBiosamples(group)) {
						if(skipEmptyAnimals && !hasData.contains(animal)) continue;						
						set(sheet, row, 0, animal.getInheritedGroup()==null?"": animal.getInheritedGroup().getName(), Style.S_TD_LEFT);
						set(sheet, row, 1, animal.getSampleId(), Style.S_TD_CENTER);
						set(sheet, row, 2, animal.getSampleName(), Style.S_TD_RIGHT);
						row++;
						nAnimals++;
					}
					if(nAnimals==0) continue;
					//Mean
					set(sheet, row, 0, "", Style.S_TD_LEFT);
					set(sheet, row, 1, "", Style.S_TD_LEFT);
					set(sheet, row++, 2, "Mean", Style.S_TD_BOLD_RIGHT);
					//SD
					set(sheet, row, 0, "", Style.S_TD_LEFT);
					set(sheet, row, 1, "", Style.S_TD_LEFT);
					set(sheet, row++, 2, "SD", Style.S_TD_BOLD_RIGHT);
				}
				
				//////////////////////////////////////////////////////////////////////////////////////////
				//Display Input/units at the column level
				col = 3;
				int index=0;
				set(sheet, startRow+1, 0, "Group", Style.S_TH_CENTER);					
				set(sheet, startRow+1, 1, "Id", Style.S_TH_CENTER);					
				set(sheet, startRow+1, 2, "No", Style.S_TH_RIGHT);					
				for(String input : inputs) {
					String name = TestAttribute.extractNameWithoutUnit(input);
					String unit = TestAttribute.extractUnit(input);
					
					for(String output : input2output.get(input)) {
						index++; if(index<=page*nPerPage || index>(page+1)*nPerPage) continue;							
						
						if(name.length()>0) set(sheet, startRow, col, name, Style.S_TH_CENTER);
						set(sheet, startRow+1, col, ((output==null?"":output) + (unit==null?"":" [" + unit + "]")).trim(), Style.S_TH_CENTER);					
						col++;
					}
				}
				
				//Display Output
				row = startRow+2;
				for(Group group: study.getGroups()) {
					ListHashMap<Integer, Integer> col2Lines = new ListHashMap<Integer, Integer>();
					int nAnimals = 0;
					for (Biosample animal : study.getTopAttachedBiosamples(group)) {
						if(skipEmptyAnimals && !hasData.contains(animal)) continue;
						col = 3;
						index = 0;
						for(String input: inputs) {
							for(String output: input2output.get(input)) {
								index++; if(index<=page*nPerPage || index>(page+1)*nPerPage) continue;							
								//For each cell, get the value and display it
								String key = animal.getId()+"_"+input+"_"+output;
								List<ResultValue> rvs = key2results.get(key);
								if(rvs!=null && rvs.size()>0) {
									String val = MiscUtils.flatten(ResultValue.getValues(rvs), "; ");
									set(sheet, row, col, val, Style.S_TD_DOUBLE2);	
									col2Lines.add(col, row);
								} else {
									set(sheet, row, col, "", Style.S_TD_LEFT);										
								}
								col++;
							}						
						}
						row++;
						nAnimals++;
					}
					if(nAnimals==0) continue;
					
					//Mean
					col = 3;
					index = 0;
					for(String input: inputs) {
						for(String output: input2output.get(input)) {
							index++; if(index<=page*nPerPage || index>(page+1)*nPerPage) continue;							
							if(col2Lines.get(col)!=null) {
								String range = convertLinesToCells(col2Lines.get(col), col);						
								setFormula(sheet, row, col, "AVERAGE(" + range + ")", Style.S_TD_DOUBLE2_BLUE);
							}
							col++;
						}
					}
					row++;
					
					//SD
					col = 3;
					index = 0;
					for(String input: inputs) {
						for(String output: input2output.get(input)) {
							index++; if(index<=page*nPerPage || index>(page+1)*nPerPage) continue;							
							if(col2Lines.get(col)!=null) {
								String range = convertLinesToCells(col2Lines.get(col), col);						
								setFormula(sheet, row, col, "STDEV(" + range + ")", Style.S_TD_DOUBLE2);
							}
							col++;
						}
					}
					drawLineUnder(sheet, row, 0, col-1, (short)1);					
					row++;
				}
				POIUtils.autoSizeColumns(sheet);
			}
		} //end-sheet
		if(wb.getNumberOfSheets()==0) throw new Exception("The study does not have appropriate measurements to report.");

	}
}
