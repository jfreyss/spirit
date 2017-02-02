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

package com.actelion.research.spiritcore.business.pivot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;

/**
 * Excel like representation
 * @author freyssj
 *
 */
public class ColumnPivotTemplate extends PivotTemplate {

	public ColumnPivotTemplate() {
		super("Column", "column.png");
	}		

	public ColumnPivotTemplate(String name) {
		super(name, "column.png");
	}		

	@Override
	public void init(List<Result> results) {
		clear();
			
		setWhere(PivotItemFactory.STUDY_STUDYID, Where.ASROW);
		setWhere(PivotItemFactory.STUDY_GROUP, Where.ASROW);
		setWhere(PivotItemFactory.STUDY_SUBGROUP, Where.ASROW);
		setWhere(PivotItemFactory.BIOSAMPLE_TOPID, Where.ASROW);
		setWhere(PivotItemFactory.BIOSAMPLE_TOPNAME, Where.ASROW);
//		setWhere(PivotItemFactory.BIOSAMPLE_SAMPLING, Where.ASCOL);
		setWhere(PivotItemFactory.RESULT_TEST, Where.ASCOL);
		setWhere(PivotItemFactory.RESULT_OUTPUT, Where.ASCOL);
		
		if(isDiscriminating(PivotItemFactory.BIOSAMPLE_NAME, results)) {
			setWhere(PivotItemFactory.BIOSAMPLE_NAME, Where.ASCOL);
		}
			
		//We display the phase in the cell only if we don't create nested tables (otherwise in cols)
		//We display the biomarker in the cell only if we don't create nested tables (otherwise in cols)
		boolean phaseOnTop = false;
		boolean markerOnTop = false;
		Set<String> keysWithAll = new HashSet<String>();
		Set<String> keysWithPhase = new HashSet<String>();
		Set<String> keysWithBioMarker = new HashSet<String>();		
		StringBuilder key = new StringBuilder();
		List<PivotItem> pvs = new ArrayList<PivotItem>();
		pvs.addAll(getPivotItems(Where.ASROW));
		pvs.addAll(getPivotItems(Where.ASCOL));
				
		checkAllResults: for (Result r : results) {
			for(ResultValue rv: r.getOutputResultValues()) {
				key.setLength(0);
				for(PivotItem pv: pvs) {
					key.append(pv.getTitle(rv) + "|");
				}
				String key1 = key + "|" + PivotItemFactory.STUDY_PHASE_DATE.getTitle(rv);
				String key2 = key + "|" + PivotItemFactory.RESULT_INPUT.getTitle(rv);
				String key3 = key1 + "|" + PivotItemFactory.RESULT_INPUT.getTitle(rv);
				if(keysWithAll.contains(key3)) {//always a doublon
					continue;
				} else {
					keysWithAll.add(key3);
				}
				
				if(keysWithPhase.contains(key1)) {
					phaseOnTop = true;
					if(phaseOnTop && markerOnTop) break checkAllResults;
				} else {
					keysWithPhase.add(key1);
				}
				if(keysWithBioMarker.contains(key2)) {
					markerOnTop = true;
					if(phaseOnTop && markerOnTop) break checkAllResults;
				} else {
					keysWithBioMarker.add(key2);
				}
			}
		}
		
		if(isDiscriminating(PivotItemFactory.BIOSAMPLE_METADATA, results)) {
			setWhere(PivotItemFactory.BIOSAMPLE_METADATA, Where.ASCOL);
		}
		if(isDiscriminating(PivotItemFactory.STUDY_PHASE_DATE, results)) {
			setWhere(PivotItemFactory.STUDY_PHASE_DATE, Where.ASCOL);
			setWhere(PivotItemFactory.RESULT_INPUT, isDiscriminating(PivotItemFactory.RESULT_INPUT, results)? Where.ASCOL: Where.ASCELL);

		} else {
			setWhere(PivotItemFactory.RESULT_INPUT, Where.ASCOL);
			setWhere(PivotItemFactory.STUDY_PHASE_DATE, Where.ASCELL);
		}

		if(isDiscriminating(PivotItemFactory.BIOSAMPLE_COMMENTS, results)) {
			setWhere(PivotItemFactory.BIOSAMPLE_COMMENTS, Where.ASCOL);
		}
		
//		simplify(results);
		
	}
	
	

}
