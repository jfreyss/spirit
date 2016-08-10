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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import org.apache.poi.ss.usermodel.Sheet;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.ActionTreatment;
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
import com.actelion.research.util.FormatterUtils;

public class SpecimenWeighingInVivoReport extends AbstractReport {

	private static ReportParameter ONE_PER_DAY_PARAMETER = new ReportParameter("Max one BW/day (take the first BW)", Boolean.TRUE);
	private static ReportParameter ADD_OBSERVATION_PARAMETER = new ReportParameter("Add Observations below the table", Boolean.FALSE);
	private static ReportParameter ADD_RATIO = new ReportParameter("Add Ratio", Boolean.FALSE);
	
	private Map<Group, Group> compareGroup2Groups = new HashMap<Group, Group>();

	public SpecimenWeighingInVivoReport() {
		super(ReportCategory.SPECIMEN, 
				"Bodyweights (All phases in one table)", 
				"The body weights are shown in one table, additional sheets show the absolute increase and the relative increase from d-1", 
				new ReportParameter[] { ONE_PER_DAY_PARAMETER, ADD_RATIO, ADD_OBSERVATION_PARAMETER });
	}

	@Override
	public JPanel getExtraParameterPanel(Study study) {
		return createCompareGroupsPanel(study, compareGroup2Groups);
	}


