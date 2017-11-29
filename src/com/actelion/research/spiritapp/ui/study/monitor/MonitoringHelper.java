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

package com.actelion.research.spiritapp.ui.study.monitor;

import java.util.Collection;
import java.util.List;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.FoodWater;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.util.ui.iconbutton.IconType;

public class MonitoringHelper {

	public static class MonitoringStats {
		int nRequired;
		int nDone;
		int nMade;
		public MonitoringStats(int nRequired, int nDone, int nMade) {
			this.nRequired = nRequired;
			this.nDone = nDone;
			this.nMade = nMade;
		}
		public IconType getIconType() {
			return nRequired==0? IconType.EMPTY: nRequired==nDone? IconType.GREEN_FLAG: nDone>0? IconType.ORANGE_FLAG: IconType.RED_FLAG;
		}
		public String getText(String title) {
			if(nMade==0 && nRequired==0) return "";
			
			return nMade + " " + title + (nRequired==0?" ": " (<span style='font-size:90%'><b style='color:"+(nDone<nRequired?"#FF0000":"#009900")+"'>"+nDone+"</b>/"+nRequired+" required</span>)");
		}
		
		public static IconType getIconType(List<MonitoringStats> stats) {
			boolean hasRequired = false;
			boolean allDone = true;
			
			for (MonitoringStats s : stats) {
				if(s.nRequired>0) {
					hasRequired = true;
					if(s.nDone<s.nRequired) allDone = false;
				}
			}
			return !hasRequired? IconType.EMPTY: allDone? IconType.GREEN_FLAG: IconType.RED_FLAG;
		}
		
	}
	
	public static MonitoringStats calculateDoneRequiredFW(Collection<Container> cages, Phase phase, List<FoodWater> phaseFws) {

		int nRequired = 0;
		int nDone = 0;
		int nMade = 0;
		
		for (Container cage : cages) {
			if(cage.getContainerId()==null) continue;
			boolean foodRequired = false;
			boolean waterRequired = false;
			for(Biosample animal: cage.getBiosamples()) {
				if(!animal.getStatus().isAvailable()) continue;
				StudyAction a = animal.getStudyAction(phase);
				if(a!=null && a.isMeasureFood()) foodRequired = true;
				if(a!=null && a.isMeasureWater()) waterRequired = true;
			}
			FoodWater fw = FoodWater.extract(phaseFws, cage.getContainerId(), phase);
			boolean made = fw!=null && (fw.getFoodWeight()!=null || fw.getWaterWeight()!=null || fw.getFoodTare()!=null || fw.getWaterTare()!=null);
			if(made) nMade++;
			if(foodRequired || waterRequired) {
				nRequired++;
				if(made) nDone++;
			}			
		}
		return new MonitoringStats(nRequired, nDone, nMade);
	}
	
	public static MonitoringStats calculateDoneRequiredTest(Collection<Biosample> animals, Phase phase, Test test, List<Result> results) {
		
		if(test==null) return new MonitoringStats(0, 0, 0);
		int nRequired = 0;
		int nDone = 0;
		int nMade = 0;
	
		ListHashMap<String, Result> resultsMap = new ListHashMap<>();
		for (Result r : results) resultsMap.add(r.getTest().getId() + "_" + r.getBiosample().getId() + "_"+ r.getPhase(), r);
				
		for (Biosample animal : animals) {
			if(!animal.getStatus().isAvailable()) continue;
			//required?
			boolean required = false;
			StudyAction a = animal.getStudyAction(phase);
			if(a!=null) {
				if(DAOTest.WEIGHING_TESTNAME.equals(test.getName())) {
					required = a.isMeasureWeight() || (a.getNamedTreatment()!=null && a.getNamedTreatment().isWeightDependant());							
				} else if(DAOTest.FOODWATER_TESTNAME.equals(test.getName())) {
					required = a.isMeasureWater() || a.isMeasureFood();							
				} else {
					for(Measurement em: a.getMeasurements()) {
						if(em.getTestId()==test.getId()) {
							required = true; break;
						}
					}
				}
			}
			
			//Loop through results			
			boolean made = false;
			List<Result> list = resultsMap.get(test.getId() + "_" + animal.getId() + "_" + phase);
			if(list!=null) {
				for (Result r : list) {				
					if(r.getOutputResultValuesAsString().length()>0) {
						made = true;
						break;
					}
				}
			}		

			if(made) nMade++;				
			if(required) {
				nRequired++;
				if(made) nDone++;
			}

		}
		return new MonitoringStats(nRequired, nDone, nMade);
	}

}
