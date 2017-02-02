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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.StatUtils;

public class SampleMeasurementReport extends AbstractReport {
	
	
	private static ReportParameter REQUIRED_ONLY_PARAMETER = new ReportParameter("Show Non-Required Measurements", Boolean.FALSE);
	private static ReportParameter SHOW_OBSERVATIONS_PARAMETER = new ReportParameter("Add Observations", Boolean.TRUE);

	private Map<Group, Group> compareGroup2Groups = new HashMap<>();
	

	
	public SampleMeasurementReport() {		
		super(ReportCategory.SAMPLES, 
				"Weighing (one sheet for all groups)", MiscUtils.convert2Html( 
				"Group1\n"
				+ "\t\t\tBW\tOrgan1\tOrgan2\n"
				+ "ANL1\t1\t33\t1.3\t0.6\n"
				+ "ANL2\t2\t31\t1.2\t0.5\n"
				+ "\n"
				+ "Group2\n"
				+ "\t\t\tBW\tOrgan1\tOrgan2\n"
				+ "ANL3\t3\t30\t1.2\t0.4\n"
				+ "ANL4\t4\t32\t1.1\t0.5\n"), 
				new ReportParameter[]{REQUIRED_ONLY_PARAMETER, SHOW_OBSERVATIONS_PARAMETER});
	}

	@Override
	public JPanel getExtraParameterPanel(Study study) {
		return createCompareGroupsPanel(study, compareGroup2Groups);
	}


