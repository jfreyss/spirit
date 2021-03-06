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

package com.actelion.research.spiritapp.ui.biosample.editor;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.study.PhaseLabel;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.JTextComboBox;


/**
 * Table CellEditor to select a phase from a study.
 * The developer must develop the getStudy function
 *
 * @author Joel Freyss
 */
public abstract class PhaseCellEditor extends AbstractCellEditor implements TableCellEditor {

	private boolean allowTyping;
	private Study study;
	private final List<Phase> phases = new ArrayList<>();
	private final JTextComboBox cb = new JTextComboBox() {
		PhaseLabel gl = new PhaseLabel();
		@Override
		public Component processCellRenderer(JLabel comp, String value, int index) {
			gl.setPhase(index>=1 && index-1<phases.size()? phases.get(index-1): null);
			return gl;
		}
	};

	public PhaseCellEditor(boolean allowTyping) {
		this.allowTyping = allowTyping;
		cb.setAllowTyping(allowTyping);
		cb.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
	}

	public abstract Study getStudy(int row);

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		this.study = getStudy(row);
		phases.clear();
		List<String> choices = new ArrayList<>();
		if(study!=null) {
			phases.addAll(study.getPhases());
			for (Phase p : phases) {
				choices.add(p.getName());
			}
		}
		cb.setChoices(choices);
		cb.setText(value==null?"": ((Phase)value).getName());
		cb.selectAll();
		return cb;
	}

	@Override
	public Phase getCellEditorValue() {
		if(cb.getText().length()==0) return null;

		for (Phase p : phases) {
			if(p.getName().equals(cb.getText())) return p;
		}
		if(allowTyping) {
			Phase p = new Phase(cb.getText());
			p.setStudy(study);
			return p;
		}
		return null;
	}

}