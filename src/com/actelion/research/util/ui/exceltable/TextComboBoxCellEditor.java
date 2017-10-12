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

package com.actelion.research.util.ui.exceltable;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.util.ui.JTextComboBox;

/**
 * TextComboBoxCellEditor
 * @author freyssj
 */
public class TextComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {

	private JTextComboBox cb;

	public TextComboBoxCellEditor(final JTextComboBox cb) {
		this.cb = cb;
		cb.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		cb.setText((String)value);
		cb.setCaretPosition(0);
		cb.selectAll();
		return cb;
	}

	@Override
	public String getCellEditorValue() {
		return cb.getText();
	}
}