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

package com.actelion.research.spiritapp.spirit.ui.result.edit;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdBrowser;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;

public class BiosampleIdCellEditor extends AbstractCellEditor implements TableCellEditor {
	private final SampleIdBrowser sampleIdBrowser;

	public BiosampleIdCellEditor(Biotype forcedBiotype) {
		sampleIdBrowser = new SampleIdBrowser(forcedBiotype);
		sampleIdBrowser.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
	}

	@Override
	public String getCellEditorValue() {
		return sampleIdBrowser.getBiosample()==null?"": sampleIdBrowser.getBiosample().getSampleId();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Biosample biosample = value==null || value.toString().length()==0? null: DAOBiosample.getBiosample((String) value);
		sampleIdBrowser.setBiosample(biosample);
		sampleIdBrowser.selectAll();
		return sampleIdBrowser;
	}

}