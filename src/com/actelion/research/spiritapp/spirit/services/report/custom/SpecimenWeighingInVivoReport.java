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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;

import org.apache.poi.ss.usermodel.Sheet;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.services.report.AbstractReport;
import com.actelion.research.spiritapp.spirit.services.report.ReportParameter;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.FormatterUtils;

public class SpecimenWeighingInVivoReport extends AbstractReport {

	private static ReportParameter ONE_PER_DAY_PARAMETER = new ReportParameter("Max one BW/day (take the first BW and skip the others)", Boolean.TRUE);
	private static ReportParameter USE_SINCEFIRST = new ReportParameter("Use 'since 1st treatment' instead of phases", Boolean.FALSE);
	private static ReportParameter ADD_OBSERVATION_PARAMETER = new ReportParameter("Add Observations below the table", Boolean.FALSE);
	private static ReportParameter ADD_RATIO = new ReportParameter("Add Ratio below the table", Boolean.FALSE);
	
	private Map<Group, Group> compareGroup2Groups = new HashMap<>();

	public SpecimenWeighingInVivoReport() {
		super(ReportCategory.TOP, 
				"Bodyweights (All phases in one table)", 
				"<ul><li>First sheet for the bodyweight<li>Second sheet for the absolute increase and third for the relative increase (since d0)</ul>" + MiscUtils.convert2Html(
						"TopId\tNo\tPhase1\tPhase2\tPhase3\n"
						+ "TopId1\t\t102\t105\n"
						+ "TopId2\t\t110\t108\n"), 
				new ReportParameter[] { ONE_PER_DAY_PARAMETER, USE_SINCEFIRST, ADD_RATIO, ADD_OBSERVATION_PARAMETER });
	}

	@Override
	public JPanel getExtraParameterPanel(Study study) {
		return createCompareGroupsPanel(study, compareGroup2Groups);
	}


