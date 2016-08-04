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

package com.actelion.research.spiritapp.spirit.ui.biosample.linker;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.biosample.editor.AutoCompletionCellEditor;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.AlphaNumericalCellEditor;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class SampleNameColumn extends AbstractLinkerColumn<String> {

	private JLabelNoRepaint lbl = new JLabelNoRepaint();
	
	protected SampleNameColumn(BiosampleLinker linker) {
		super(linker, String.class, 40, 120);		
		lbl.setFont(FastFont.BOLD);
	}

	@Override
	public String getValue(Biosample row) {
		row = linker.getLinked(row);
		if(row==null) return null;
		return row.getSampleName();
	}
	
	@Override
	public void setValue(Biosample row, String value) {
		row.setSampleName((String) value);
	}	
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		lbl.setVerticalAlignment(SwingConstants.TOP);
		lbl.setText(value==null?"": value.toString());
		return lbl;
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		super.postProcess(table, row, rowNo, value, comp);		
		if(row!=null && table.isEditable() && !linker.isLinked() && row.getBiotype()!=null && row.getBiotype().isNameRequired()) {
			comp.setBackground(LF.BGCOLOR_REQUIRED);
		}
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		Biotype t = getBiotype();
		if(t!=null && t.isNameAutocomplete()){
			return new AutoCompletionCellEditor(t);
		} else {
			return new AlphaNumericalCellEditor();
		}
	}
	
	@Override
	public boolean isAutoWrap() {
		return true;
	}
}
