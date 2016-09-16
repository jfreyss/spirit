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

package com.actelion.research.spiritapp.spirit.ui.biosample.column;


import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.biosample.editor.QualityCellEditor;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class BioQualityColumn extends Column<Biosample, Quality> {

	public BioQualityColumn() {
		super("Quality", Quality.class, 40);
		lbl.setFont(FastFont.SMALL);
	}
	@Override
	public float getSortingKey() {
		return 9.5f;
	}
	@Override
	public Quality getValue(Biosample row) {
		return row.getQuality();
	}		
	@Override
	public boolean isEditable(Biosample row) {
		return true;
	}
	
	@Override
	public void setValue(Biosample row, Quality value) {
		row.setQuality(value);
	}
	
	@Override
	public void paste(Biosample row, String value) throws Exception {
		for(Quality q: Quality.values()) {
			if(q.getName().equalsIgnoreCase(value)) {
				setValue(row, q);
				break;
			}
		}
		throw new Exception(value+" is not a valid quality");
	}
	
	private static JLabelNoRepaint lbl = new JLabelNoRepaint();
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		Quality quality = (Quality) value;
		lbl.setVerticalAlignment(SwingConstants.TOP);
		if(quality==null) {
			lbl.setText(null);
			lbl.setBackground(Color.WHITE);
		} else {
			lbl.setText(quality.getName());
			lbl.setBackground(quality.getBackground());
			
		}
		return lbl;
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		super.postProcess(table, row, rowNo, value, comp);
		
		Quality status = (Quality) value;
		if(status!=null) {
			if(status.getBackground()!=null) lbl.setBackground(status.getBackground());
			
		}
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		return new QualityCellEditor();
	}
	
	@Override
	public boolean shouldMerge(Biosample r1, Biosample r2) {return false;}
	

	
}