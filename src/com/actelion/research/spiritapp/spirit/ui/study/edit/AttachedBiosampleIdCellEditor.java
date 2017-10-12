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

package com.actelion.research.spiritapp.spirit.ui.study.edit;

import java.awt.Color;
import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdGenerateField;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;

/**
 * SampleIdGenerateEditor
 * Editor for a scanned or generated biosample (value=string)
 * @author freyssj
 *
 */
public class AttachedBiosampleIdCellEditor extends AbstractCellEditor implements TableCellEditor {

	private AttachedBiosampleTableModel model;
	private SampleIdGenerateField<AttachedBiosample> scanTextField;

	public AttachedBiosampleIdCellEditor(AttachedBiosampleTableModel model) {
		scanTextField = new SampleIdGenerateField<AttachedBiosample>();
		scanTextField.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		this.model = model;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		AttachedBiosample b = row<table.getRowCount()? (AttachedBiosample) ((AbstractExtendTable<AttachedBiosample>) table).getRows().get(row): null;
		Biotype biotype = model==null? null: model.getBiotype();
		scanTextField.putCachedSampleId(b,
				b==null || b.getBiosample()==null || b.getBiosample().getBiotype()==null? (biotype==null? null: biotype.getPrefix()): b.getBiosample().getBiotype().getPrefix(),
						b==null || b.getBiosample()==null || b.getBiosample().getId()<=0? null: b.getBiosample().getSampleId());
		scanTextField.setText(value==null?"": value.toString());
		scanTextField.selectAll();
		scanTextField.setBorder(BorderFactory.createLineBorder(Color.BLUE));
		return scanTextField;
	}

	@Override
	public Object getCellEditorValue() {
		return scanTextField.getSampleId();
	}
	public String generateSampleIdFor(AttachedBiosample row) {
		return scanTextField.generateSampleIdFor(row, row.getBiosample()==null || row.getBiosample().getBiotype()==null? "INT": row.getBiosample().getBiotype().getPrefix());
	}
}