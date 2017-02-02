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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

import com.actelion.research.util.ui.JGenericComboBox;

/**
 * ComboboxCellEditor
 * @author freyssj
 */
public class ComboboxCellEditor<T> extends AbstractCellEditor implements TableCellEditor {
	private JGenericComboBox<T> cb;
	
	public ComboboxCellEditor(final JGenericComboBox<T> cb) {
		
		this.cb = cb;		
		for(Component c: cb.getComponents()) {
			c.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					cb.showPopup();
				}
			});				
		}
		cb.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				fireEditingStopped();
			}
		});
		cb.setBorder(BorderFactory.createMatteBorder(1,1,1,1, Color.BLUE));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		cb.setSelection((T)value);
		cb.setEditable(false);
		if(value==null && cb.isShowing()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					cb.showPopup();
				}
			});
		}
		
		return cb;
	}

	@Override
	public T getCellEditorValue() {
		return cb.getSelection();
	}				
}