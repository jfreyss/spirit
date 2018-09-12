/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.poi.ss.usermodel.Sheet;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.POIUtils;
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
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;

public class WeighingAllReport extends AbstractReport {

	private static ReportParameter ONE_PER_DAY_PARAMETER = new ReportParameter("Max one BW/day (take the first BW and skip the others)", Boolean.FALSE);
	private static ReportParameter ADD_OBSERVATION_PARAMETER = new ReportParameter("Add Observations below the table", Boolean.TRUE);
	private static ReportParameter ADD_RATIO = new ReportParameter("Add Ratio below the table", Boolean.FALSE);

	private Map<Group, Group> compareGroup2Groups = new HashMap<>();
	private JTextComboBox referenceComboBox = new JTextComboBox();

	public WeighingAllReport() {
		super(ReportCategory.PARTICIPANTS,
				"Bodyweights (all phases)",
				MiscUtils.convert2Html(
						"Participant\tNo\tPhase1\tPhase2\tPhase3\n"
								+ "Id1\t\t102\t105\t108\n"
								+ "Id2\t\t110\t108\t111\n"
								+ "Weight | Abs Inc. | % Inc."),
				new ReportParameter[] { ONE_PER_DAY_PARAMETER, ADD_RATIO, ADD_OBSERVATION_PARAMETER });
	}

	@Override
	public JPanel getExtraParameterPanel(Study study) {

		List<String> choices = new ArrayList<>();
		choices.add("since 1st treatment");
		choices.add("since 1st weighing");
		for (Phase p : study.getPhases()) {
			choices.add(p.getShortName());
		}
		referenceComboBox.setChoices(choices);

		referenceComboBox.setText(study.getPhaseClosestFromDayZero()==null? "": study.getPhaseClosestFromDayZero().getShortName());
		referenceComboBox.setText("since 1st weighing");

		return UIUtils.createVerticalBox(
				UIUtils.createTitleBox("Reference for delta and % increase", UIUtils.createHorizontalBox(new JLabel("Phase: "), referenceComboBox, Box.createHorizontalGlue())),
				UIUtils.createTitleBox("Compare groups with", createCompareGroupsPanel(study, compareGroup2Groups)));
	}


