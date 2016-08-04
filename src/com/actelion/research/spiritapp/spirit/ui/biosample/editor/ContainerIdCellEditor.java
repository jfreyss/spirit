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

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.container.ContainerTextField;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.ui.exceltable.ExcelTableModel;

public class ContainerIdCellEditor extends AbstractCellEditor implements TableCellEditor {
		
	private ContainerTextField containerTextField;
		
		public ContainerIdCellEditor() {
			containerTextField = new ContainerTextField();
			containerTextField.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));			
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			ExcelTableModel<Biosample> model = (ExcelTableModel<Biosample>) table.getModel();
			Biosample b = row<model.getRows().size()? model.getRows().get(row): null;
			containerTextField.setContainerType(b==null?null: b.getContainerType());
			containerTextField.setText(b==null?null: b.getContainerId());
			containerTextField.selectAll();
			return containerTextField;
		}

		@Override
		public String getCellEditorValue() {
			return containerTextField.getText();
		}		
	}