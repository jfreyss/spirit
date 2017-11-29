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

import com.actelion.research.spiritapp.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class TopIdColumn extends Column<Result, Biosample> {

	private SampleIdLabel sampleIdLabel = new SampleIdLabel();

	public TopIdColumn() {
		super("ParticipantId", Biosample.class);
	}

	@Override
	public float getSortingKey() {
		return 2.6f;
	}

	@Override
	public Biosample getValue(Result row) {
		if(row.getBiosample()==null) return null;
		Biosample top = row.getBiosample().getTopParent();
		return top;
	}
	@Override
	public boolean isEditable(Result row) {return false;}

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
		sampleIdLabel.setBiosample((Biosample)value);
		return sampleIdLabel;
	}
}