	@Override
	protected void populateWorkBook() throws Exception {		
		//Load the weighing, 
		boolean requiredOnly = getParameter(REQUIRED_ONLY_PARAMETER)==Boolean.TRUE;
		boolean showObservations = getParameter(SHOW_OBSERVATIONS_PARAMETER)==Boolean.TRUE;
		
		
		//Load the samples and their results
		Set<Biosample> allSamples = new HashSet<>();
		for(Biosample topAnimal: study.getTopAttachedBiosamples()) {				
			allSamples.addAll(topAnimal.getSamplesFromStudyDesign(null, true));
		}
		
		DAOResult.attachOrCreateStudyResultsToTops(study, study.getTopAttachedBiosamples(), null, null);
		DAOResult.attachOrCreateStudyResultsToSamples(study, allSamples, null, null);
		
		if(allSamples.size()==0) throw new Exception("There are no samples to be reported. Make sure you have a sampling template with some required weighings.");
		
		//Find the samplings with weighing, the samplings with lengths, and the phases
		Set<NamedSampling> allNss = new HashSet<>();
		ListHashMap<String, Sampling> samplingHashMap = new ListHashMap<>();
		Set<String> shortKeySet = new TreeSet<>();
		Set<String> weighingKeys = new TreeSet<>();
		Set<String> lengthKeys = new TreeSet<>();
		for(StudyAction a: study.getStudyActions()) {
			for(NamedSampling ns: a.getNamedSamplings()) {
				if(allNss.contains(ns)) continue;
				allNss.add(ns);
				
				for(Sampling s: ns.getAllSamplings()) {
					if(s.isWeighingRequired() || !requiredOnly) {
						if(requiredOnly  || hasValue(study.getTopAttachedBiosamples(), s, DAOTest.WEIGHING_TESTNAME)) {
							shortKeySet.add(s.getDetailsShort());
							String key = normalize(s.getDetailsLong());
							weighingKeys.add(key);
							samplingHashMap.add(key, s);
						}
					}
					if(s.isLengthRequired()) {
						if(requiredOnly || hasValue(study.getTopAttachedBiosamples(), s, DAOTest.LENGTH_TESTNAME)) {
							String key = normalize(s.getDetailsLong());
							lengthKeys.add(key);
							samplingHashMap.add(key, s);
						}
					}
				}				
			}
		}		
		
		//Should we display the sampling complement? This only makes senses if the complements discriminates more the the short key
		boolean displayComplement = shortKeySet.size()!=weighingKeys.size();
		
		List<String> lengthsList = new ArrayList<>(lengthKeys);
		
		for (int i = 0; i < 2+lengthsList.size(); i++) {
			
			String sheetName = i==0? "OrgansWeights": i==1?  "per BW": "per "+lengthsList.get(i-2);
			String titleName = i==0? "Organs weight": i==1? "Organs weight per BW": "Organs weight per "+lengthsList.get(i-2);
			String unit = i==0? "g": i==1? "%BW": "%";
			
			//Create sheet
			Sheet sheet = createSheet(wb, sheetName);			
			createHeadersWithTitle(sheet, study, titleName);

			//Create a new table for each group
			int groupRow = 3;
			
			Map<Group, Map<String, String>> groupSampling2Range = new HashMap<>();
			for (Group group : study.getGroups()) {
				if (study.getTopAttachedBiosamples(group).size() == 0) continue;
				
				//Write group
				Map<String, String> sampling2RangeRef = groupSampling2Range.get(compareGroup2Groups.get(group));
				Map<String, String> sampling2Range = new HashMap<>();
				groupSampling2Range.put(group, sampling2Range);
				
				String name = group.getBlindedName(Spirit.getUsername()) + " " + group.getTreatmentDescription(-1, true);
				set(sheet, groupRow, 0, name, Style.S_TITLE14BLUE);
				groupRow++;	
				
				if(group.getNSubgroups()>1) set(sheet, groupRow+2, 0, "Subgroup", Style.S_TH_CENTER);
				set(sheet, groupRow+2, 1, "AnimalId", Style.S_TH_CENTER);
				set(sheet, groupRow+2, 2, "No", Style.S_TH_CENTER);
				set(sheet, groupRow+2, 3, "Necro", Style.S_TH_CENTER);
				set(sheet, groupRow+2, 4, " ", Style.S_TH_CENTER);

				//Write animalIds at row level
				int animalRow = groupRow + 3;
				for(Biosample animal: study.getTopAttachedBiosamples(group)) {
					Phase phase = animal.getExpectedEndPhase();
					if(group.getNSubgroups()>1) set(sheet, animalRow, 0, ""+(1+animal.getInheritedSubGroup()) , Style.S_TD_DOUBLE0);					
					set(sheet, animalRow, 1, animal.getSampleId(), Style.S_TD_CENTER);					
					set(sheet, animalRow, 2, animal.getSampleName(), Style.S_TD_BOLD_CENTER);
					set(sheet, animalRow, 3, phase==null?"": phase.getShortName(), Style.S_TD_CENTER);
					animalRow++;
				}

				//Write stats at row level
				set(sheet, animalRow++, 3, "Number", Style.S_TD_BOLD_CENTER);
				set(sheet, animalRow++, 3, "Mean", Style.S_TD_BOLD_CENTER);
				set(sheet, animalRow++, 3, "STDEV", Style.S_TD_BOLD_CENTER);
				set(sheet, animalRow++, 3, "SEM", Style.S_TD_BOLD_CENTER);
				if (sampling2RangeRef != null) {
					set(sheet, animalRow++, 3, "p", Style.S_TD_BOLD_CENTER);
					set(sheet, animalRow++, 3, "sign(*)", Style.S_TD_BOLD_CENTER);
				}
				
				//Add comparator column (BW, or length)
				if(i<=1) {
					//Add BW title			
					if(displayComplement) {
						set(sheet, groupRow, 4, "BW", Style.S_TH_CENTER);
						set(sheet, groupRow+1, 4, " ", Style.S_TH_CENTER);
					} else {
						set(sheet, groupRow+1, 4, "BW", Style.S_TH_CENTER);
					}
					set(sheet, groupRow+2, 4, "(g)", Style.S_TH_CENTER);
				} else {
					//Add Length data
					if(displayComplement) {
						set(sheet, groupRow, 4, lengthsList.get(i-2), Style.S_TH_CENTER);
						set(sheet, groupRow+1, 4, " ", Style.S_TH_CENTER);
					} else {
						set(sheet, groupRow+1, 4, lengthsList.get(i-2), Style.S_TH_CENTER);
					}
					set(sheet, groupRow+2, 4, "(mm)", Style.S_TH_CENTER);
				}

				//Write the comparator value
				List<Integer> animalRows = new ArrayList<Integer>();
				animalRow = groupRow + 3;
				for(Biosample animal: study.getTopAttachedBiosamples(group)) {	
					if(i<=1) {
						//Add BW data
						Double weight = getLastWeight(animal);
						set(sheet, animalRow, 4, weight, Style.S_TD_DOUBLE1);
					} else  {							
						//Add Length data
						Object v = getValue(animal, samplingHashMap.get(lengthsList.get(i-2)), DAOTest.LENGTH_TESTNAME);
						set(sheet, animalRow, 4, v, Style.S_TD_DOUBLE3);
						
					}	
					animalRows.add(animalRow);
					animalRow++;
				}
				
				//Write the stats for the sampling
				String range = convertLinesToCells(animalRows, 4);
				int countRow;
				int stDevRow;
				setFormula(sheet, countRow = animalRow++, 4, "IF(COUNT(" + range + ")>0,COUNT(" + range + "),\"\")", Style.S_TD_BLUE);
				String countCell = convertToCell(countRow, 4);
				setFormula(sheet, animalRow++, 4, "IF(ISNUMBER("+countCell+"), AVERAGE(" + range + "),\"\")", Style.S_TD_DOUBLE1_BLUE);
				setFormula(sheet, stDevRow = animalRow++, 4, "IF(ISNUMBER("+countCell+"), STDEV(" + range + "),\"\")", Style.S_TD_DOUBLE1_BLUE);
				setFormula(sheet, animalRow++, 4, "IF(ISNUMBER("+countCell+"), " + convertToCell(stDevRow, 4) + " / SQRT("+convertToCell(countRow, 4)+"),\"\")", Style.S_TD_DOUBLE1_BLUE);
				if (sampling2RangeRef != null) {
					int ttRow;
					setFormula(sheet, ttRow = animalRow++, 4, "IF(ISNUMBER("+countCell+"), TTEST(" + sampling2RangeRef.get(null) + "," + range + ",2,3),\"\")", Style.S_TD_DOUBLE3_RED);
					String ttCell = convertToCell(ttRow, 4);
					setFormula(sheet, animalRow++, 4, "IF("+ttCell+"=0,\" \",IF("+ttCell+"<0.001,\"***\",IF("+ttCell+"<0.01,\"**\",IF("+ttCell+"<0.05,\"*\",\" \"))))", Style.S_TD_RED);
				} 
				sampling2Range.put(null, range);
				
				//Loop through samplings
				int sampleIndex = 0;
				for(String s: weighingKeys) {
					Sampling first = samplingHashMap.get(s).iterator().next();
					if(displayComplement) {
						set(sheet, groupRow, 5+sampleIndex, first.getDetailsShort(), Style.S_TH_CENTER);
						set(sheet, groupRow+1, 5+sampleIndex, first.getDetailsComplement(), Style.S_TH_CENTER);
					} else {
						set(sheet, groupRow+1, 5+sampleIndex, first.getDetailsShort(), Style.S_TH_CENTER);
					}
					set(sheet, groupRow+2, 5+sampleIndex, "("+unit+")", Style.S_TH_CENTER);
					
					animalRow = groupRow + 3;
					animalRows = new ArrayList<>();
					for(Biosample animal: study.getTopAttachedBiosamples(group)) {						

						if(i==0) {
							
							Object v = getValue(animal, samplingHashMap.get(s), DAOTest.WEIGHING_TESTNAME);
							set(sheet, animalRow, 5+sampleIndex, v, Style.S_TD_DOUBLE3);
							
						} else {
							//compared to Organ sheet
							String range1 = "OrgansWeights!"+convertToCell(animalRow, 5+sampleIndex);
							String range2 = convertToCell(animalRow, 4);
							setFormula(sheet, animalRow, 5+sampleIndex, "IF(AND(ISNUMBER("+range1+"),ISNUMBER("+range2+")), 100*" + range1 + "/"+range2+", \"\")", Style.S_TD_DOUBLE3_BLUE);
						}
						animalRows.add(animalRow);
						animalRow++;
					}
					
					//Add the stats for the sampling
					range = convertLinesToCells(animalRows, 5+sampleIndex);
					setFormula(sheet, countRow = animalRow++, 5+sampleIndex, "IF(COUNT(" + range + ")>0, COUNT(" + range + "), \"\")", Style.S_TD_BLUE);
					countCell = convertToCell(countRow, 5+sampleIndex);
					setFormula(sheet, animalRow++, 5+sampleIndex, "IF(ISNUMBER("+countCell+"), AVERAGE(" + range + "), \"\")", Style.S_TD_DOUBLE3_BLUE);
					setFormula(sheet, stDevRow = animalRow++, 5+sampleIndex, "IF(ISNUMBER("+countCell+"), STDEV(" + range + "), \"\")", Style.S_TD_DOUBLE3_BLUE);
					setFormula(sheet, animalRow++, 5+sampleIndex, "IF(ISNUMBER("+countCell+"), " + convertToCell(stDevRow, 5+sampleIndex) + " / SQRT("+convertToCell(countRow, 5+sampleIndex)+"), \"\")", Style.S_TD_DOUBLE3_BLUE);
					if(sampling2RangeRef != null) {
						int ttRow;
						setFormula(sheet, ttRow = animalRow++, 5+sampleIndex, "IF(ISNUMBER("+countCell+"), TTEST(" + sampling2RangeRef.get(s) + "," + range + ",2,3), \"\")", Style.S_TD_DOUBLE3_RED);
						String ttCell = convertToCell(ttRow, 5+sampleIndex);
						setFormula(sheet, animalRow++, 5+sampleIndex, "IF("+ttCell+"=0,\" \",IF("+ttCell+"<0.001,\"***\",IF("+ttCell+"<0.01,\"**\",IF("+ttCell+"<0.05,\"*\",\" \"))))", Style.S_TD_RED);
					} 
					sampling2Range.put(s, range);

					sampleIndex++;
				}
				
				//Add observations?
				if(showObservations) {
					sampleIndex = 0;
					int observationRow = animalRow+1;
					for(String s: weighingKeys) {
						int row = observationRow;
						for(Biosample animal: study.getTopAttachedBiosamples(group)) {						
							String v = getValueString(animal, samplingHashMap.get(s), DAOTest.OBSERVATION_TESTNAME);
							if(v!=null && v.length()>0) {
								set(sheet, row, 3, "Observations: ", Style.S_TH_LEFT);
								set(sheet, row, 4, "", Style.S_TD_SMALL);
								set(sheet, row, 5 + sampleIndex, animal+": "+v, Style.S_TD_SMALL);
								row++;
								animalRow = Math.max(animalRow, row);
							}
						}
						sampleIndex++;
					}
					
					sampleIndex = 0;
					for (int j = 0; j < weighingKeys.size(); j++) {
						for(int row=observationRow; row<animalRow; row++) {
							if(get(sheet, row, 5+sampleIndex)==null || get(sheet, row, 5+sampleIndex).getStringCellValue()==null || get(sheet, row, 5+sampleIndex).getStringCellValue().length()==0) {
								set(sheet, row, 5+sampleIndex, "", Style.S_TD_SMALL);
							}							
						}
						sampleIndex++;
					}
				}
				
				groupRow=animalRow+2;				
			}
			POIUtils.autoSizeColumns(sheet);
		}
			

		if(wb.getNumberOfSheets()==0) throw new Exception("There are no samplings to be reported");

	}
	
