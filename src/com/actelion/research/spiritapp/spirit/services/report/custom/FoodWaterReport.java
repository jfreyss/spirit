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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.actelion.research.spiritapp.spirit.services.report.AbstractReport;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.FoodWater;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOFoodWater;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.FormatterUtils;

public class FoodWaterReport extends AbstractReport {


	public FoodWaterReport() {
		super(ReportCategory.TOP, "Food / Water", "Food & Water consumption " + MiscUtils.convert2Html(
				"Group\tParticipant\tNo\tContainerId\tTare\tWeight\tN\tConsumption\n"
						+ "A\tParticipant1\tCage1\n"
						+ "A\tParticipant2\tCage1\n"));
	}

	@Override
	protected void populateWorkBook() throws Exception {
		createWorkBookNew(wb);
	}

	protected void createWorkBookNew(Workbook wb) throws Exception {
		if(study.getPhases().size()==0) throw new Exception("You cannot generate a FoodWater report if you don't have phases in your study");

		Row row;

		DAOResult.attachOrCreateStudyResultsToTops(study, study.getTopAttachedBiosamples(), null, null);
		List<FoodWater> fws = DAOFoodWater.getFoodWater(study, null);
		List<Phase> phases = FoodWater.getPhases(fws);

		Map<Biosample, List<Biosample>> mapHistory = DAORevision.getHistories(study.getTopAttachedBiosamples());
		boolean hasCageChanges = false;
		for (Biosample top : study.getTopAttachedBiosamples()) {
			Set<String> containerIds = new HashSet<>();
			for (Phase phase : phases) {

				String containerId = getContainerAt(top, phase, fws, mapHistory.get(top));
				if(containerId!=null && containerId.length()>0) containerIds.add(containerId);
			}
			System.out.println("FoodWaterReport.createWorkBookNew() "+top+">"+containerIds);
			if(containerIds.size()>1) {
				hasCageChanges = true;
				break;
			}
		}
		boolean displayCageEachPhase = study.getFirstDate()!=null && study.getPhaseFormat()==PhaseFormat.DAY_MINUTES && hasCageChanges;
		System.out.println("FoodWaterReport.createWorkBookNew() hasCageChanges="+hasCageChanges+" displayCageEachPhase="+displayCageEachPhase);


		//Loop through Food (i=0) and water (i==1) reports
		for (int i = 0; i < 2; i++) {
			//Check if we have some data
			boolean hasData = false;
			for (FoodWater fw : fws) {
				if(i==0 && (fw.getFoodTare()!=null || fw.getFoodWeight()!=null)) hasData = true;
				else if(i==1 && (fw.getWaterTare()!=null || fw.getWaterWeight()!=null)) hasData = true;
			}
			if(!hasData) continue;


			Sheet sheet = createSheet(wb, i==0?"Food":"Water");

			createHeadersWithTitleSubtitle(sheet, study, i==0?"Food Consumption [g/animal/day]":"Water Consumption [ml/animal/day]", null);

			//Create Table Header
			row = sheet.createRow(5);
			row.setHeightInPoints(21f);
			row = sheet.createRow(6);
			row.setHeightInPoints(21f);
			int x = 0;
			set(sheet, 6, x++, "Group", Style.S_TH_CENTER);
			set(sheet, 6, x++, "Id", Style.S_TH_CENTER);
			set(sheet, 6, x++, "No", Style.S_TH_CENTER);
			if(!displayCageEachPhase) {
				set(sheet, 6, x++, "Container", Style.S_TH_CENTER);
			}

			Phase previousPhase = null;
			for (Phase phase : phases) {
				if(previousPhase==null) {
					set(sheet, 5, x, phase.getShortName() + (phase.getAbsoluteDate()!=null?"["+FormatterUtils.formatDate(phase.getAbsoluteDate())+"]":""), Style.S_TH_CENTER, 1, 1);
					if(displayCageEachPhase) set(sheet, 6, x++, "Container", Style.S_TH_CENTER);
					if(!displayCageEachPhase) set(sheet, 6, x++, "newTare", Style.S_TH_CENTER);
				} else {
					set(sheet, 5, x, " -> "+ phase.getShortName() + (phase.getAbsoluteDate()!=null?" ["+FormatterUtils.formatDate(phase.getAbsoluteDate())+"]":""), Style.S_TH_CENTER, 1, 2 + (displayCageEachPhase?4:2));
					if(displayCageEachPhase) set(sheet, 6, x++, "ContainerId", Style.S_TH_CENTER);
					if(displayCageEachPhase) set(sheet, 6, x++, "Tare", Style.S_TH_CENTER);
					if(displayCageEachPhase) set(sheet, 6, x++, "Weight", Style.S_TH_CENTER);
					if(displayCageEachPhase) set(sheet, 6, x++, "days", Style.S_TH_CENTER);
					if(!displayCageEachPhase) set(sheet, 6, x++, "oldTare", Style.S_TH_CENTER);
					if(!displayCageEachPhase) set(sheet, 6, x++, "newTare", Style.S_TH_CENTER);
					set(sheet, 6, x++, "n", Style.S_TH_CENTER);
					set(sheet, 6, x++, "Cons.", Style.S_TH_CENTER);
				}
				previousPhase = phase;
			}
			int maxX = x-1;

			//
			//Create table data
			int y = 7;
			Group previousGroup = null;
			ListHashMap<Group, Integer> group2Lines = new ListHashMap<>();
			for (Biosample b: study.getTopAttachedBiosamples()) {
				Group gr = b.getInheritedGroup();

				group2Lines.add(gr, y);

				x = 0;
				set(sheet, y, x++, b.getInheritedGroupString(SpiritFrame.getUsername()), Style.S_TD_BOLD_LEFT);
				set(sheet, y, x++, b.getSampleId(), Style.S_TD_CENTER);
				set(sheet, y, x++, b.getSampleName(), Style.S_TD_CENTER);

				String containerId = null;
				if(!displayCageEachPhase) {
					//Display cage at time of death
					containerId = b.getContainerId();
					if(containerId==null || containerId.length()==0) {
						List<Biosample> history = mapHistory.get(b);
						for(Biosample h: history) {
							if(h.getContainerId()!=null && h.getContainerId().length()>0) {
								containerId = h.getContainerId();
								break;
							}
						}
					}

					set(sheet, y, x++, containerId, Style.S_TD_BOLD_CENTER);
				}

				//data: Loop through phases
				previousPhase = null;
				for (Phase phase : phases) {

					if(displayCageEachPhase) {
						//Find the containerId of this sample at this date:
						containerId = getContainerAt(b, phase, fws, mapHistory.get(b));
					}
					if(containerId == null || containerId.length()==0) containerId = "??";
					FoodWater fw = FoodWater.extract(fws, containerId, phase);
					FoodWater previousFW = fw==null? null: fw.getPreviousFromList(fws, i==1);

					Result r = b.getAuxResult(DAOTest.getTest(DAOTest.FOODWATER_TESTNAME), phase);
					ResultValue val = r==null || r.getOutputResultValues().size()<2? null: i==0? r.getOutputResultValues().get(0): r.getOutputResultValues().get(1);
					String value = val==null? null: val.getValue();

					if(previousPhase==null) {
						if(displayCageEachPhase) set(sheet, y, x++, containerId, Style.S_TD_BOLD_CENTER);
						if(!displayCageEachPhase) set(sheet, y, x++, fw==null? null: i==0? fw.getFoodTare(): fw.getWaterTare(), Style.S_TD_DOUBLE1);
					} else {
						if(displayCageEachPhase) set(sheet, y, x++, containerId, Style.S_TD_BOLD_CENTER);
						if(displayCageEachPhase) set(sheet, y, x++, previousFW==null? null: i==0? previousFW.getFoodTare(): previousFW.getWaterTare(), Style.S_TD_DOUBLE1);
						set(sheet, y, x++, fw==null? null: i==0? fw.getFoodWeight(): fw.getWaterWeight(), Style.S_TD_DOUBLE1);
						if(displayCageEachPhase) set(sheet, y, x++, previousFW==null? null: fw.getPhase().getDays()-previousFW.getPhase().getDays(), Style.S_TD_DOUBLE0);
						if(!displayCageEachPhase) set(sheet, y, x++, fw==null? null: i==0? fw.getFoodTare(): fw.getWaterTare(), Style.S_TD_DOUBLE1);
						set(sheet, y, x++, fw==null? null: fw.getNAnimals(), Style.S_TD_DOUBLE0);
						set(sheet, y, x++, value, Style.S_TD_DOUBLE1_BLUE);
					}
					previousPhase = phase;
				}

				if(previousGroup!=null && !previousGroup.equals(gr)) {
					drawLineAbove(sheet, y, 0, maxX, (short)1);
				}
				previousGroup = gr;
				y++;
			}

			y++;


			////////////////////// AVERAGES
			x = 0;
			set(sheet, y+1, x++, "Averages", Style.S_TH_CENTER);
			set(sheet, y+1, x++, "", Style.S_TH_CENTER);
			set(sheet, y+1, x++, "", Style.S_TH_CENTER);
			if(!displayCageEachPhase) set(sheet, y+1, x++, "", Style.S_TH_CENTER);
			previousPhase = null;
			for (Phase phase : phases) {
				if(previousPhase==null) {
					set(sheet, y, x, phase.getShortName() + (phase.getAbsoluteDate()!=null?" ["+FormatterUtils.formatDate(phase.getAbsoluteDate())+"]":""), Style.S_TH_CENTER);
					set(sheet, y+1, x++, "", Style.S_TH_CENTER);
				} else {
					set(sheet, y, x, " -> "+ phase.getShortName() + (phase.getAbsoluteDate()!=null?" ["+FormatterUtils.formatDate(phase.getAbsoluteDate())+"]":""), Style.S_TH_CENTER, 1, 2 + (displayCageEachPhase?4:2));
					for(int k=0; k<(displayCageEachPhase?4:2); k++) {
						set(sheet, y+1, x++, "", Style.S_TH_CENTER);
					}
					set(sheet, y+1, x++, "", Style.S_TH_CENTER);
					set(sheet, y+1, x++, "Cons.", Style.S_TH_CENTER);
				}
				previousPhase = phase;
			}

			y+=2;
			for (Group gr : study.getGroups()) {
				if(study.getTopAttachedBiosamples(gr).size()==0) continue;
				x = 0;
				set(sheet, y, x++, gr==null? "N/A": gr.getBlindedName(SpiritFrame.getUsername()), Style.S_TD_BOLD_LEFT);
				set(sheet, y, x++, "" , Style.S_TD_LEFT);
				set(sheet, y, x++, "" , Style.S_TD_LEFT);
				if(!displayCageEachPhase) set(sheet, y, x++, "" , Style.S_TD_BOLD_LEFT);

				//data: Loop through phases
				previousPhase = null;
				for (Phase phase : phases) {

					List<Integer> lines = group2Lines.get(gr);
					if(previousPhase==null) {
						set(sheet, y, x, "", Style.S_TD_DOUBLE1);
						x+=1;
					} else {
						for(int k=0; k<(displayCageEachPhase?4:2); k++) {
							set(sheet, y, x++, "", Style.S_TD_CENTER);
						}

						if(lines!=null && lines.size()>0) {
							int valcol = x+1;
							set(sheet, y, x++, "", Style.S_TD_CENTER);
							setFormula(sheet, y, x++, "IF(COUNT("+convertLinesToCells(lines, valcol)+")>0, AVERAGE("+convertLinesToCells(lines, valcol)+"), \"\")", Style.S_TD_DOUBLE1_BLUE);
						} else {
							set(sheet, y, x++, "", Style.S_TD_CENTER);
							set(sheet, y, x++, "", Style.S_TD_CENTER);
						}
					}
					previousPhase = phase;

				}
				y++;
			}



			POIUtils.autoSizeColumns(sheet);
		}
	}

