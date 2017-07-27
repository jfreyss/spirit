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

import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;

public class CommentsColumn extends AbstractLinkerColumn<String> {

	protected CommentsColumn(BiosampleLinker linker) {
		super(linker, String.class, 30, 350);
		lbl.setFont(FastFont.REGULAR);
	}
	
	@Override
	public String getValue(Biosample row) {
		if(row==null) return null;
		if(linker.isLinked()) row = linker.getLinked(row);
		if(row==null) return null;
		
		return row.getComments();
	}		
	@Override
	public void setValue(Biosample row, String value) {
		if(linker.isLinked()) row = linker.getLinked(row);
		
		row.setComments((String) value);
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		super.postProcess(table, row, rowNo, value, comp);
		
		if(row==null || !table.isEditable()) return;
		if(linker.isLinked()) row = linker.getLinked(row);
		Sampling s = row.getAttachedSampling();
		if(s!=null && s.isCommentsRequired()) {
			comp.setBackground(LF.BGCOLOR_REQUIRED);
		}
	}
	
	@Override
	public boolean isAutoWrap() {
		return true;
	}
}