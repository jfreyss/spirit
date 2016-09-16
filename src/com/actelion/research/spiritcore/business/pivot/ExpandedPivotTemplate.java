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

import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorConfig;
import com.actelion.research.spiritcore.business.pivot.datawarrior.PivotDataType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.SpiritUser;


public class ExpandedPivotTemplate extends PivotTemplate {

	public ExpandedPivotTemplate() {
		super("Expanded", "column.png");
	}		

	@Override
	public void init(List<Result> results) {
		clear();
		setWhere(PivotItemFactory.STUDY_SUBGROUP, Where.ASROW);
		setWhere(PivotItemFactory.BIOSAMPLE_TOPID, Where.ASROW);
		setWhere(PivotItemFactory.BIOSAMPLE_TOPNAME, Where.ASROW);

		setWhere(PivotItemFactory.STUDY_STUDYID, Where.ASCOL);
		setWhere(PivotItemFactory.STUDY_GROUP, Where.ASCOL);		
		setWhere(PivotItemFactory.BIOSAMPLE_SAMPLING, Where.ASCOL);
		setWhere(PivotItemFactory.RESULT_TEST, Where.ASCOL);
		setWhere(PivotItemFactory.RESULT_INPUT, Where.ASCOL);
		setWhere(PivotItemFactory.RESULT_OUTPUT, Where.ASCOL);
		setWhere(PivotItemFactory.STUDY_PHASE_DATE, Where.ASCOL);
	}
	
	public void init(List<Result> results, DataWarriorConfig config, SpiritUser user) {
		ExpandedPivotTemplate tpl = this;
		init(results);		
		tpl.removeBlindItems(results, user);

		if (config.isSet(PivotDataType.PHASE)) {
			for (PivotItem item : tpl.getPivotItems()) {
				if (item.getClassifier() == PivotItemClassifier.STUDY_PHASE && tpl.getWhere(item) == Where.ASCOL) {
					tpl.setWhere(item, Where.ASROW);
				}
			}
		}

		if (config.isSet(PivotDataType.BIOTYPE)) {
			for (PivotItem item : tpl.getPivotItems()) {
				if (item.getClassifier() == PivotItemClassifier.BIOSAMPLE && tpl.getWhere(item) == Where.ASCOL) {
					tpl.setWhere(item, Where.ASROW);
				}
			}
		}

		if (config.isSet(PivotDataType.INPUT)) {
			for (PivotItem item : tpl.getPivotItems()) {
				if (item == PivotItemFactory.RESULT_INPUT && tpl.getWhere(item) == Where.ASCOL) {
					tpl.setWhere(item, Where.ASROW);
				}
			}
		}

		if (config.isSet(PivotDataType.GROUP)) {
			for (PivotItem item : tpl.getPivotItems()) {
				if (item.getClassifier() == PivotItemClassifier.STUDY_GROUP && tpl.getWhere(item) == Where.ASCOL) {
					tpl.setWhere(item, Where.ASROW);
				}
			}
		}
				
		if (config.getComputed() != null) {
			tpl.setComputed(config.getComputed());
		}
		
		tpl.expand(results, user);
		tpl.simplify(results);
	}

}
