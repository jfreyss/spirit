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

import org.apache.poi.ss.usermodel.Sheet;

import com.actelion.research.spiritapp.spirit.services.report.AbstractReport;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.CompareUtils;

public class StatusReport extends AbstractReport {

	public StatusReport() {
		super(ReportCategory.TOP, "Living Status", "Living Status of each attached sample<br>"
				+ MiscUtils.convert2Html("\tNo\tContainer\tGroup\tSt.\tStatus\tPhase\tObservation\n"
						+ "ANL1\t\nANL2\t\n"));
	}


	@Override
	protected void populateWorkBook() throws Exception {


		//Create Header
		Sheet sheet = createSheet(wb, "Status");
		sheet.setFitToPage(true);
		createHeadersWithTitle(sheet, study, "Living Status");
		sheet.createRow(4).setHeightInPoints(23f);


		int col = 0;
		set(sheet, 5, col++, "Participant", Style.S_TH_CENTER);
		set(sheet, 5, col++, "No.", Style.S_TH_CENTER);
		set(sheet, 5, col++, "ContainerId", Style.S_TH_CENTER);
		set(sheet, 5, col++, "Group", Style.S_TH_CENTER);
		set(sheet, 5, col++, "St.", Style.S_TH_CENTER);
		set(sheet, 5, col++, "Status", Style.S_TH_CENTER);
		set(sheet, 5, col++, "Phase", Style.S_TH_CENTER);
		set(sheet, 5, col++, "Observation", Style.S_TH_CENTER);
		int maxCol = col-1;

		//Group separator?
		int line = 5;
		String cageBefore = null;
		Group groupBefore = null;

		DAOResult.attachOrCreateStudyResultsToTops(study, study.getAttachedBiosamples(), null, null);
		Test observationTest = DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME);
		if(observationTest==null) throw new Exception("The test "+DAOTest.OBSERVATION_TESTNAME+" does not exist");
		for (Biosample a : study.getAttachedBiosamples()) {
			if(CompareUtils.compare(groupBefore, a.getInheritedGroup())!=0) {
				drawLineUnder(sheet, line, 0, maxCol, (short)2);
			} else if(CompareUtils.compare(cageBefore, a.getContainerId())!=0) {
				drawLineUnder(sheet, line, 0, maxCol, (short)1);
			}

			Pair<Status, Phase> actionStatus = a.getLastActionStatus();
			//			if(actionStatus!=null && actionStatus.getStatus()!=a.getStatus()) throw new Exception("The status of "+a+" is inconsistent: last action is "+actionStatus.getStatus()+" but status is "+a.getStatus());

			Phase lastPhase = actionStatus.getSecond();
			//			if(actionStatus!=null && !actionStatus.getPhase().equals(lastPhase)) throw new Exception("The endPhase of "+a+" is inconsistent: last action is at "+actionStatus.getPhase()+" but endphase is "+lastPhase);
			Result lastObservation = lastPhase==null? null: a.getAuxResult(observationTest, lastPhase);


			Group g = a.getInheritedGroup();
			line++; col=0;
			set(sheet, line, col++, a.getSampleName(), Style.S_TD_CENTER);
			set(sheet, line, col++, a.getSampleId(), Style.S_TD_CENTER);
			set(sheet, line, col++, a.getContainerId(), Style.S_TD_CENTER);
			set(sheet, line, col++, g==null?"": g.getBlindedName(SpiritFrame.getUsername()) , Style.S_TD_LEFT);
			set(sheet, line, col++, g==null || g.getNSubgroups()<=1?"": (a.getInheritedSubGroup()+1), Style.S_TD_CENTER);



			set(sheet, line, col++, g==null?"": actionStatus.getFirst()==null?"": actionStatus.getFirst().getName() , Style.S_TD_LEFT);
			set(sheet, line, col++, g==null?"": lastPhase==null?"": lastPhase.getAbsoluteDateAndName() , Style.S_TD_LEFT);
			set(sheet, line, col++, g==null?"": lastObservation==null?"": lastObservation.getFirstValue(), Style.S_TD_LEFT);

			cageBefore = a.getContainerId();
			groupBefore = a.getInheritedGroup();
		}
		drawLineUnder(sheet, line, 0, maxCol, (short)1);
		POIUtils.autoSizeColumns(sheet);

		if(wb.getNumberOfSheets()==0) throw new Exception("There was no randomization fone for "+study);
	}

}
