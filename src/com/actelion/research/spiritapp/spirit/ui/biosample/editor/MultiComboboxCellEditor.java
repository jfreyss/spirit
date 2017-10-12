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

package com.actelion.research.spiritapp.spirit.ui.biosample.editor;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.util.ui.JComboCheckBox;

/**
 * ComboboxCellEditor
 * @author freyssj
 */
public class MultiComboboxCellEditor extends AbstractCellEditor implements TableCellEditor {
	private JComboCheckBox cb = new JComboCheckBox();
	//	private JTextComboBox cb = new JTextComboBox();
	private final List<String> choices = new ArrayList<String>();

	public MultiComboboxCellEditor(String csvChoices) {

		for(String s: BiotypeMetadata.splitChoices(csvChoices)) {
			choices.add(s);
		}
		Collections.sort(choices);
		cb.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		cb.setAllowTyping(false);
		cb.setMargin(null);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		cb.setChoices(choices);
		final String v = value==null?"": value.toString();
		cb.setText(v);
		cb.selectAll();
		return cb;
	}

	@Override
	public String getCellEditorValue() {
		String s =  cb.getText();
		return s;
	}
}