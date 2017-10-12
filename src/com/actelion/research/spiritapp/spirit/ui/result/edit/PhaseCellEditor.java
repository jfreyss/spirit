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

package com.actelion.research.spiritapp.spirit.ui.result.edit;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.JTextComboBox;

/**
 * Phase Editor (value instanceof Phase)
 * @author freyssj
 *
 */
public class PhaseCellEditor extends AbstractCellEditor implements TableCellEditor {

	private JTextComboBox cb = new JTextComboBox(false);
	private Study study;

	public PhaseCellEditor() {
		cb.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Result r = ((EditResultTable) table).getRows().get(row);
		this.study = r.getBiosample()==null?null:r.getBiosample().getInheritedStudy();
		List<String> choices = new ArrayList<String>();
		if(study!=null) {
			for (Phase phase : study.getPhases()) {
				choices.add(phase.getShortName());
			}
		}
		cb.setChoices(choices);

		//cb.getEditor().setItem(value);
		cb.setText(value==null? "": value.toString());
		return cb;
	}

	@Override
	public Object getCellEditorValue() {
		return study==null? null: study.getPhase(cb.getText());
	}
}