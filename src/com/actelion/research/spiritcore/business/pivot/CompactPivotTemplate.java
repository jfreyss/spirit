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

		if(isDiscriminating(PivotItemFactory.BIOSAMPLE_BIOTYPE, results)) {
			setWhere(PivotItemFactory.BIOSAMPLE_BIOTYPE, Where.ASCOL);
		}
		
		if(isDiscriminating(PivotItemFactory.RESULT_TEST, results)) {
			setWhere(PivotItemFactory.RESULT_TEST, Where.ASCOL);
		}
		
		if(isDiscriminating(PivotItemFactory.STUDY_PHASE_DATE, results)) {
			setWhere(PivotItemFactory.STUDY_PHASE_DATE, getPivotItems(Where.ASCOL).size()<=1? Where.ASCOL: Where.ASCELL);			
		}

		if(isDiscriminating(PivotItemFactory.BIOSAMPLE_SAMPLING, results)) {
			setWhere(PivotItemFactory.BIOSAMPLE_SAMPLING, getPivotItems(Where.ASCOL).size()<=1? Where.ASCOL: Where.ASCELL);
		} else if(isDiscriminating(PivotItemFactory.BIOSAMPLE_SAMPLENAME, results)) {
			setWhere(PivotItemFactory.BIOSAMPLE_SAMPLENAME, getPivotItems(Where.ASCOL).size()<=1? Where.ASCOL: Where.ASCELL);
		}
		
		setWhere(PivotItemFactory.RESULT_INPUT, getPivotItems(Where.ASCOL).size()<=1? Where.ASCOL: Where.ASCELL);
		setWhere(PivotItemFactory.RESULT_TEST, Where.ASCOL);

		

		if(isDiscriminating(PivotItemFactory.RESULT_COMMENTS, results)) {
			setWhere(PivotItemFactory.RESULT_COMMENTS, Where.ASCELL);
		}
		
		
		
		
	}

}
