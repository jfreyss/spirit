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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Sheet;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;

public class SamplesToxicologyReport extends AbstractReport {
	
	
	private static ReportParameter REQUIRED_ONLY_PARAMETER = new ReportParameter("Shows Only Required Measurements", Boolean.FALSE);
//	private static ReportParameter VALUES_ONLY_PARAMETER = new ReportParameter("Hide Organs without values", Boolean.TRUE);

	

	
	public SamplesToxicologyReport() {		
		super(ReportCategory.SAMPLES, 
				"Samples Weighing (one table per group)", 
				"One sheet per group, the table also contain the observations", 
				new ReportParameter[]{REQUIRED_ONLY_PARAMETER});
	}

	@Override
	protected void populateWorkBook() throws Exception {	
		//Load the weiging, 
		boolean requiredOnly = getParameter(REQUIRED_ONLY_PARAMETER)==Boolean.TRUE;
//		boolean valuesOnly = getParameter(VALUES_ONLY_PARAMETER)==Boolean.TRUE;
		
		
		Set<Biosample> allSamples = new HashSet<Biosample>();
		for(Biosample topAnimal: study.getTopAttachedBiosamples()) {				
			allSamples.addAll(topAnimal.getSamplesFromStudyDesign(null, requiredOnly));
		}
		
		DAOResult.attachOrCreateStudyResultsToSpecimen(study, study.getTopAttachedBiosamples(), null, null);
		DAOResult.attachOrCreateStudyResultsToSamples(study, allSamples, null, null);
		
		if(allSamples.size()==0) throw new Exception("There are no samples to be reported. Make sure you have a sampling template with some required weighings.");
		
		//Create the workbook, 1 sheet per group
		for(Group group: study.getGroups()) {
			
			//Find the samplings
			Set<NamedSampling> nss = new HashSet<NamedSampling>();
			for(StudyAction a: study.getStudyActions(group)) {
				nss.addAll(a.getNamedSamplings());
			}
						
			Set<Sampling> weighingSamplings = new TreeSet<Sampling>();
			Set<Sampling> lengthSamplings = new TreeSet<Sampling>();
			Set<Sampling> observationSamplings = new TreeSet<Sampling>();
			
			//For each test, extract the samplings that should be displayed (required, with values) depending of filters
			for(NamedSampling ns: nss) {
				for(Sampling s: ns.getAllSamplings()) {
					if(s.isWeighingRequired() || !requiredOnly) {
						if(requiredOnly  || hasValue(study.getTopAttachedBiosamples(), s, DAOTest.WEIGHING_TESTNAME)) {
							weighingSamplings.add(s);
						}
					}
					if(s.isLengthRequired() || !requiredOnly) {
						if(requiredOnly || hasValue(study.getTopAttachedBiosamples(), s, DAOTest.LENGTH_TESTNAME)) {
							lengthSamplings.add(s);
						}
					}
					if(s.isCommentsRequired() || !requiredOnly) {
						if(requiredOnly || hasValue(study.getTopAttachedBiosamples(), s, DAOTest.OBSERVATION_TESTNAME)) {
							observationSamplings.add(s);
						}
					}
				}
			}

			//Skip groups with no samplings
			if(weighingSamplings.size()+lengthSamplings.size()+observationSamplings.size()==0) continue;

			//Start the report
			Sheet sheet = createSheet(wb, "Sampling Gr. " + group.getShortName());
			sheet.setFitToPage(true);		
			createHeadersWithTitleSubtitle(sheet, study, "Organ Weights", group.getBlindedName(Spirit.getUsername()) + (group.getTreatmentDescription().length()>0?" ("+group.getTreatmentDescription()+")":""));
			
			///////////////
			//		0		1		2		3			4		5
			//5											EndPhase
			//6											AnimalId
			//7											Name
			//8						BW	
			//				Length	Organ   Formalin4%
			//				Weight	Organ	Formalin4%
			//				Obs		Organ	Formalin4%
			
			//Column Headers
			int col = 4;
			for(Biosample animal: group.getTopAttachedBiosamples()) {
			
				set(sheet, 4, col, animal.getEndPhase()==null?"": animal.getEndPhase().getShortName(), Style.S_TH_CENTER);
				set(sheet, 5, col, animal.getSampleId(), Style.S_TH_CENTER);
				set(sheet, 6, col, animal.getSampleName(), Style.S_TH_CENTER);
				
				col++;
			}
			set(sheet, 6, col++, "Mean", Style.S_TH_CENTER);
			set(sheet, 6, col++, "SD", Style.S_TH_CENTER);
			
			
			//Row Headers
			int row = 7;
			set(sheet, row, 1, "", Style.S_TH_LEFT);
			set(sheet, row, 2, "BW", Style.S_TH_LEFT);
			set(sheet, row, 3, "", Style.S_TH_LEFT);
			row++;
			for(Sampling sampling: lengthSamplings) {
				set(sheet, row, 1, "Length", Style.S_TH_LEFT);				
				set(sheet, row, 2, sampling.getDetailsShort(), Style.S_TH_LEFT);				
				set(sheet, row, 3, sampling.getDetailsComplement(), Style.S_TH_LEFT);				
				row++;
			}			
			for(Sampling sampling: weighingSamplings) {
				set(sheet, row, 1, "Weight", (Style.S_TH_LEFT));				
				set(sheet, row, 2, sampling.getDetailsShort(), Style.S_TH_LEFT);				
				set(sheet, row, 3, sampling.getDetailsComplement(), Style.S_TH_LEFT);				
				row++;
			}
			for(Sampling sampling: observationSamplings) {
				set(sheet, row, 1, "Observation", (Style.S_TH_LEFT));				
				set(sheet, row, 2, sampling.getDetailsShort(), Style.S_TH_LEFT);				
				set(sheet, row, 3, sampling.getDetailsComplement(), Style.S_TH_LEFT);				
				row++;
			}
			
			
			//Data
			col = 4;
			for(Biosample animal: group.getTopAttachedBiosamples()) {
				row = 7;
				//BW
				set(sheet, row, col, getLastWeight(animal), Style.S_TD_DOUBLE1);
				row++;
				
				//Length
				for(Sampling sampling: lengthSamplings) {
					Object v = getValue(animal, sampling, DAOTest.LENGTH_TESTNAME);
					set(sheet, row, col, v==null? null: v, Style.S_TD_DOUBLE2);
					row++;					
				}
				//Weighing
				for(Sampling sampling: weighingSamplings) {
					Object v = getValue(animal, sampling, DAOTest.WEIGHING_TESTNAME);
					set(sheet, row, col, v==null? null: v, Style.S_TD_DOUBLE3);
					row++;					
				}
				//Observations
				for(Sampling sampling: observationSamplings) {
					Object v = getValue(animal, sampling, DAOTest.OBSERVATION_TESTNAME);
					set(sheet, row, col, v==null? null: v, Style.S_TD_SMALL);
					row++;					
				}
				col++;
			}
			//Mean, SD
			row = 7;			
			setAverage(sheet, row, col, convertToCell(row, 4)+":"+convertToCell(row, col-1), Style.S_TD_DOUBLE1_BLUE); //BW
			setStd(sheet, row, col+1, convertToCell(row, 4)+":"+convertToCell(row, col-1), Style.S_TD_DOUBLE1_BLUE);
			row++;	
			drawLineUnder(sheet, row-1, 1, col+1, (short) 1);
			for(int i=0; i<lengthSamplings.size(); i++) {
				setAverage(sheet, row, col, convertToCell(row, 4)+":"+convertToCell(row, col-1), Style.S_TD_DOUBLE2_BLUE);
				setStd(sheet, row, col+1, convertToCell(row, 4)+":"+convertToCell(row, col-1), Style.S_TD_DOUBLE2_BLUE);
				row++;					
			}
			for(int i=0; i<weighingSamplings.size(); i++) {
				setAverage(sheet, row, col, convertToCell(row, 4)+":"+convertToCell(row, col-1), Style.S_TD_DOUBLE3_BLUE);
				setStd(sheet, row, col+1, convertToCell(row, 4)+":"+convertToCell(row, col-1), Style.S_TD_DOUBLE3_BLUE);
				row++;					
			}
			drawLineUnder(sheet, row-1, 1, col+1, (short) 1);
			
			wb.setSelectedTab(wb.getSheetIndex(sheet));
			wb.setActiveSheet(wb.getSheetIndex(sheet));
			POIUtils.autoSizeColumns(sheet);
		}

		if(wb.getNumberOfSheets()==0) throw new Exception("There are no samplings to be reported");


	}	
	
	private static Object getValue(Biosample animal, Sampling s, String testName) {
		Phase p = animal.getEndPhase();
		Biosample sample = animal.getSample(s, p);
		
		if(sample==null) sample = animal.getSample(s, null);
		
		if(sample==null) return null;
		Result r = sample.getAuxResult(testName, p);
		if(r==null) r = sample.getAuxResult(testName, null);
		
		if(animal.getSampleId().equals("104058")) {
			System.out.println("SamplingToxicologyReport.getValue() "+animal+"/"+s+"/"+testName+">"+sample+" >"+r);
		}
		
		if(r==null) return null;
		return r.getFirstValue();
	}
	
	public Double getLastWeight(Biosample animal) {
		
		List<Result> results = animal.getAuxResults(DAOTest.WEIGHING_TESTNAME, null);
		if(results==null) return null;
		Phase last = animal.getEndPhase();
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
	
	public static void main(String[] args)  {
		try {
			Spirit.setUser(DAOSpiritUser.loadUser("freyssj"));
			SamplesToxicologyReport wg = new SamplesToxicologyReport();
			wg.populateReport(DAOStudy.getStudyByStudyId("S-00446"));
			wg.export(null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}


}
