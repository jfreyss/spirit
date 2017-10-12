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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Randomization;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

public class StudyGroupAssignmentReport extends AbstractReport {

	public StudyGroupAssignmentReport() {
		super(ReportCategory.STUDY,
				"Group Assignment",
				"Group Assignment done through the Group Assignment Wizard, showing the data before and after the randomization.<ul>"
						+ "<li>Each tab shows the assignment done at one given phase</ul>"
						+ MiscUtils.convert2Html("No\tBW\tParticipantId\tNewNo\tContainerId\tGroup\tMetadata\tTreatment\n"
								+ "\t\tParticipantId1\n"
								+ "\t\tParticipantId2\n"));
	}


	@Override
	protected void populateWorkBook() throws Exception {
		List<Biosample> topBiosamples = new ArrayList<>(study.getParticipants());
		Biotype biotype = Biosample.getBiotype(topBiosamples);
		List<Phase> phases = new ArrayList<>();
		phases.addAll(study.getPhases());
		phases.add(null);

		//Rnd Data
		for(Phase phase: phases) {

			List<AttachedBiosample> samples;
			int nData;

			if(phase==null) {
				//Current Status
				nData = 0;
				samples = new ArrayList<>();
				int count = 0;
				for (Biosample biosample : topBiosamples) {
					AttachedBiosample sample = new AttachedBiosample();
					sample.setBiosample(biosample);
					sample.setContainerId(biosample.getContainerId());
					sample.setGroup(biosample.getInheritedGroup());
					sample.setNo(++count);
					sample.setSampleId(biosample.getTopParent().getSampleId());
					sample.setSampleName(biosample.getSampleName());
					sample.setSubGroup(biosample.getInheritedSubGroup());
					samples.add(sample);
				}
			} else {
				//Intermediate phase
				if(!phase.hasRandomization()) continue;

				//Load data
				Randomization rnd = phase.getRandomization();
				DAOStudy.loadBiosamplesFromStudyRandomization(rnd);
				samples = rnd.getSamples();
				if(samples.size()==0) continue;

				nData = rnd.getNData();
			}

			Collections.sort(samples, new Comparator<AttachedBiosample>() {
				@Override
				public int compare(AttachedBiosample o1, AttachedBiosample o2) {
					int c = CompareUtils.compare(o1.getGroup(), o2.getGroup());
					if(c!=0) return c;
					c = CompareUtils.compare(o1.getSubGroup(), o2.getSubGroup());
					if(c!=0) return c;
					c = CompareUtils.compare(o1.getSampleName(), o2.getSampleName());
					return c;
				}
			});


			//Create Header
			Sheet sheet = createSheet(wb, "GA "+ (phase==null?"Final": phase.getShortName()));
			sheet.setFitToPage(true);
			createHeadersWithPhase(sheet, study, phase, "Group Assignment Data");
			sheet.createRow(4).setHeightInPoints(23f);


			int col = 0;
			set(sheet, 5, col++, "No.", Style.S_TH_CENTER);
			set(sheet, 5, col++, "BW [g]", Style.S_TH_CENTER);
			for(int i=0; i<nData; i++) set(sheet, 5, col++, "Data"+(i+1), Style.S_TH_CENTER);
			set(sheet, 4, 0, "Before Rando.", Style.S_TH_CENTER, 1, col);


			set(sheet, 5, col++, "AnimalId", Style.S_TH_CENTER);
			set(sheet, 5, col++, "New No.", Style.S_TH_CENTER);
			set(sheet, 5, col++, "Cage", Style.S_TH_CENTER);
			set(sheet, 5, col++, "Group", Style.S_TH_CENTER);
			set(sheet, 5, col++, "St.", Style.S_TH_CENTER);
			if(biotype!=null) {
				for (BiotypeMetadata bm : biotype.getMetadata()) {
					set(sheet, 5, col++, bm.getName(), Style.S_TH_CENTER);
				}
			}
			set(sheet, 5, col++, "Treatment", Style.S_TH_CENTER);
			set(sheet, 4, 2, "After Rando.", Style.S_TH_CENTER, 1, col-2);
			int maxCol = col-1;

			//Group separator?
			int line = 5;
			String cageBefore = null;
			Group groupBefore = null;
			for (AttachedBiosample r : samples) {
				if(r.getBiosample()==null || r.getBiosample().getId()<=0) continue;
				if(CompareUtils.compare(groupBefore, r.getGroup())!=0) {
					drawLineUnder(sheet, line, 0, maxCol, (short)2);
				} else if(CompareUtils.compare(cageBefore, r.getContainerId())!=0) {
					drawLineUnder(sheet, line, 0, maxCol, (short)1);
				}

				line++;
				Group g = r.getGroup();
				Biosample b = r.getBiosample();
				col=0;
				set(sheet, line, col++, r.getNo(), Style.S_TD_CENTER);
				set(sheet, line, col++, r.getWeight(), Style.S_TD_CENTER);
				for(int i=0; i<nData; i++) set(sheet, line, col++, r.getDataList()!=null && i<r.getDataList().size()? r.getDataList().get(i): null, Style.S_TD_CENTER);
				set(sheet, line, col++, r.getSampleId(), Style.S_TD_CENTER);
				set(sheet, line, col++, r.getSampleName(), Style.S_TD_CENTER);
				set(sheet, line, col++, r.getContainerId(), Style.S_TD_CENTER);
				set(sheet, line, col++, g==null?"": g.getBlindedName(SpiritFrame.getUsername()) , Style.S_TD_LEFT);
				set(sheet, line, col++, g==null || g.getNSubgroups()<=1?"": (r.getSubGroup()+1), Style.S_TD_CENTER);
				if(biotype!=null) {
					for (BiotypeMetadata bm : biotype.getMetadata()) {
						set(sheet, line, col++, b.getMetadataValue(bm), Style.S_TD_CENTER);
					}
				}
				set(sheet, line, col++, g==null?"": g.getTreatmentDescription() , Style.S_TD_LEFT);

				cageBefore = r.getContainerId();
				groupBefore = r.getGroup();
			}
			drawLineUnder(sheet, line, 0, maxCol, (short)1);
			POIUtils.autoSizeColumns(sheet);
		}

		if(wb.getNumberOfSheets()==0) throw new Exception("There was no randomization fone for "+study);
	}

}
