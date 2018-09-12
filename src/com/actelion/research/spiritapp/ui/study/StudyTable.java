/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.study;

import java.awt.Color;
import java.util.List;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.ui.util.component.SpiritExtendTable;
import com.actelion.research.spiritcore.business.study.Study;

public class StudyTable extends SpiritExtendTable<Study> {

	public StudyTable() {
		super(new StudyTableModel());
		setBorderStrategy(BorderStrategy.ALL_BORDER);
	}

	@Override
	public void setRows(List<Study> data) {
		getModel().setRows(data);
		getModel().initColumns();
		getModel().removeEmptyColumns();
		resetPreferredColumnWidth();
	}

	@Override
	public StudyTableModel getModel() {
		return (StudyTableModel) super.getModel();
	}

	@Override
	public void postProcess(Study row, int rowNo, Object value, JComponent c) {
		super.postProcess(row, rowNo, value, c);
		if("Archived".equals(row.getState())) {
			c.setForeground(Color.GRAY);
		}
	}
}
