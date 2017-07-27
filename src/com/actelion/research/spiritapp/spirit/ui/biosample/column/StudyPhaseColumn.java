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

import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleTableModel;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.PhaseCellEditor;
import com.actelion.research.spiritapp.spirit.ui.study.PhaseLabel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class StudyPhaseColumn extends Column<Biosample, Phase> {

	private PhaseLabel phaseLabel = new PhaseLabel();
	private EditBiosampleTableModel model;

	public StudyPhaseColumn() {
		super("Study\nPhase", Phase.class, 20);
	}

	/**
	 * Create a column to display/edit the study group.
	 */
	public StudyPhaseColumn(EditBiosampleTableModel editTableModel) {
		this();
		this.model = editTableModel;
	}
	@Override
	public float getSortingKey() {return 3.4f;}

	@Override
	public Phase getValue(Biosample row) {
		return row.getInheritedPhase();
	}

	@Override
	public void setValue(Biosample row, Phase value) {
		row.setInheritedPhase(value);
	}

	@Override
	public void paste(Biosample row, String value) throws Exception {
		if(value==null || value.length()==0) {setValue(row, null); return; }

		if(row.getInheritedStudy()==null) throw new Exception("You must select a study to enter a phase");
		Phase phase = row.getInheritedStudy().getPhase(value);
		if(phase==null) throw new Exception("The phase " +row.getInheritedStudy().getStudyId() + " / " + value + " is invalid");
		setValue(row, phase);
	}

	@Override
	public boolean isEditable(Biosample row) {
		return row!=null && row.getInheritedStudy()!=null;// && row.getParent()!=null && row.getParent().getInheritedPhase()==null;
	}


	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		phaseLabel.setPhase(row==null? null: row.getInheritedPhase(), row==null? null: row.getInheritedGroup());
		phaseLabel.setToolTipText(row==null || row.getInheritedPhase()==null? null: row.getInheritedPhase().getName());
		return phaseLabel;
	}




	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		return new PhaseCellEditor(true) {
			@Override
			public Study getStudy(int row) {
				return model!=null && model.getRow(row)!=null? model.getRow(row).getInheritedStudy(): null;
			}
		};
	}
}