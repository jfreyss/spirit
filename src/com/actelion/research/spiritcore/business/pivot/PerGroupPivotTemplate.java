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

/**
 * Excel like representation
 * @author freyssj
 *
 */
public class PerGroupPivotTemplate extends PivotTemplate {

	public PerGroupPivotTemplate() {
		super("PerGroup", "column.png");
	}		

	public PerGroupPivotTemplate(String name) {
		super(name, "column.png");
	}		

	public PerGroupPivotTemplate(String name, String thumbnailName) {
		super(name, thumbnailName);
	}		

	@Override
	public void init(List<Result> results) {
		clear();
			
		setWhere(PivotItemFactory.STUDY_STUDYID, Where.ASCOL);
		setWhere(PivotItemFactory.STUDY_GROUP, Where.ASCOL);
		setWhere(PivotItemFactory.STUDY_SUBGROUP, Where.ASCELL);
		setWhere(PivotItemFactory.BIOSAMPLE_TOPID, Where.ASCELL);
		setWhere(PivotItemFactory.BIOSAMPLE_TOPNAME, Where.ASCELL);
		setWhere(PivotItemFactory.BIOSAMPLE_NAME, Where.ASROW);
		setWhere(PivotItemFactory.RESULT_TEST, Where.ASROW);
		setWhere(PivotItemFactory.RESULT_OUTPUT, Where.ASROW);
		
		if(isDiscriminating(PivotItemFactory.BIOSAMPLE_METADATA, results)) {
			setWhere(PivotItemFactory.BIOSAMPLE_METADATA, Where.ASCELL);
		}
		if(isDiscriminating(PivotItemFactory.STUDY_PHASE_DATE, results)) {
			setWhere(PivotItemFactory.STUDY_PHASE_DATE, Where.ASROW);
		} else {
			setWhere(PivotItemFactory.STUDY_PHASE_DATE, Where.ASCELL);
		}
		setWhere(PivotItemFactory.RESULT_INPUT, Where.ASROW);

		if(isDiscriminating(PivotItemFactory.BIOSAMPLE_COMMENTS, results)) {
			setWhere(PivotItemFactory.BIOSAMPLE_COMMENTS, Where.ASROW);
		}
		
		simplify(results);
		
	}
	
	

}