	@Override
	protected void populateWorkBook() throws Exception {
		boolean onePerDay = getParameter(ONE_PER_DAY_PARAMETER).equals(Boolean.TRUE);
		boolean addObservations = getParameter(ADD_OBSERVATION_PARAMETER).equals(Boolean.TRUE);
		boolean addRatio = getParameter(ADD_RATIO).equals(Boolean.TRUE);

		
		Test weighingTest = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
		if(weighingTest==null) throw new Exception("Error test "+DAOTest.WEIGHING_TESTNAME+" not found");

		Set<Biosample> animals = study.getAttachedBiosamples();
		List<Phase> phases = new ArrayList<Phase>();
		Set<Integer> days = new HashSet<Integer>();
		int phaseIndexRef = 0;
		Phase refPhase = study.getReferencePhase(-1);
		for (Phase phase : study.getPhases()) {
			if (onePerDay && days.contains(phase.getDays())) continue;
			
			for (Biosample b : animals) {
				if (b.getAction(ActionTreatment.class, phase) != null) {
					if (phase.equals(refPhase)) {
						phaseIndexRef = phases.size();
					}

					phases.add(phase);
					days.add(phase.getDays());
					break;
				}
			}
		}

		// Load Weighings
		DAOResult.attachOrCreateStudyResultsToSpecimen(study, animals, null, null);

		for (int i = 0; i < 3; i++) {

			String sheetName = i == 0 ? "BW" : i == 1 ? "BW inc.g" : "BW %";
			String titleName = i == 0 ? "Bodyweight" : i == 1 ? "Bodyweight delta change g" : "Bodyweight delta change %";
			String unit = i == 0 ? "g" : i == 1 ? "\u0394g" : "\u0394%";

			// Create sheet
			Sheet sheet = createSheet(wb, sheetName);
			createHeadersWithTitle(sheet, study, titleName);

			// Create a new table for each group
			int groupRow = 3;
			Map<Group, Map<Phase, String>> groupPhase2Range = new HashMap<Group, Map<Phase, String>>();
			Map<Group, Map<Phase, String>> groupPhase2CountCell = new HashMap<Group, Map<Phase, String>>();
			for (Group group : study.getGroups()) {
				if (study.getTopAttachedBiosamples(group).size() == 0) continue;
				
				Map<Phase, String> phase2RangeRef = groupPhase2Range.get(compareGroup2Groups.get(group));
				Map<Phase, String> phase2CountCellRef = groupPhase2CountCell.get(compareGroup2Groups.get(group));

				Map<Phase, String> phase2Range = new HashMap<Phase, String>();
				groupPhase2Range.put(group, phase2Range);

				Map<Phase, String> phase2CountCell = new HashMap<Phase, String>();
				groupPhase2CountCell.put(group, phase2CountCell);

				String name = group.getBlindedName(Spirit.getUsername()) + " " + group.getTreatmentDescription(-1, false);
				set(sheet, groupRow, 0, name, Style.S_TITLE14BLUE);
				groupRow++;

				if (group.getNSubgroups() > 1) {
					set(sheet, groupRow + 2, 0, "Subgroup", Style.S_TH_CENTER);
				}
				set(sheet, groupRow + 2, 1, "AnimalId", Style.S_TH_CENTER);
				set(sheet, groupRow + 2, 2, "No", Style.S_TH_CENTER);
				set(sheet, groupRow + 2, 3, "Necro", Style.S_TH_CENTER);

				int animalRow = groupRow + 3;
				for (Biosample animal : study.getTopAttachedBiosamples(group)) {
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
				for (Phase phase : phases) {
					if (phase.getAbsoluteDate() != null) {
						set(sheet, groupRow, 3 + phaseIndex, FormatterUtils.formatDate(phase.getAbsoluteDate()), Style.S_TH_CENTER);
					}
					set(sheet, groupRow + 1, 3 + phaseIndex, phase.getShortName(), Style.S_TH_CENTER);
					set(sheet, groupRow + 2, 3 + phaseIndex, "BW (" + unit + ")", Style.S_TH_CENTER);

					// Add the weights or delta
					animalRow = groupRow + 3;
					List<Integer> animalRows = new ArrayList<Integer>();
					for (Biosample animal : study.getTopAttachedBiosamples(group)) {
						if (i == 0) {
							Result weighResult = animal.getAuxResult(weighingTest, phase);
							Double weight = weighResult == null ? null : weighResult.getFirstAsDouble();
							set(sheet, animalRow, 3 + phaseIndex, weight, Style.S_TD_DOUBLE1);
						} else if (i == 1) {
							String rangeRef = convertToCell(animalRow, 3 + phaseIndexRef);
							String range = convertToCell(animalRow, 3 + phaseIndex);
							setFormula(sheet, animalRow, 3 + phaseIndex, "IF(ISNUMBER(BW!" + range + "), BW!" + range + "-BW!$" + rangeRef + ",\"\")", Style.S_TD_DOUBLE1_BLUE);
						} else if (i == 2) {
							String rangeRef = convertToCell(animalRow, 3 + phaseIndexRef);
							String range = convertToCell(animalRow, 3 + phaseIndex);
							setFormula(sheet, animalRow, 3 + phaseIndex, "IF(ISNUMBER(BW!" + range + "), 100/BW!$" + rangeRef + "*BW!" + range + "-100,\"\")", Style.S_TD_DOUBLE1_BLUE);
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
						String isNumber1 = "ISNUMBER(" + countCell + ")";

						setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + isNumber1 + ", AVERAGE(" + range + "), \"\")", Style.S_TD_DOUBLE1_BLUE);

						setFormula(sheet, stDevRow = animalRow++, 3 + phaseIndex, "IF(" + isNumber1 + ", STDEV(" + range + "), \"\")", Style.S_TD_DOUBLE1_BLUE);
						setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + isNumber1 + ", " + convertToCell(stDevRow, 3 + phaseIndex) + " / SQRT(" + convertToCell(countRow, 3 + phaseIndex) + "), \"\")", Style.S_TD_DOUBLE1_BLUE);
						if (phase2RangeRef != null) {
							assert phase2CountCellRef!=null;
							assert phase2CountCellRef.get(phase)!=null;
							String isNumber2 = "ISNUMBER(" + countCell + " + " + phase2CountCellRef.get(phase) + ")";
							if(addRatio) {
								setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + isNumber2 + ", AVERAGE("+range+")/AVERAGE(" + phase2RangeRef.get(phase) + "), \"\")", Style.S_TD_DOUBLE100_RED);
							}
							int ttRow;
							setFormula(sheet, ttRow = animalRow++, 3 + phaseIndex, "IF(" + isNumber2 + ", TTEST(" + phase2RangeRef.get(phase) + "," + range + ",2,3), \"\")", Style.S_TD_DOUBLE3_RED);
							String ttCell = convertToCell(ttRow, 3 + phaseIndex);
							setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + ttCell + "=0,\" \",IF(" + ttCell + "<0.001,\"***\",IF(" + ttCell + "<0.01,\"**\",IF(" + ttCell + "<0.05,\"*\",\" \"))))", Style.S_TD_RED);
						}
					}
					phase2Range.put(phase, range);
					phase2CountCell.put(phase, countCell);

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
			wg.populateReport(DAOStudy.getStudyByStudyId("S-00532"));
			wg.export(null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}

}
