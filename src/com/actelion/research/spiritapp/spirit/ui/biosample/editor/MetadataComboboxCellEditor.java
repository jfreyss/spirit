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
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.exceltable.AlphaNumericalCellEditor;

/**
 * ComboboxCellEditor
 * @author freyssj
 */
public class MetadataComboboxCellEditor extends AbstractCellEditor implements TableCellEditor {

	private JTextComboBox textComboBox = new JTextComboBox(false) {
		@Override
		public List<String> getChoices() {
			return choices;				
		}
	};
	
	private final List<String> choices;
	
	public MetadataComboboxCellEditor(String csvChoices) {
		choices = BiotypeMetadata.splitChoices(csvChoices);
	}
	
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		AlphaNumericalCellEditor.initComp(textComboBox, (JComponent) table.getCellRenderer(row, column).getTableCellRendererComponent(table, value, isSelected, isSelected, row, column));
		textComboBox.setText(value==null?"": value.toString());
		textComboBox.selectAll();
		return textComboBox;
	}

	@Override
	public String getCellEditorValue() {
		return (String) textComboBox.getText();
	}				
}