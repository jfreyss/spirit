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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.ActionBiosample;
import com.actelion.research.spiritcore.business.biosample.ActionContainer;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.FoodWater;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.services.dao.DAOFoodWater;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.util.FormatterUtils;

public class SpecimenFoodWaterReport extends AbstractReport {

//	private static final ReportParameter FWReportParameter = new ReportParameter("Older version", Boolean.FALSE);
//	private static final ReportParameter ADD_RATIO_PARAMETER = new ReportParameter("Add Ratio", Boolean.FALSE);
	
	public SpecimenFoodWaterReport() {
		super(ReportCategory.SPECIMEN, "Food & Water", "Report on the Food & Water consumption", new ReportParameter[] {});
	}

	@Override
	protected void populateWorkBook() throws Exception {
		createWorkBookNew(wb);
	}
	
	protected void createWorkBookNew(Workbook wb) throws Exception {
		if(study.getPhases().size()==0) throw new Exception("You cannot generate a FoodWater report if you don't have phases in your study");
		
		Row row;
		
		DAOResult.attachOrCreateStudyResultsToSpecimen(study, study.getTopAttachedBiosamples(), null, false);
		List<FoodWater> fws = DAOFoodWater.getFoodWater(study, null);
		List<Phase> phases = FoodWater.getPhases(fws);
		
		
//		List<String> containerIds = FoodWater.getContainerIds(fws);
//		boolean hasCageChanges = containerIds.size()>study.getTopAttachedBiosamples().size();

		boolean hasCageChanges = false;
		for (Biosample top : study.getTopAttachedBiosamples()) {
			System.out.println("FoodWaterReport.createWorkBookNew() "+top+" > "+top.getActions(ActionContainer.class).size()+" cage changes");
			if(top.getActions(ActionContainer.class).size()>1) {
				hasCageChanges = true;
			}
		}
		
		boolean displayCageEachPhase = study.getFirstDate()!=null && study.getPhaseFormat()==PhaseFormat.DAY_MINUTES && hasCageChanges;
		
		
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
				set(sheet, 6, x++, "Cage", Style.S_TH_CENTER);
			}
			
