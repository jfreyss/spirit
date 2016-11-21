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

package com.actelion.research.spiritapp.spirit.ui.pivot.column;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.spirit.ui.pivot.PivotCellPanel;
import com.actelion.research.spiritapp.spirit.ui.pivot.PivotTable;
import com.actelion.research.spiritcore.business.pivot.PivotCell;
import com.actelion.research.spiritcore.business.pivot.PivotColumn;
import com.actelion.research.spiritcore.business.pivot.PivotRow;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class PivotCellColumn extends Column<PivotRow, PivotCell>{
	
	private PivotColumn col;
	private PivotCellPanel valueListPanel = new PivotCellPanel();
	
	public PivotCellColumn(PivotColumn col) {
		super(col.getTitle(), PivotCell.class, 30, 200);
		this.col = col;
	}

	@Override
	public PivotCell getValue(PivotRow row) {
		return row.getPivotCell(col);
	}
		
	@Override
	public JComponent getCellComponent(AbstractExtendTable<PivotRow> t, PivotRow row, int rowNo, Object value) {
		PivotCell pivotCell = (PivotCell) value;
		PivotTable table = (PivotTable) t;
		if(pivotCell!=null) {
	
			valueListPanel.setPivotCell(pivotCell);
			
			return valueListPanel;
		} else {
			JComponent comp = super.getCellComponent(table, row, rowNo, value);
			return comp;
		}
	}
	
	@Override
	public boolean isMultiline() {
		return true;
	}
	
	
	@Override
	public boolean shouldMerge(PivotRow r1, PivotRow r2) {
		return false;
	}

}