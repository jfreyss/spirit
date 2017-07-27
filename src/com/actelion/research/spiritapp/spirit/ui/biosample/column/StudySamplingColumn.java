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

import javax.swing.JComponent;

import com.actelion.research.spiritapp.spirit.ui.study.GroupLabel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class StudySamplingColumn extends Column<Biosample, NamedSampling> {

	private static GroupLabel groupLabel = new GroupLabel();


	public StudySamplingColumn() {
		super("Study\nSampling", NamedSampling.class, 60);
	}
	@Override
	public float getSortingKey() {return 3.36f;}


	@Override
	public NamedSampling getValue(Biosample row) {
		return row==null || row.getAttachedSampling()==null? null: row.getAttachedSampling().getNamedSampling();
	}

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		NamedSampling ns = getValue(row);
		groupLabel.setText(ns==null?"": ns.getName(), row.getInheritedGroup());
		return groupLabel;
	}

	@Override
	public boolean isMultiline() {
		return false;
	}

	@Override
	public boolean isHideable() {
		return true;
	}

}