	@Override
	protected void populateWorkBook() throws Exception {
		boolean sinceFirst = getParameter(USE_SINCEFIRST).equals(Boolean.TRUE);
		boolean onePerDay = getParameter(ONE_PER_DAY_PARAMETER).equals(Boolean.TRUE);
		boolean addObservations = getParameter(ADD_OBSERVATION_PARAMETER).equals(Boolean.TRUE);
		boolean addRatio = getParameter(ADD_RATIO).equals(Boolean.TRUE);

		
		Test weighingTest = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
		if(weighingTest==null) throw new Exception("Error test "+DAOTest.WEIGHING_TESTNAME+" not found");
		
		
		// Load Weighings
		DAOResult.attachOrCreateStudyResultsToTops(study);

		List<Phase> phases = new ArrayList<>();
		Set<Integer> days = new HashSet<>();
		Set<Integer> since1stSet = new TreeSet<>();
		int phaseIndexRef = 0;
		Phase refPhase = study.getReferencePhase(0);
		Map<Pair<Biosample, String>, Phase> animalSince1st2Phase = new HashMap<>();
		for (Phase phase : study.getPhases()) {
			if (onePerDay && days.contains(phase.getDays())) continue;
			
			for (Biosample animal : study.getAttachedBiosamples()) {
				Result weighResult = animal.getAuxResult(weighingTest, phase);
				if (weighResult != null) {
					if (phase.equals(refPhase)) {
						phaseIndexRef = phases.size();
					}
					Phase firstPhase = study.getPhaseFirstTreatment(animal.getInheritedGroup(), animal.getInheritedSubGroup());
					int since1st = phase.getDays() - (firstPhase==null? 0: firstPhase.getDays());
					since1stSet.add(since1st);
					animalSince1st2Phase.put(new Pair<Biosample, String>(animal, (since1st>0?"+":"") + since1st), phase);
					phases.add(phase);
					days.add(phase.getDays());
				}
			}
		}

		//Calculate X Axis
		Map<String, Integer> xAxis = new LinkedHashMap<>();
		
		if(sinceFirst) {
			List<Integer> since1stList = new ArrayList<>(since1stSet);
			for (int i = 0; i < since1stList.size(); i++) {
				xAxis.put((since1stList.get(i)>0?"+":"") + since1stList.get(i), i);
			}			
		} else {
			for (int i = 0; i < phases.size(); i++) {
				xAxis.put(phases.get(i).getShortName(), i);
			}
		}
		for (int i = 0; i < 3; i++) {

			String sheetName = i == 0 ? "BW" : i == 1 ? "BW inc.g" : "BW %";
			String titleName = i == 0 ? "Bodyweight" : i == 1 ? "Bodyweight delta change g" : "Bodyweight delta change %";
			String unit = i == 0 ? "g" : i == 1 ? "\u0394g" : "\u0394%";

			// Create sheet
			Sheet sheet = createSheet(wb, sheetName);
			createHeadersWithTitle(sheet, study, titleName);

			// Create a new table for each group
			int groupRow = 3;
			Map<Group, Map<String, String>> groupPhase2Range = new HashMap<>();
			Map<Group, Map<String, String>> groupPhase2CountCell = new HashMap<>();
			for (Group group : study.getGroups()) {
				List<Biosample> animals = study.getTopAttachedBiosamples(group); 
				if (animals.size() == 0) continue;
				
				Map<String, String> phase2RangeRef = groupPhase2Range.get(compareGroup2Groups.get(group));
				Map<String, String> phase2CountCellRef = groupPhase2CountCell.get(compareGroup2Groups.get(group));

				Map<String, String> phase2Range = new HashMap<>();
				groupPhase2Range.put(group, phase2Range);

				Map<String, String> phase2CountCell = new HashMap<>();
				groupPhase2CountCell.put(group, phase2CountCell);

				String name = group.getBlindedName(Spirit.getUsername()) + " " + group.getTreatmentDescription(-1, false);
				set(sheet, groupRow, 0, name, Style.S_TITLE14BLUE);
				groupRow++;

				if (group.getNSubgroups() > 1) {
					set(sheet, groupRow + 2, 0, "Subgroup", Style.S_TH_CENTER);
				}
				set(sheet, groupRow + 2, 1, "TopId", Style.S_TH_CENTER);
				set(sheet, groupRow + 2, 2, "No", Style.S_TH_CENTER);
				set(sheet, groupRow + 2, 3, "Necro", Style.S_TH_CENTER);

				int animalRow = groupRow + 3;
				for (Biosample animal : animals) {
					if (group.getNSubgroups() > 1) {
						set(sheet, animalRow, 0, "" + (1 + animal.getInheritedSubGroup()), Style.S_TD_DOUBLE0);						
					}
					set(sheet, animalRow, 1, animal.getSampleId(), Style.S_TD_CENTER);
					set(sheet, animalRow, 2, animal.getSampleName(), Style.S_TD_BOLD_CENTER);
					animalRow++;
				}
				set(sheet, animalRow++, 2, "Number", Style.S_TD_BOLD_CENTER);
				set(sheet, animalRow++, 2, "Mean", Style.S_TD_BOLD_CENTER);
				set(sheet, animalRow++, 2, "STDEV", Style.S_TD_BOLD_CENTER);
				set(sheet, animalRow++, 2, "SEM", Style.S_TD_BOLD_CENTER);
				if (phase2RangeRef != null) {
					if(addRatio) set(sheet, animalRow++, 2, "Ratio", Style.S_TD_BOLD_CENTER);
					set(sheet, animalRow++, 2, "p", Style.S_TD_BOLD_CENTER);
					set(sheet, animalRow++, 2, "sign(*)", Style.S_TD_BOLD_CENTER);
				}

				// Loop through phases
				int phaseIndex = 0;
				for(String xLabel: xAxis.keySet()) {
					if(sinceFirst) {
						set(sheet, groupRow, 3 + phaseIndex, "after 1st tr.", Style.S_TH_CENTER);
					} else {
						Phase phase = phases.get(xAxis.get(xLabel));
						if (phase!=null && phase.getAbsoluteDate() != null) {
							set(sheet, groupRow, 3 + phaseIndex, FormatterUtils.formatDate(phase.getAbsoluteDate()), Style.S_TH_CENTER);
						}
					}
					set(sheet, groupRow + 1, 3 + phaseIndex, xLabel, Style.S_TH_CENTER);
					set(sheet, groupRow + 2, 3 + phaseIndex, "BW (" + unit + ")", Style.S_TH_CENTER);

					// Add the weights or delta
					animalRow = groupRow + 3;
					List<Integer> animalRows = new ArrayList<>();
					for (Biosample animal : animals) {
						Phase phase; 
						if(sinceFirst) {
							phase = animalSince1st2Phase.get(new Pair<Biosample, String>(animal, xLabel));
						} else {
							phase = phases.get(xAxis.get(xLabel));
						}
						if (i == 0) {
							Result weighResult = phase==null? null: animal.getAuxResult(weighingTest, phase);
							Double weight = weighResult == null ? null : weighResult.getFirstAsDouble();
							set(sheet, animalRow, 3 + phaseIndex, weight, Style.S_TD_DOUBLE1);
						} else if (i == 1) {
							String rangeRef = convertToCell(animalRow, 3 + phaseIndexRef);
							String range = convertToCell(animalRow, 3 + phaseIndex);
							setFormula(sheet, animalRow, 3 + phaseIndex, "IF(AND(ISNUMBER(BW!" + range + "), ISNUMBER(BW!" + rangeRef + ")), BW!" + range + "-BW!$" + rangeRef + ",\"\")", Style.S_TD_DOUBLE1_BLUE);
						} else if (i == 2) {
							String rangeRef = convertToCell(animalRow, 3 + phaseIndexRef);
							String range = convertToCell(animalRow, 3 + phaseIndex);
							setFormula(sheet, animalRow, 3 + phaseIndex, "IF(AND(ISNUMBER(BW!" + range + "), ISNUMBER(BW!" + rangeRef + ")),  100/BW!$" + rangeRef + "*BW!" + range + "-100,\"\")", Style.S_TD_DOUBLE1_BLUE);
						}
						animalRows.add(animalRow);
						animalRow++;
					}

					// Add the stats
					String range = convertLinesToCells(animalRows, 3 + phaseIndex);
					int countRow;
					int stDevRow;
					String countCell = null;
					if (phaseIndex == 0 && i > 0) {
						animalRow++;
						animalRow++;
						animalRow++;
						if (phase2RangeRef != null) {
							animalRow++;
						}						
					} else {
						setFormula(sheet, countRow = animalRow++, 3 + phaseIndex, "IF(COUNT(" + range + ")>0, COUNT(" + range + "), \"\")", Style.S_TD_BLUE);
						countCell = convertToCell(countRow, 3 + phaseIndex);
//						String isNumber1 = "ISNUMBER(" + countCell + ")";

						setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + countCell + ">0, AVERAGE(" + range + "), \"\")", Style.S_TD_DOUBLE1_BLUE);
						setFormula(sheet, stDevRow = animalRow++, 3 + phaseIndex, "IF(" + countCell + ">1, STDEV(" + range + "), \"\")", Style.S_TD_DOUBLE1_BLUE);
						setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + countCell + ">1, " + convertToCell(stDevRow, 3 + phaseIndex) + " / SQRT(" + convertToCell(countRow, 3 + phaseIndex) + "), \"\")", Style.S_TD_DOUBLE1_BLUE);
						if (phase2RangeRef != null) {
							assert phase2CountCellRef!=null;
							assert phase2CountCellRef.get(xLabel)!=null;
//							String isNumber2 = "ISNUMBER(" + countCell + " + " + phase2CountCellRef.get(phase) + ")";
							if(addRatio) {
								setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + countCell + ">0, AVERAGE("+range+")/AVERAGE(" + phase2RangeRef.get(xLabel) + "), \"\")", Style.S_TD_DOUBLE100_RED);
							}
							int ttRow;
							setFormula(sheet, ttRow = animalRow++, 3 + phaseIndex, "IF(" + countCell + ">1, TTEST(" + phase2RangeRef.get(xLabel) + "," + range + ",2,3), \"\")", Style.S_TD_DOUBLE3_RED);
							String ttCell = convertToCell(ttRow, 3 + phaseIndex);
							setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + ttCell + "=0,\" \",IF(" + ttCell + "<0.001,\"***\",IF(" + ttCell + "<0.01,\"**\",IF(" + ttCell + "<0.05,\"*\",\" \"))))", Style.S_TD_RED);
						}
					}
					phase2Range.put(xLabel, range);
					phase2CountCell.put(xLabel, countCell);