			Phase previousPhase = null;
			for (Phase phase : phases) {
				if(previousPhase==null) {
					set(sheet, 5, x, phase.getShortName() + (phase.getAbsoluteDate()!=null?"["+FormatterUtils.formatDate(phase.getAbsoluteDate())+"]":""), Style.S_TH_CENTER, 1, 1);
					if(displayCageEachPhase) set(sheet, 6, x++, "Cage", Style.S_TH_CENTER);
					if(!displayCageEachPhase) set(sheet, 6, x++, "newTare", Style.S_TH_CENTER);
				} else {
					set(sheet, 5, x, " -> "+ phase.getShortName() + (phase.getAbsoluteDate()!=null?" ["+FormatterUtils.formatDate(phase.getAbsoluteDate())+"]":""), Style.S_TH_CENTER, 1, 2 + (displayCageEachPhase?4:2));						
					if(displayCageEachPhase) set(sheet, 6, x++, "Cage", Style.S_TH_CENTER);
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
			ListHashMap<Group, Integer> group2Lines = new ListHashMap<Group, Integer>();
			for (Biosample b: study.getTopAttachedBiosamples()) {
				Group gr = b.getInheritedGroup();
				
				group2Lines.add(gr, y);
				
				x = 0;
				set(sheet, y, x++, b.getInheritedGroupString(Spirit.getUsername()), Style.S_TD_BOLD_LEFT);
				set(sheet, y, x++, b.getSampleId(), Style.S_TD_LEFT);
				set(sheet, y, x++, b.getSampleName(), Style.S_TD_LEFT);
				if(!displayCageEachPhase) { 
					set(sheet, y, x++, b.getContainerId() , Style.S_TD_BOLD_LEFT);
				}

				//data: Loop through phases
				previousPhase = null;
				for (Phase phase : phases) {
					
					String containerId = b.getContainerId();
					if(displayCageEachPhase) {
						//Find the containerId of this sample at this date:
						//The containerId is the last one that was set before the FW measurement. 
						Map<String, Date> cage2fwDate = new HashMap<>();
						for(FoodWater fw: FoodWater.extract(fws, phase)) {
							cage2fwDate.put(fw.getContainerId(), fw.getCreDate());
						}
						System.out.println("FoodWaterReport.createWorkBookNew() "+phase+" > "+cage2fwDate);
						Date d = null; 
						containerId = "";
						for(ActionBiosample a: b.getActions(ActionContainer.class)) {							
							String cid = ((ActionContainer)a).getDetails();
							cid = cid.replace("Set Container to ", "");
							long refTime = cage2fwDate.get(cid)!=null? cage2fwDate.get(cid).getTime(): (phase.getAbsoluteDate().getTime()+24*3600*1000L);
							System.out.println("FoodWaterReport.createWorkBookNew() "+b+">"+cid+" at "+a.getUpdDate()+" /ref="+refTime);
							if( (d==null || a.getUpdDate().after(d)) && a.getUpdDate().getTime()<=refTime) {
								d = a.getUpdDate();
								containerId = cid;
							}
						}
					}
					
					FoodWater fw = FoodWater.extract(fws, containerId, phase);
					FoodWater previousFW = fw==null? null: fw.getPreviousFromList(fws, i==1);
					
					Result r = b.getAuxResult(DAOTest.FOODWATER_TESTNAME, phase);
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
			set(sheet, y+1, x++, "Group", Style.S_TH_CENTER);
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
				set(sheet, y, x++, gr==null? "N/A": gr.getBlindedName(Spirit.getUsername()), Style.S_TD_BOLD_LEFT);
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

		/*
	protected void createWorkBookOld(Workbook wb) throws Exception {
		Row row;
		
		List<FoodWater> fws = DAOFoodWater.getFoodWater(study, null, null);
		List<Phase> phases = FoodWater.getPhases(fws);
		List<Container> cages = FoodWater.getCages(fws);
		
		
		
		for (int i = 0; i < 2; i++) {
			
			ListHashMap<Group, Integer> group2Lines = new ListHashMap<Group, Integer>();

			Sheet sheet = createSheet(wb, i==0?"Food":"Water");
			sheet.setFitToPage(true);
			
			//Create Header
			createHeadersWithPhase(sheet, study, null, i==0?"Food Consumption [g/animal/day]":"Water Consumption [ml/animal/day]");
	
			//Create table Header			
			row = sheet.createRow(5);
			row.setHeightInPoints(21f);
			row = sheet.createRow(6);
			row.setHeightInPoints(21f);
			set(sheet, 6, 0, "Group", (Style.S_TH));
			set(sheet, 6, 1, "AnimalIds", (Style.S_TH));
			set(sheet, 6, 2, "AnimalNos", (Style.S_TH));
			set(sheet, 6, 3, "Cage", (Style.S_TH));
			
			//headers: Loop through phases
			{
				int x = 4;
				boolean first = true;
				for (Phase phase : phases) {
					set(sheet, 5, x, phase.toString(), (Style.S_TH));				
					if(first) {
						set(sheet, 6, x, "new", (Style.S_TH));
					} else {
						set(sheet, 5, x+1, "", (Style.S_TH));				
						set(sheet, 5, x+2, "", (Style.S_TH));				
						set(sheet, 5, x+3, "", (Style.S_TH));				
						sheet.addMergedRegion(new CellRangeAddress(5, 5, x, x+2));
						set(sheet, 6, x, "old", (Style.S_TH));
						set(sheet, 6, x+1, "new", (Style.S_TH));
						set(sheet, 6, x+2, "nAnimals", (Style.S_TH));					
						set(sheet, 6, x+3, "Consumpt.", (Style.S_TH));					
					}
					
					if(first) {x++; first=false;}
					else {x+=4;}				
				}
			}
			
			//Create table data
			int y = 7;
			for (Container cage : cages) {
				Group gr = cage.getGroup();
				group2Lines.add(gr, y);
				StringBuilder animalIds = new StringBuilder();
				StringBuilder animalNos = new StringBuilder();
				for (Biosample b : cage.getBiosamples()) {
					animalIds.append((animalIds.length()>0? ",\n": "") + b.getSampleId());
					animalNos.append((animalNos.length()>0? ",\n": "") + (b.getSampleName()==null || b.getSampleName().length()==0?"??": b.getSampleName()));
				}
				set(sheet, y, 0, gr==null? "N/A": gr.getBlindedName(Spirit.getUsername()) , (Style.S_TD_BOLD));
				set(sheet, y, 1, animalIds.toString() , (Style.S_TD_LEFT));
				set(sheet, y, 2, animalNos.toString() , (Style.S_TD_LEFT));
				set(sheet, y, 3, cage.getContainerId() , (Style.S_TD_BOLD));

				//data: Loop through phases
				int x = 4;
				boolean first = true;
				for (Phase phase : phases) {
					FoodWater fw = FoodWater.extract(fws, cage, phase);
					if(fw==null) {
						if(first) {
							set(sheet, y, x, "", (Style.S_TD_DOUBLE2));
						} else {
							set(sheet, y, x, "", (Style.S_TD_DOUBLE2));
							set(sheet, y, x+1, "", (Style.S_TD_DOUBLE2));
							set(sheet, y, x+2, "", (Style.S_TD_BOLD));
							set(sheet, y, x+3, "", (Style.S_TD_BOLD));
						}
					} else {
						Consumption foodConsumption = fw.calculatePrevConsumptionFromList(fws, false);
						Consumption waterConsumption = fw.calculatePrevConsumptionFromList(fws, true);
						
						if(first) {
							set(sheet, y, x, i==0? fw.getFoodTare(): fw.getWaterTare(), (Style.S_TD_DOUBLE2));
						} else {
							set(sheet, y, x, i==0? fw.getFoodWeight(): fw.getWaterWeight(), (Style.S_TD_DOUBLE2));
							set(sheet, y, x+1, i==0? fw.getFoodTare(): fw.getWaterTare(), (Style.S_TD_DOUBLE2));
							set(sheet, y, x+2, fw.getNAnimals(), (Style.S_TD_DOUBLE0));
							
							set(sheet, y, x+3, i==0? (foodConsumption==null? null: foodConsumption.value): (waterConsumption==null? null: waterConsumption.value), (Style.S_TD_DOUBLE2));
						}
					}
					
					if(first) {x++; first=false;}
					else {x+=4;}				
				}
				y++;
			}
			
			y++;
			set(sheet, y, 0, "Averages" , (Style.S_TITLE12));
			//Create Mean table per treatment
			y++;
			for (Group gr : study.getGroups()) {
				set(sheet, y, 0, gr==null? "N/A": gr.getBlindedName(Spirit.getUsername()) , (Style.S_TD_BOLD));
				set(sheet, y, 1, "" , (Style.S_TD_LEFT));
				set(sheet, y, 2, "" , (Style.S_TD_LEFT));
				set(sheet, y, 3, "" , (Style.S_TD_BOLD));

				//data: Loop through phases
				int x = 4;
				boolean first = true;
				for(int j=0; j<phases.size(); j++) {

					List<Integer> lines = group2Lines.get(gr);
					if(first) {
						set(sheet, y, x, "", Style.S_TD_DOUBLE2);
						x++;
						first = false;
					} else {
						set(sheet, y, x, "", Style.S_TD_DOUBLE2);
						set(sheet, y, x+1, "", Style.S_TD_DOUBLE2);
						set(sheet, y, x+2, "", Style.S_TD_DOUBLE2);
						if(lines!=null && lines.size()>0) {
							setFormula(sheet, y, x+2, "SUM("+convertLinesToCells(lines, x+2)+")", Style.S_TD_DOUBLE0);
							StringBuilder sb = new StringBuilder();
							for (Integer line : lines) {
								if(sb.length()>0) sb.append("+");		
								sb.append("N(" + convertToCell(line, x+3) + ")*N(" + convertToCell(line, x+2) + ")");
							}
							setFormula(sheet, y, x+3, "IF(" + convertToCell(y, x+2) + ">0, ("+sb+")/"+convertToCell(y, x+2) + ", \"\")", (Style.S_TD_DOUBLE2));
						}
						x+=4;
					}
					
				}
				y++;
			}
			
			
			POIUtils.autoSizeColumns(sheet);
		}
	}
	*/

}