	/**
	 * Get the ContainerId of the given biosample at the given date
	 * @param b
	 * @param phase
	 * @param fws
	 * @param history
	 * @return
	 */
	private String getContainerAt(Biosample b, Phase phase, Collection<FoodWater> fws, List<Biosample> history) {
		String containerId = "";

		Map<String, Date> cage2fwDate = new HashMap<>();
		for(FoodWater fw: FoodWater.extract(fws, phase)) {
			cage2fwDate.put(fw.getContainerId(), fw.getCreDate());
		}
		Date d = null;
		containerId = "";
		for(Biosample h: history) {
			String cid = h.getContainerId();
			long refTime = cage2fwDate.get(cid)!=null? cage2fwDate.get(cid).getTime(): (phase.getAbsoluteDate().getTime()+24*3600*1000L);
			if(h.getUpdDate().getTime()>refTime) continue; //skip cage change if it happened after the phase
			if(cid==null || cid.length()==0 && h.getUpdDate().getTime()>=refTime) continue; //skip cage change to null if it happened at the phase (dead animal)
			if(d==null || h.getUpdDate().compareTo(d)>0) {
				d = h.getUpdDate();
				containerId = cid;
			}
		}
		return containerId;
	}

	public static void main(String[] args) throws Exception {
		Study study = DAOStudy.getStudyByStudyId("S-00715");
		FoodWaterReport rep = new FoodWaterReport();
		rep.populateReport(study);
		rep.export(null);


	}

}
