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

import java.awt.Color;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;

import com.actelion.research.util.ui.JCustomTextField;

/**
 * AlphaNumericalCellEditor
 * @author freyssj
 *
 */
public class AlphaNumericalCellEditor extends AbstractCellEditor implements TableCellEditor {
	private JCustomTextField alphaTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC);
	
	public AlphaNumericalCellEditor() {
	}
	
	@Override
	public JCustomTextField getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {		
		initComp(alphaTextField, (JComponent) table.getCellRenderer(row, column).getTableCellRendererComponent(table, value, isSelected, isSelected, row, column));
		
		alphaTextField.setText(value==null?"": value.toString());
		alphaTextField.selectAll();
		return alphaTextField;
	}

	@Override
	public String getCellEditorValue() {
		return alphaTextField.getText();
	}						
	
	public static void initComp(JComponent comp, JComponent model) {		
		if(comp instanceof JCustomTextField) {
			if(comp instanceof JLabelNoRepaint) {
				((JCustomTextField)comp).setHorizontalAlignment(((JLabelNoRepaint)comp).getHorizontalAlignment());
			} else {
				((JCustomTextField)comp).setHorizontalAlignment(SwingConstants.LEFT);
			}
			((JCustomTextField)comp).setMargin(null);
		}
		
		comp.setFont(comp.getFont());
		comp.setBorder(BorderFactory.createMatteBorder(1,1,1,1, Color.BLUE));
		
	}
}