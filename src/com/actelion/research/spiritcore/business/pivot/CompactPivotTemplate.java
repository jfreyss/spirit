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

import java.util.List;

import com.actelion.research.spiritcore.business.result.Result;


public class CompactPivotTemplate extends PivotTemplate {

	public CompactPivotTemplate() {
		super("Compact", "compact.png");
	}
	
	@Override
	public void init(List<Result> results) {
		clear();
		setWhere(PivotItemFactory.STUDY_STUDYID, Where.ASROW);
		setWhere(PivotItemFactory.STUDY_GROUP, Where.ASROW);
		setWhere(PivotItemFactory.STUDY_SUBGROUP, Where.ASROW);
		setWhere(PivotItemFactory.BIOSAMPLE_TOPID, Where.ASROW);
		setWhere(PivotItemFactory.BIOSAMPLE_TOPNAME, Where.ASROW);

		boolean isDiscriminating1 = isDiscriminating(PivotItemFactory.BIOSAMPLE_BIOTYPE, results);
		if(isDiscriminating1) {
			setWhere(PivotItemFactory.BIOSAMPLE_BIOTYPE, Where.ASCOL);
		}
		
		boolean isDiscriminating2 = isDiscriminating(PivotItemFactory.RESULT_TEST, results);
		if(isDiscriminating2) {
			setWhere(PivotItemFactory.RESULT_TEST, Where.ASCOL);
		}
		
		setWhere(PivotItemFactory.RESULT_INPUT, Where.ASCELL);

		if(isDiscriminating(PivotItemFactory.RESULT_OUTPUT, results)) {
			setWhere(PivotItemFactory.RESULT_OUTPUT, Where.ASCELL);
		}
		
		boolean isDiscriminating3 = isDiscriminating(PivotItemFactory.BIOSAMPLE_SAMPLING, results);
		boolean isDiscriminating4 = isPopulated(PivotItemFactory.BIOSAMPLE_SAMPLENAME, results);
		boolean isDiscriminating5 = isDiscriminating(PivotItemFactory.STUDY_PHASE_DATE, results);
		if(isDiscriminating3) {
			setWhere(PivotItemFactory.BIOSAMPLE_SAMPLING, isDiscriminating5? Where.ASCELL: Where.ASCOL);
		} else if(isDiscriminating4) {
			setWhere(PivotItemFactory.BIOSAMPLE_SAMPLENAME, isDiscriminating5? Where.ASCELL: Where.ASCOL);
		}
		
		System.out.println("CompactPivotTemplate.init() isPopulated(STUDY_PHASE_DATE)="+isPopulated(PivotItemFactory.STUDY_PHASE_DATE, results));
		if(isPopulated(PivotItemFactory.STUDY_PHASE_DATE, results)) {
			if(isDiscriminating1 || isDiscriminating2 || isDiscriminating3 || isDiscriminating4) {
				setWhere(PivotItemFactory.STUDY_PHASE_DATE, Where.ASCELL);
			} else {				
				setWhere(PivotItemFactory.STUDY_PHASE_DATE, Where.ASCOL);			
			}
		}

		if(isDiscriminating(PivotItemFactory.RESULT_COMMENTS, results)) {
			setWhere(PivotItemFactory.RESULT_COMMENTS, Where.ASCELL);
		}

		if(isDiscriminating(PivotItemFactory.BIOSAMPLE_COMMENTS, results)) {
			setWhere(PivotItemFactory.BIOSAMPLE_COMMENTS, Where.ASCELL);
		}
		
		
	}

}
