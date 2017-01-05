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
import javax.swing.SwingUtilities;

import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotRow;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class PivotStringColumn extends Column<PivotRow, String>{
	private PivotItem item;
	
	public PivotStringColumn(PivotItem item) {
		super(item.getFullName(), String.class, 50, 400);
		this.item = item;
	}
	
	@Override
	public String getCategory() {
		return "Row";
	}
	
	@Override
	public String getValue(PivotRow row) {
		String v = item.getTitle(row.getRepresentative());
		return v==null? null: v.replace("\n", ".");
	}
	
	@Override
	public void postProcess(AbstractExtendTable<PivotRow> table, PivotRow row, int rowNo, Object value, JComponent comp) {
		if(row==null) return;
		if(comp instanceof JLabelNoRepaint) {
			//Header
			((JLabelNoRepaint)comp).setVerticalAlignment(SwingUtilities.TOP);
		}
	}
	

}