	private static Double getValue(Biosample animal, Sampling s, String testName) {
		Phase p = animal.getExpectedEndPhase();
		Biosample sample = animal.getSample(s, p);
		if(sample==null) sample = animal.getSample(s, null);
		if(sample==null) return null;
		Test test = DAOTest.getTest(testName);
		Result r = sample.getAuxResult(test, p);
		if(r==null) r = sample.getAuxResult(test, null);
		if(r==null) return null;
		return r.getFirstAsDouble();
	}
	
	private static String getValueString(Biosample animal, Sampling s, String testName) {
		Phase p = animal.getExpectedEndPhase();
		Biosample sample = animal.getSample(s, p);
		if(sample==null) sample = animal.getSample(s, null);
		if(sample==null) return null;
		Test test = DAOTest.getTest(testName);

		Result r = sample.getAuxResult(test, p);
		if(r==null) r = sample.getAuxResult(test, null);
		if(r==null) return null;
		return r.getFirstValue()==null?"": r.getFirstValue().toString();
	}
	
	private static Double getValue(Biosample animal, Collection<Sampling> samplings, String testName) {
		List<Double> res = new ArrayList<Double>();
		for (Sampling s : samplings) {
			Double d = getValue(animal, s, testName);
			if(d!=null) res.add(d);
		}
		return StatUtils.getMean(res);
	}
	private static String getValueString(Biosample animal, Collection<Sampling> samplings, String testName) {
		List<String> res = new ArrayList<String>();
		for (Sampling s : samplings) {
			String d = getValueString(animal, s, testName);
			if(d!=null) res.add(d);
		}
		return MiscUtils.flatten(res, "; ");
	}
	
