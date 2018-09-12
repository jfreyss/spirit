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

package com.actelion.research.spiritapp.ui.biosample.column;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.study.GroupLabel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class StudySubGroupColumn extends Column<Biosample, Integer> {
	private static GroupLabel groupLabel = new GroupLabel();

	public StudySubGroupColumn() {
		super("Study\nSubgroup", Integer.class, 15, 15);
		groupLabel.setFont(FastFont.SMALL);
	}

	@Override
	public float getSortingKey() {return 3.3f;}

	@Override
	public Integer getValue(Biosample row) {
		if(SpiritRights.isBlindAll(row.getInheritedStudy(), SpiritFrame.getUser())) return null;
		return row.getInheritedGroup()==null || row.getInheritedGroup().getNSubgroups()<=1? null: row.getInheritedSubGroup()+1;
	}

	@Override
	public void setValue(Biosample row, Integer value) {
		try {
			row.setAttached(row.getInheritedStudy(), row.getInheritedGroup(), value==null? 0: value-1);
		} catch(Exception e) {
			JExceptionDialog.showError(e);
		}
	}

	@Override
	public void paste(Biosample row, String value) throws Exception {
		setValue(row, Integer.parseInt(value));
	}

	@Override
	public boolean isEditable(Biosample row) {
		return row!=null && row.getInheritedGroup()!=null && row.getAttachedStudy()!=null && row.getInheritedGroup().getNSubgroups()>1;
	}


	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample b, int rowNo, Object value) {
		groupLabel.setText(value==null? "": "'"+value, b.getInheritedGroup());
		return groupLabel;
	}
}