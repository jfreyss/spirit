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

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

@Deprecated
public class OwnershipColumn extends Column<Biosample, String> {
	
	public OwnershipColumn() {
		super("Ownership change", String.class, 100);
	}
	
	@Override
	public float getSortingKey() {return 10.2f;}
	
	
	@Override
	public String getValue(Biosample row) {
		//For faster Load, load a bunch of data from the model and assign to the aux values.
//		List<ActionOwnership> list =  row.getActions(ActionOwnership.class);
//		ActionOwnership a = list.size()>0? list.get(0): null;
//
//		if(a==null) return ""; //don't return null to avoid filtering empty column
//		return "to " + a.getComments() + " [" + FormatterUtils.formatDateOrTime(a.getUpdDate()) + "]";
		return "To be reimplemented";
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		comp.setForeground(Color.PINK);
	}
	
	
	@Override
	public boolean isMultiline() {
		return false;
	}
	
}