	public Double getLastWeight(Biosample animal) {
		
		List<Result> results = animal.getAuxResults(DAOTest.WEIGHING_TESTNAME, null);
		if(results==null) return null;
		Phase last = animal.getExpectedEndPhase();
		Result sel = null;
		for (int i = 0; i < results.size(); i++) {
			Result result = results.get(i);
			if(result.getInheritedPhase()==null) continue;
			if(result.getFirstAsDouble()==null) continue;
			if(last!=null && result.getInheritedPhase().compareTo(last)>0) continue;
			if(sel==null || sel.getInheritedPhase().compareTo(result.getInheritedPhase())<0) {
				sel = result;
			}
		}
		if(sel==null) {
			return null;
		} else {
			return sel.getFirstAsDouble();
		}
	}

	
	private static boolean hasValue(Collection<Biosample> animals, Sampling s, String testName) {
		for(Biosample animal: animals) {
			Object v = getValue(animal, s, testName);
			if(v!=null) return true;
		}
		return false;
	}

	public static String normalize(String s) {
		return s.toLowerCase().replace(",", "");
	}

	public static void main(String[] args)  {
		try {
			Spirit.setUser(DAOSpiritUser.loadUser("freyssj"));
			SampleMeasurementReport wg = new SampleMeasurementReport();
			wg.populateReport(DAOStudy.getStudyByStudyId("S-00446"));
			wg.export(null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}
	
}