					phaseIndex++;
				} //end-phase

				//display observations?
				if(addObservations) {
					animalRow++;
					List<Result> observations = new ArrayList<Result>();
					for (Biosample animal : study.getTopAttachedBiosamples(group)) {
						for(Result r: animal.getAuxResults(DAOTest.OBSERVATION_TESTNAME, null)) {
							if(r.getFirstValue()!=null && r.getFirstValue().toString().length()>0) {
								observations.add(r);
							}
						}
					}
					for (Result result : observations) {
						if(i==0) {
							set(sheet, animalRow, 2, "Obs.", Style.S_TD_BLUE);						
							set(sheet, animalRow, 3, result.getBiosample().getSampleId(), Style.S_TD_BOLD_LEFT);
							set(sheet, animalRow, 4, result.getInheritedPhase()==null?"": result.getInheritedPhase().getShortName(), Style.S_TD_BOLD_LEFT);
							set(sheet, animalRow, 5, result.getFirstValue(), Style.S_TD_LEFT);
						}
						animalRow++;
					}
				}
				groupRow = animalRow + 2;
				
			} //end-group

			wb.setSelectedTab(0);
			wb.setActiveSheet(0);
			POIUtils.autoSizeColumns(sheet);
		} //end-sheet

	}

	public static void main(String[] args)  {
		try {
			Spirit.setUser(DAOSpiritUser.loadUser("freyssj"));
			SpecimenWeighingInVivoReport wg = new SpecimenWeighingInVivoReport();
			wg.populateReport(DAOStudy.getStudyByStudyId("S-00652"));
			wg.export(null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}

}
