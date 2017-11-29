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
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.biosample.SampleIdGenerateField;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;

/**
 * SampleIdGenerateEditor
 *
 * Editor for a scanned or generated biosample (underlyingvalue is a sampleId as string).
 *
 * @author freyssj
 *
 */
public class SampleIdCellEditor extends AbstractCellEditor implements TableCellEditor {

	private SampleIdGenerateField<Biosample> scanTextField = new SampleIdGenerateField<>();

	public SampleIdCellEditor() {
		scanTextField.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
	}


	@SuppressWarnings("unchecked")
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Biosample b = row<table.getRowCount()? (Biosample) ((AbstractExtendTable<Biosample>) table).getRows().get(row): null;
		scanTextField.putCachedSampleId(b, b==null || b.getBiotype()==null? null: b.getBiotype().getPrefix(), b==null || b.getId()<=0? null: b.getSampleId());
		scanTextField.setText(b==null? "": b.getSampleId());
		scanTextField.setEditable(b!=null && b.getBiotype()!=null && !b.getBiotype().isHideSampleId());
		scanTextField.selectAll();
		return scanTextField;
	}

	@Override
	public Object getCellEditorValue() {
		return scanTextField.getSampleId();
	}

	public String generateSampleIdFor(Biosample row) {
		return scanTextField.generateSampleIdFor(row, row.getBiotype()==null?"INT": row.getBiotype().getPrefix());
	}
}