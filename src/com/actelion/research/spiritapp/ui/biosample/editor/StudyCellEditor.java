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
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JTextComboBox;


/**
 * Table CellEditor to select a phase from a study.
 * The developer must develop the getStudy function
 *
 * @author Joel Freyss
 */
public class StudyCellEditor extends AbstractCellEditor implements TableCellEditor {

	private final JTextComboBox cb = new JTextComboBox();

	public StudyCellEditor() {
		cb.setAllowTyping(true);
		cb.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		List<String> choices = new ArrayList<>();

		for (Study study : DAOStudy.getRecentStudies(Spirit.getUser(), RightLevel.WRITE)) {
			choices.add(study.getStudyId());
		}
		cb.setChoices(choices);
		cb.setText(value==null?"": value.toString());
		cb.selectAll();
		return cb;
	}

	@Override
	public String getCellEditorValue() {
		return cb.getText();
	}

}