	@Override
	protected void populateWorkBook() throws Exception {
		boolean onePerDay = getParameter(ONE_PER_DAY_PARAMETER).equals(Boolean.TRUE);
		boolean addObservations = getParameter(ADD_OBSERVATION_PARAMETER).equals(Boolean.TRUE);
		boolean addRatio = getParameter(ADD_RATIO).equals(Boolean.TRUE);


		Test weighingTest = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
		if(weighingTest==null) throw new Exception("Error test "+DAOTest.WEIGHING_TESTNAME+" not found");


		// Load Weighings
		DAOResult.attachOrCreateStudyResultsToTops(study);

		List<Phase> phases = new ArrayList<>();
		Set<Integer> days = new HashSet<>();
		Map<Phase, Integer> phase2Index = new HashMap<>();
		Phase refPhase = study.getPhase(referenceComboBox.getText());
		Map<Biosample, Phase> animal2RefPhase = new HashMap<>();
		for (Phase phase : study.getPhases()) {
			if (onePerDay && days.contains(phase.getDays())) continue;
			for (Biosample animal : study.getParticipants()) {
				if (animal.getAuxResult(weighingTest, phase) != null) {
					phases.add(phase);
					days.add(phase.getDays());
					break;
				}
			}
		}
		if(phases.size()==0) throw new Exception("No phases");

		if(refPhase==null || referenceComboBox.getText().startsWith("since 1st weighing")) {
			for (Biosample animal : study.getParticipants()) {
				Phase p = phases.iterator().next();
				for(Phase phase: phases) {
					if(animal.getAuxResult(weighingTest, phase)!=null) {
						p = phase;
						break;
					}
				}
				animal2RefPhase.put(animal, p);
			}
		} else if(referenceComboBox.getText().startsWith("since 1st treatment")) {
			for (Biosample animal : study.getParticipants()) {
				Phase firstPhase = study.getPhaseFirstTreatment(animal.getInheritedGroup(), animal.getInheritedSubGroup());
				animal2RefPhase.put(animal, firstPhase);
			}
		} else {
			assert refPhase!=null;
			for (Biosample animal : study.getParticipants()) {
				animal2RefPhase.put(animal, refPhase);
			}
		}
		for (int i = 0; i < phases.size(); i++) {
			phase2Index.put(phases.get(i), i);
		}

		for (int i = 0; i < 3; i++) {

			String sheetName = i == 0 ? "BW" : i == 1 ? "BW inc." : "BW %";
			String titleName = i == 0 ? "Bodyweight" : i == 1 ? "Bodyweight delta change g" : "Bodyweight delta change %";
			String unit = i == 0 ? "g" : i == 1 ? "d" : "%";

			// Create sheet
			Sheet sheet = createSheet(wb, sheetName);
			createHeadersWithTitle(sheet, study, titleName);

			// Create a new table for each group
			int groupRow = 3;
			Map<Group, Map<Phase, String>> groupPhase2Range = new HashMap<>();
			Map<Group, Map<Phase, String>> groupPhase2CountCell = new HashMap<>();
			for (Group group : study.getGroups()) {
				List<Biosample> animals = study.getParticipants(group);
				if (animals.size() == 0) continue;

				Map<Phase, String> phase2RangeRef = groupPhase2Range.get(compareGroup2Groups.get(group));
				Map<Phase, String> phase2Range = new HashMap<>();
				groupPhase2Range.put(group, phase2Range);

				Map<Phase, String> phase2CountCell = new HashMap<>();
				groupPhase2CountCell.put(group, phase2CountCell);

				String name = group.getBlindedName(SpiritFrame.getUsername()) + " " + group.getTreatmentDescription(-1, false);
				set(sheet, groupRow, 0, name, Style.S_TITLE14BLUE);
				groupRow++;

				if (group.getNSubgroups() > 1) {
					set(sheet, groupRow + 2, 0, "Subgroup", Style.S_TH_CENTER);
				}
				set(sheet, groupRow + 2, 1, "Participant", Style.S_TH_CENTER);
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
				if (refPhase!=null && phase2RangeRef != null) {
					if(addRatio) set(sheet, animalRow++, 2, "Ratio", Style.S_TD_BOLD_CENTER);
					set(sheet, animalRow++, 2, "p", Style.S_TD_BOLD_CENTER);
					set(sheet, animalRow++, 2, "sign(*)", Style.S_TD_BOLD_CENTER);
				}

				// Loop through phases
				int phaseIndex = 0;
				int nextRow = 0;
				for(Phase phase: phases) {
					//					if(sinceFirstTreatment) {
					//						set(sheet, groupRow, 3 + phaseIndex, "after 1st tr.", Style.S_TH_CENTER);
					//					} else {
					//						//						Phase phase = phases.get(xAxis.get(xLabel));
					if (phase!=null && phase.getAbsoluteDate() != null) {
						set(sheet, groupRow, 3 + phaseIndex, FormatterUtils.formatDate(phase.getAbsoluteDate()), Style.S_TH_CENTER);
					}
					//					}
					set(sheet, groupRow + 1, 3 + phaseIndex, phase.getShortName(), Style.S_TH_CENTER);
					set(sheet, groupRow + 2, 3 + phaseIndex, "BW (" + unit + ")", Style.S_TH_CENTER);

					// Add the weights or delta
					animalRow = groupRow + 3;
					List<Integer> animalRows = new ArrayList<>();
					for (Biosample animal : animals) {
						//						Phase phase;
						//						if(sinceFirstTreatment) {
						//							phase = animalSince1st2Phase.get(new Pair<Biosample, String>(animal, xLabel));
						//						} else {
						//							phase = phases.get(xAxis.get(xLabel));
						//						}
						if (i == 0) {
							Result weighResult = phase==null? null: animal.getAuxResult(weighingTest, phase);
							Double weight = weighResult == null ? null : weighResult.getFirstAsDouble();
							set(sheet, animalRow, 3 + phaseIndex, weight, Style.S_TD_DOUBLE1);
						} else if (i == 1) { //Inc. abs
							String rangeRef = convertToCell(animalRow, 3 + phase2Index.get(animal2RefPhase.get(animal)));
							String range = convertToCell(animalRow, 3 + phaseIndex);
							setFormula(sheet, animalRow, 3 + phaseIndex, "IF(AND(ISNUMBER(BW!" + range + "), ISNUMBER(BW!" + rangeRef + ")), BW!" + range + "-BW!$" + rangeRef + ",\"\")", Style.S_TD_DOUBLE1_BLUE);
						} else if (i == 2) { //Inc. %
							String rangeRef = convertToCell(animalRow, 3 + phase2Index.get(animal2RefPhase.get(animal)));
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
						set(sheet, animalRow++, 3, "", Style.S_TD_BLUE);
						set(sheet, animalRow++, 3, "", Style.S_TD_BLUE);
						set(sheet, animalRow++, 3, "", Style.S_TD_BLUE);
						set(sheet, animalRow++, 3, "", Style.S_TD_BLUE);
						if (refPhase!=null && phase2RangeRef != null) {
							if(addRatio)  set(sheet, animalRow++, 3, "", Style.S_TD_BLUE);
							set(sheet, animalRow++, 3, "", Style.S_TD_BLUE);
							set(sheet, animalRow++, 3, "", Style.S_TD_BLUE);
						}
					} else {
						setFormula(sheet, countRow = animalRow++, 3 + phaseIndex, "IF(COUNT(" + range + ")>0, COUNT(" + range + "), \"\")", Style.S_TD_BLUE);
						countCell = convertToCell(countRow, 3 + phaseIndex);

						setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(COUNT(" + range + ")>0, AVERAGE(" + range + "), \"\")", Style.S_TD_DOUBLE1_BLUE);
						setFormula(sheet, stDevRow = animalRow++, 3 + phaseIndex, "IF(COUNT(" + range + ")>1, STDEV(" + range + "), \"\")", Style.S_TD_DOUBLE1_BLUE);
						setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(COUNT(" + range + ")>1, " + convertToCell(stDevRow, 3 + phaseIndex) + " / SQRT(" + convertToCell(countRow, 3 + phaseIndex) + "), \"\")", Style.S_TD_DOUBLE1_BLUE);
						if (refPhase!=null && phase2RangeRef != null) {
							if(addRatio) {
								setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + countCell + ">0, AVERAGE("+range+")/AVERAGE(" + phase2RangeRef.get(phase) + "), \"\")", Style.S_TD_DOUBLE100_RED);
							}
							int ttRow;
							setFormula(sheet, ttRow = animalRow++, 3 + phaseIndex, "IF(" + countCell + ">1, TTEST(" + phase2RangeRef.get(phase) + "," + range + ",2,3), \"\")", Style.S_TD_DOUBLE3_RED);
							String ttCell = convertToCell(ttRow, 3 + phaseIndex);
							setFormula(sheet, animalRow++, 3 + phaseIndex, "IF(" + ttCell + "=0,\" \",IF(" + ttCell + "<0.001,\"***\",IF(" + ttCell + "<0.01,\"**\",IF(" + ttCell + "<0.05,\"*\",\" \"))))", Style.S_TD_RED);
						}
					}
					phase2Range.put(phase, range);
					phase2CountCell.put(phase, countCell);

					phaseIndex++;
					nextRow = animalRow+1;
				} //end-phase

				//Reference?
				if(i>0) {
					set(sheet, groupRow + 2, 3 + phaseIndex, "Reference", Style.S_TH_CENTER);
					animalRow = groupRow + 3;
					for (Biosample animal : animals) {
						Phase ref = animal2RefPhase.get(animal);
						set(sheet, animalRow++, 3 + phaseIndex, ref.getShortName(), Style.S_TD_CENTER);
					}
				}

				//display observations?
				if(addObservations) {
					animalRow++;
					List<Result> observations = new ArrayList<>();
					for (Biosample animal : study.getParticipants(group)) {
						for(Result r: animal.getAuxResults(DAOTest.OBSERVATION_TESTNAME, null)) {
							if(r.getFirstValue()!=null && r.getFirstValue().toString().length()>0) {
								observations.add(r);
							}
						}
					}
					for (Result result : observations) {
						if(i==0) {
							set(sheet, nextRow, 2, "Obs.", Style.S_TD_BLUE);
							set(sheet, nextRow, 3, result.getBiosample().getSampleId(), Style.S_TD_BOLD_LEFT);
							set(sheet, nextRow, 4, result.getInheritedPhase()==null?"": result.getInheritedPhase().getShortName(), Style.S_TD_BOLD_LEFT);
							set(sheet, nextRow, 5, result.getFirstValue(), Style.S_TD_LEFT);
						}
						animalRow++;
					}
				}
				groupRow = nextRow + 2;

			} //end-group

			wb.setSelectedTab(0);
			wb.setActiveSheet(0);
			POIUtils.autoSizeColumns(sheet);
		} //end-sheet

	}

	public static void main(String[] args)  {
		try {
			Study study = DAOStudy.getStudyByStudyId("S-00928");
			System.out.println("WeighingAllReport.main() "+study);
			assert study!=null;
			//			MixedReport wg = new MixedReport(ReportFactory.getInstance().getReports());
			//			MixedReport wg = new MixedReport(MiscUtils.listOf(new FoodWaterReport()));
			//			MixedReport wg = new MixedReport(MiscUtils.listOf(new SampleMeasurementPerGroupReport()));
			MixedReport wg = new MixedReport(MiscUtils.listOf(new WeighingAllReport()));
			SpiritFrame.setUser(DAOSpiritUser.loadUser("freyssj"));

			wg.populateReport(study);
			//			wg.exportPDF(null);
			wg.export(null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}


}
