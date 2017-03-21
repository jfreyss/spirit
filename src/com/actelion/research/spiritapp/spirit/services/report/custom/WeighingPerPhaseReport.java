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

package com.actelion.research.spiritapp.spirit.services.report.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;

import com.actelion.research.spiritapp.spirit.services.report.AbstractReport;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.ActionTreatment;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.MiscUtils;

public class WeighingPerPhaseReport extends AbstractReport {

	public WeighingPerPhaseReport() {
		super(ReportCategory.TOP, "Bodyweights (one sheet per phase)", MiscUtils.convert2Html(
				"Group\tContainerId\tTopId\tNo\tWeight\tIncrease\tTreatment\tFormulation\tDate\n"
				+ "1. \tCage1\tTopId1\t1\t20\t+1%\tVehicle\n"
				+ "1. \tCage1\tTopId2\t2\t18\t+2%\tVehicle\n"
				+ "Phase1 | Phase2 | Phase 3 | Summary | Increase"));
	}
	
	public static ActionTreatment getTreatment(Biosample h) {
		if(h==null) return null;
		if(h.getLastAction() instanceof ActionTreatment) {
			return ((ActionTreatment)h.getLastAction());
		}
		return null;
	}
	
	@Override
	protected void populateWorkBook() throws Exception {

		List<Biosample> animals = study.getTopAttachedBiosamples();
		Map<Biosample, Map<String, Biosample>> bio2history = new HashMap<>();
		Set<Phase> phases = new TreeSet<>();
		
		//Load Weighings
		Test weightingTest = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
		if(weightingTest==null) throw new Exception("Error test "+DAOTest.WEIGHING_TESTNAME+" not found");
		DAOResult.attachOrCreateStudyResultsToTops(study, animals, null, null);

		
		for(Biosample b: animals) {
			bio2history.put(b, new HashMap<String, Biosample>());
			List<Biosample> history = DAORevision.getHistory(b);
			for (Biosample b2 : history) {
				if(!study.equals(b2.getInheritedStudy())) continue;
				ActionTreatment t = getTreatment(b2);
				if(t!=null) {
					//Add the treatment, if it is the most recent
					if(!bio2history.get(b).containsKey(t.getPhaseName())) {
						bio2history.get(b).put(t.getPhaseName(), b2);
					}
				}
			}
			for(Result r: b.getAuxResults(weightingTest.getName(), null)){
				phases.add(r.getInheritedPhase());
			}
		}
		
		for (Phase phase : phases) {				
			//Check if we need to display a compound
			boolean hasCompound2 = false;
			for (Biosample animal : animals) {
				Biosample h = bio2history.get(animal).get(phase.getShortName());
				ActionTreatment t = getTreatment(h);
				if(t!=null && t.getEff2().length()>0) {
					hasCompound2 = true;
				}
			}			
			
			//Print headers
			Phase after = phase.getNextPhase();
			Sheet sheet = createSheet(wb, "BW "+phase.getShortName());

			createHeadersWithPhase(sheet, study, phase, "BW used for given dose " + (after==null?"at " + phase.getShortName(): "from " + phase.getShortName() + " to " + after.getShortName()));

			//Create Header for Weighing & Doses
			int line = 3;
			int col = 0;
			
			set(sheet, line, col++, "Group", Style.S_TH_CENTER);
			set(sheet, line, col++, "ContainerId", Style.S_TH_CENTER);
			set(sheet, line, col++, "TopId", Style.S_TH_CENTER);
			set(sheet, line, col++, "No", Style.S_TH_CENTER);
			
			set(sheet, line, col++, "Weight [g]", Style.S_TH_CENTER);
			set(sheet, line, col++, "Increase [%]", Style.S_TH_CENTER);

			set(sheet, line, col++, "Treatment", Style.S_TH_CENTER);
			
			set(sheet, line, col++, "Eff1.", Style.S_TH_CENTER);
			if(hasCompound2) {
				set(sheet, line, col++, "Eff2.", Style.S_TH_CENTER);
			}
			
			
			set(sheet, line, col++, "Formulation", Style.S_TH_CENTER);
			set(sheet, line, col++, "Comments", Style.S_TH_CENTER);
			
			set(sheet, line, col++, "Date", Style.S_TH_CENTER);
			int maxCol = col-1;
			line++;

			//Add Biosample Weighing
			Group lastGroup = null;
			ListHashMap<Group, Integer> group2Lines = new ListHashMap<>();
			for (Biosample animal : animals) {
				//Gets Weight + Increase
				Result weighResult = animal.getAuxResult(weightingTest, phase);
				Result prevWeighResult = weighResult==null?null: Result.getPrevious(weighResult, animal.getAuxResults(DAOTest.WEIGHING_TESTNAME, null));
				
				
				Double weight = weighResult==null?null: weighResult.getOutputResultValues().get(0).getDoubleValue();
				Double increase = null;
				if(weight!=null && prevWeighResult!=null && prevWeighResult.getOutputResultValues().get(0).getDoubleValue()!=null && prevWeighResult.getOutputResultValues().get(0).getDoubleValue()>0) {
					Double prev = prevWeighResult.getOutputResultValues().get(0).getDoubleValue();
					increase = 100*(weight - prev)/prev;
				}
				
				//Memorize line to compute averages
				group2Lines.add(animal.getInheritedGroup(), line);
				
				//Display data
				Biosample h = bio2history.get(animal).get(phase.getShortName());
				ActionTreatment t = getTreatment(h);
				
				col = 0;
				set(sheet, line, col++, animal==null? null: animal.getInheritedGroupString(SpiritFrame.getUsername()), Style.S_TD_LEFT);				
				set(sheet, line, col++, animal==null? null: animal.getContainerId(), Style.S_TD_CENTER);				
				set(sheet, line, col++, animal==null? null: animal.getTopParent().getSampleId(), Style.S_TD_CENTER);				
				set(sheet, line, col++, animal==null? null: animal.getSampleName(), Style.S_TD_CENTER);	

				set(sheet, line, col++, weight, Style.S_TD_DOUBLE1_BLUE);						
				set(sheet, line, col++, increase , Style.S_TD_DOUBLE1);									
				
				set(sheet, line, col++, t==null? null: t.getTreatmentName(), Style.S_TD_LEFT);

				set(sheet, line, col++, t==null? null: t.getEff1(), Style.S_TD_DOUBLE1);
				
				if(hasCompound2) {
					set(sheet, line, col++, t==null? null: t.getEff2(), Style.S_TD_DOUBLE1);
				}
				
				set(sheet, line, col++, t==null? null: t.getFormulation(), Style.S_TD_LEFT);				
				set(sheet, line, col++, t==null? null: t.getComments(), Style.S_TD_LEFT);				
				
				set(sheet, line, col++, weighResult==null? (h==null? null: h.getUpdDate()): weighResult.getUpdDate(), Style.S_TD_DATE);
				
				
				//Separator if we change group
				Group group = animal.getInheritedGroup();
				if(lastGroup!=null && !lastGroup.equals(group)) {
					drawLineAbove(sheet, line, 0, maxCol, (short) 1);						
				}
				lastGroup = animal.getInheritedGroup();
								
				line++;
			}
			
			//Add weighing averages
			col = 4;
			line+=2;
			set(sheet, line, col++, "Weight [g]" , (Style.S_TH_CENTER));
			set(sheet, line, col++, "Increase [%]" , (Style.S_TH_CENTER));
			line++;
			for(Group group: study.getGroups()) {
				if(study.getTopAttachedBiosamples(group).size()==0) continue;
				
				List<Integer> lines = group2Lines.get(group);
				col = 0;
				set(sheet, line, col++, group.getBlindedName(SpiritFrame.getUsername()), Style.S_TD_LEFT);
				set(sheet, line, col++, "" , Style.S_TD_LEFT);
				set(sheet, line, col++, "" , Style.S_TD_LEFT);
				set(sheet, line, col++, "" , Style.S_TD_LEFT);
				if(lines!=null && lines.size()>0) {
					setAverage(sheet, line, col, convertLinesToCells(lines, col++), Style.S_TD_DOUBLE1);
					setAverage(sheet, line, col, convertLinesToCells(lines, col++), Style.S_TD_DOUBLE1);
				}
				line++;
			}
			
			wb.setSelectedTab(wb.getSheetIndex(sheet));
			wb.setActiveSheet(wb.getSheetIndex(sheet));
			POIUtils.autoSizeColumns(sheet);
		}		
		
		/////////////////////////////////////////////////////////////////////////////////
		//Add a sheet for the summary weighing (i==0 --> "Weighing", i==1 --> "Increase")
		for (int i = 0; i < 2; i++) {			
			Sheet sheet = createSheet(wb, WorkbookUtil.createSafeSheetName((i==0? "BW Summary": "BW Increase")));
			createHeadersWithTitleSubtitle(sheet, study, "Summary", "Weighing " + (i==0? "Summary [g]": "Increase [%]"));
			int line = 3;	
			
			int col=0;
			set(sheet, line, col++, "Group", Style.S_TH_CENTER);
			set(sheet, line, col++, "Cage", Style.S_TH_CENTER);
			set(sheet, line, col++, "AnimalId", Style.S_TH_CENTER);
			set(sheet, line, col++, "No", Style.S_TH_CENTER);
			for (Phase phase : phases) {
				set(sheet, line, col++, "      " + phase.getShortName() + "      ", Style.S_TH_CENTER);
			}			
			int maxCol = col - 1;
			line++;
			
			Group lastGroup = null;
			ListHashMap<Group, Integer> group2Lines = new ListHashMap<>();
			for (Biosample biosample : animals) {

				//Memorize line to compute averages
				group2Lines.add(biosample.getInheritedGroup(), line);				
				
				//Display data
				col = 0;
				set(sheet, line, col++, biosample.getInheritedGroupString(SpiritFrame.getUsername()), Style.S_TD_LEFT);
				set(sheet, line, col++, biosample.getContainerId(), Style.S_TD_CENTER);				
				set(sheet, line, col++, biosample.getTopParent().getSampleId(), Style.S_TD_CENTER);
				set(sheet, line, col++, biosample.getSampleName(), Style.S_TD_CENTER);
				for (Phase phase : phases) {
					String cell = "\'" + WorkbookUtil.createSafeSheetName("BW " +phase.getShortName()) +"\'" + "!"+convertToCell(line, 4+i);
					String formula = "IF(LEN("+cell+")>0, "+cell+", \"\")";
					try {						
						setFormula(sheet, line, col, formula, Style.S_TD_DOUBLE1);
					} catch (Exception e) {
						e.printStackTrace();
						set(sheet, line, col, formula, Style.S_TD_DOUBLE1);
					} 
					col++;
				}
				
				Group group = biosample.getInheritedGroup();
				if(lastGroup!=null && !lastGroup.equals(group)) {
					drawLineAbove(sheet, line, 0, maxCol, (short) 1);
				}
				lastGroup = biosample.getInheritedGroup();
				

				
				line++;
			
			}
			
			//Group Summary averages

			line++;
			for(Group group: study.getGroups()) {
				
				List<Integer> linesForGroup = group2Lines.get(group);
				col = 0;
				set(sheet, line, col++, group.getName() , Style.S_TD_LEFT);				
				set(sheet, line, col++, "" , Style.S_TD_CENTER);				
				set(sheet, line, col++, "" , Style.S_TD_CENTER);
				set(sheet, line, col++, "" , Style.S_TD_CENTER);
				if(linesForGroup!=null && linesForGroup.size()>0) {
					for (int j = 0; j < phases.size(); j++) {
						List<Integer> linesForGroupPhase = new ArrayList<Integer>(linesForGroup);

						try {
							setAverage(sheet, line, col, convertLinesToCells(linesForGroupPhase, col), Style.S_TD_DOUBLE1);
						} catch (Exception e) {
							e.printStackTrace();
							set(sheet, line, col, "IF(COUNT(" + convertLinesToCells(linesForGroupPhase, col) + "), AVERAGE(" + convertLinesToCells(linesForGroupPhase, col) + "), \"\")", Style.S_TD_DOUBLE1);
						}
						col++;
					}
				}
				line++;
			}
			wb.setSelectedTab(wb.getSheetIndex(sheet));
			wb.setActiveSheet(wb.getSheetIndex(sheet));
			POIUtils.autoSizeColumns(sheet);
		}
		
	}
	
	public static void main(String[] args)  {
		try {
			SpiritFrame.setUser(DAOSpiritUser.loadUser("freyssj"));
			WeighingPerPhaseReport wg = new WeighingPerPhaseReport();
			wg.populateReport(DAOStudy.getStudyByStudyId("S-00677"));
			wg.export(null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}
		
}


