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

package com.actelion.research.spiritapp.ui.biosample.column;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.ui.study.GroupLabel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class StudyTreatmentColumn extends Column<Biosample, String> {

	private static GroupLabel groupLabel = new GroupLabel();

	/**
	 * Create a column to display the study group.
	 * If getStudy is not overriden, the group will not be editable
	 */
	public StudyTreatmentColumn() {
		super("Study\nTreatment", String.class, 60, 140);
	}

	@Override
	public float getSortingKey() {return 3.35f;}

	@Override
	public String getValue(Biosample row) {
		if(row.getInheritedStudy()==null) return null;
		if(row.getInheritedStudy().isBlind()) return null;
		return row.getInheritedGroup().getTreatmentDescription(row.getInheritedSubGroup());
	}


	@Override
	public boolean isEditable(Biosample row) {
		return false;
	}


	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		groupLabel.setText((String) value, row.getInheritedGroup());
		return groupLabel;
	}

	@Override
	public boolean isHideable() {
		return true;
	}
}