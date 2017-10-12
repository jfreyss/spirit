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
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleTableModel;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.GroupCellEditor;
import com.actelion.research.spiritapp.spirit.ui.study.GroupLabel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class StudyGroupColumn extends Column<Biosample, Group> {

	private GroupLabel groupLabel = new GroupLabel();
	private EditBiosampleTableModel model;

	/**
	 * Create a column to display the study group.
	 * If getStudy is not overriden, the group will not be editable
	 */
	public StudyGroupColumn() {
		super("Study\nGroup", Group.class, 60, 140);
	}

	/**
	 * Create a column to display/edit the study group.
	 */
	public StudyGroupColumn(EditBiosampleTableModel editTableModel) {
		this();
		this.model = editTableModel;
	}

	@Override
	public float getSortingKey() {return 3.2f;}

	@Override
	public Group getValue(Biosample row) {
		if(SpiritRights.isBlindAll(row.getInheritedStudy(), SpiritFrame.getUser())) return null;
		return row.getInheritedGroup();
	}


	@Override
	public String getToolTipText() {return "Study Group";}

	@Override
	public void setValue(Biosample row, Group value) {
		row.setAttached(row.getInheritedStudy(), value, row.getInheritedSubGroup());
	}

	@Override
	public void paste(Biosample row, String value) throws Exception {

		if(value==null || value.length()==0) {
			setValue(row, null);
			return;
		}
		Study study = row.getInheritedStudy();
		if(study==null) throw new Exception("You must select a study to enter a group");

		Group group = study.getGroup(value);
		if( group==null) {
			group = new Group(value);
			group.setStudy(study);
		}
		setValue(row, group);
	}

	@Override
	public boolean isEditable(Biosample row) {
		return row!=null && row.getAttachedStudy()!=null && (row.getParent()==null || row.getParent().getInheritedStudy()==null);
	}


	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		groupLabel.setGroup((Group)value);
		return groupLabel;
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		GroupCellEditor res = new GroupCellEditor(true) {
			@Override
			public Study getStudy(int row) {
				return model!=null && model.getRow(row)!=null? model.getRow(row).getInheritedStudy(): null;
			}
		};
		return res;
	}


}