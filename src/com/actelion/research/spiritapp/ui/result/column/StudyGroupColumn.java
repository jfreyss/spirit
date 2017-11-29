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

package com.actelion.research.spiritapp.ui.result.column;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.ui.study.GroupLabel;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class StudyGroupColumn extends Column<Result, Group> {
	public StudyGroupColumn() {
		super("Group", Group.class);
	}

	@Override
	public float getSortingKey() {
		return 2.1f;
	}

	@Override
	public Group getValue(Result row) {
		return row.getBiosample()==null? null: row.getBiosample().getInheritedGroup();
	}
	@Override
	public boolean isEditable(Result row) {return false;}

	private GroupLabel groupLabel = new GroupLabel();

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
		groupLabel.setGroup((Group)value);
		return groupLabel;
	}
}
