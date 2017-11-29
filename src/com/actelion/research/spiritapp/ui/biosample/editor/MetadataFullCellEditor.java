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

package com.actelion.research.spiritapp.ui.biosample.editor;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleTable;
import com.actelion.research.spiritcore.business.biosample.Biosample;

public class MetadataFullCellEditor extends AbstractCellEditor implements TableCellEditor {

	private final EditBiosampleTable table;
	private Biosample ref = null;
	private JTextField lbl = new JTextField();

	public MetadataFullCellEditor(final EditBiosampleTable table) {
		this.table = table;
		lbl.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		lbl.addCaretListener(e-> {
			if(ref!=null) {
				if(table.isShowing() && table.isEditing()) {
					final int rowNo = table.getModel().getRows().indexOf(ref);
					final int colNo = table.getSelectedColumn();
					stopCellEditing();

					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							Biosample oldObj = ref.clone();
							new MetadataFullDlg(table, lbl, ref);

							table.setRowSelectionInterval(rowNo, rowNo);
							table.setColumnSelectionInterval(colNo, colNo);

							Biosample newObj = ref.clone();
							table.getUndoManager().addOfflineRowChange(rowNo, oldObj, newObj);
							table.repaint();
						}
					});
				}
			}
		});
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, final int row, int column) {
		lbl.setText(value==null?"": value.toString());
		ref = row<this.table.getModel().getRows().size()? this.table.getModel().getRows().get(row): null;
		return lbl;
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

}