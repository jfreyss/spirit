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

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.JTextComboBox;

public class ContainerCageCellEditor extends AbstractCellEditor implements TableCellEditor {
	private final JTextComboBox cageComboBox = new JTextComboBox();

	public ContainerCageCellEditor() {
		cageComboBox.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Biosample b = (Biosample) value;
		Study study = b.getInheritedStudy();
		List<String> cageNames = new ArrayList<String>();
		if(study!=null) {
			for (int i = 0; i < 20; i++) {
				cageNames.add(Container.suggestNameForCage(study, i+1));
			}
		}
		cageComboBox.setChoices(cageNames);
		cageComboBox.setText((String)value);
		return cageComboBox;
	}

	@Override
	public String getCellEditorValue() {
		return cageComboBox.getText();
	}
}