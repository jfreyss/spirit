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

package com.actelion.research.spiritapp.spirit.ui.biosample.column;

import java.text.DecimalFormat;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.exceltable.Column;

public class StudyIdColumn extends Column<Biosample, String> {

	public StudyIdColumn() {
		super("Study\nStudyId", String.class, 30, 60);
	}

	@Override
	public float getSortingKey() {return 3.01f;}

	@Override
	public String getValue(Biosample row) {
		return row.getInheritedStudy()==null?null: row.getInheritedStudy().getStudyId();
	}

	@Override
	public void setValue(Biosample row, String value) {
		try {
			int index = MiscUtils.getIndexFirstDigit(value);
			int id = Integer.parseInt(index>=0? value.substring(index): value);
			value = "S-" + new DecimalFormat("00000").format(id);
		} catch(Exception e) {
			//nothing
		}
		Study study = DAOStudy.getStudyByStudyId(value);
		System.out.println("StudyIdColumn.setValue() "+study);
		row.setAttachedStudy(study);
	}

	@Override
	public boolean isEditable(Biosample row) {
		return row!=null && (row.getParent()==null || row.getParent().getInheritedStudy()==null);
	}

}